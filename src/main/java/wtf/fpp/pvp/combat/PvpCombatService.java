package wtf.fpp.pvp.combat;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.FppBot;
import me.bill.fakePlayerPlugin.api.event.FppBotAttackEvent;
import me.bill.fakePlayerPlugin.api.event.FppBotTaskEvent;
import wtf.fpp.pvp.PvpExtension;
import wtf.fpp.pvp.PvpLogger;

// this is where the bot actually tries to beat people up
// kinda copied swing timing from fpp attack command ngl
public final class PvpCombatService {

    private static final Map<Material, Integer> SWING_DELAY = Map.ofEntries(
            Map.entry(Material.NETHERITE_SWORD, 12),
            Map.entry(Material.DIAMOND_SWORD, 12),
            Map.entry(Material.IRON_SWORD, 12),
            Map.entry(Material.GOLDEN_SWORD, 12),
            Map.entry(Material.STONE_SWORD, 12),
            Map.entry(Material.WOODEN_SWORD, 12),
            Map.entry(Material.NETHERITE_AXE, 20),
            Map.entry(Material.DIAMOND_AXE, 20),
            Map.entry(Material.IRON_AXE, 22),
            Map.entry(Material.GOLDEN_AXE, 20),
            Map.entry(Material.STONE_AXE, 25),
            Map.entry(Material.WOODEN_AXE, 25),
            Map.entry(Material.TRIDENT, 22),
            Map.entry(Material.MACE, 33));

    private static final int FIST_DELAY = 5;

    private final Plugin plugin;
    private final FppApi api;
    private final PvpExtension extension;
    private final PvpTargetSelector targetSelector;
    private final Map<UUID, PvpCombatSession> activeFights = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public PvpCombatService(Plugin plugin, FppApi api, PvpExtension extension) {
        this.plugin = plugin;
        this.api = api;
        this.extension = extension;
        this.targetSelector = new PvpTargetSelector(api, extension);
    }

    public void start() {
        if (tickTask != null) return; // already going

        long interval = Math.max(1L, extension.getTickInterval());
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, interval, interval);
        PvpLogger.info("fight loop on, every " + interval + " ticks");
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        activeFights.clear();
    }

    public boolean isFighting(UUID botUuid) {
        return activeFights.containsKey(botUuid);
    }

    public int startForBot(FppBot bot, boolean once, UUID lockedTarget) {
        Player ent = bot.getEntity();
        if (ent == null || !ent.isOnline() || bot.isDead()) {
            return 0;
        }

        // restart clean if they were already fighting someone
        stopForBot(bot.getUuid());
        activeFights.put(bot.getUuid(), new PvpCombatSession(bot.getUuid(), once, lockedTarget));
        fireTaskEvent(bot, FppBotTaskEvent.Action.START);
        bot.setBotTypeName("pvp");
        api.setBotExtensionData(bot, extension.getName(), "pvp-enabled", "true");
        return 1;
    }

    public int startAll(boolean once) {
        int n = 0;
        for (FppBot bot : api.getBots()) {
            n += startForBot(bot, once, null);
        }
        return n;
    }

    public void stopForBot(UUID botUuid) {
        if (!activeFights.containsKey(botUuid)) return;

        activeFights.remove(botUuid);
        api.getBot(botUuid).ifPresent(bot -> {
            api.cancelNavigation(bot);
            fireTaskEvent(bot, FppBotTaskEvent.Action.STOP);
            api.setBotExtensionData(bot, extension.getName(), "pvp-enabled", "false");
        });
    }

    public void stopAll() {
        // copy keys first or concurrentmod stuff might happen
        new HashSet<>(activeFights.keySet()).forEach(this::stopForBot);
    }

    public void retaliate(FppBot bot, LivingEntity attacker) {
        if (!extension.retaliationEnabled()) return;
        if (isFighting(bot.getUuid())) return;
        if (!(attacker instanceof Player player)) return;

        Player ent = bot.getEntity();
        if (ent == null) return;
        if (!targetSelector.isValidTarget(bot, ent, player)) return;

        startForBot(bot, false, player.getUniqueId());
    }

    private void tickAll() {
        // toArray so we dont blow up if something stops mid-loop
        for (PvpCombatSession sesh : activeFights.values().toArray(new PvpCombatSession[0])) {
            api.getBot(sesh.botUuid()).ifPresent(bot -> tickFight(bot, sesh));
        }
    }

    private void tickFight(FppBot bot, PvpCombatSession sesh) {
        Player ent = bot.getEntity();
        if (ent == null || !ent.isOnline() || bot.isDead() || bot.isFrozen()) {
            stopForBot(sesh.botUuid());
            return;
        }

        LivingEntity tgt = targetSelector.findTarget(bot, ent, sesh.lockedTarget());
        if (tgt == null) {
            if (sesh.once()) stopForBot(sesh.botUuid());
            return;
        }

        sesh.setCurrentTarget(tgt);
        snapLookAt(ent, tgt);

        double range = extension.getAttackRange();
        double dist = ent.getLocation().distance(tgt.getLocation());

        if (dist > range) {
            walkToward(bot, tgt);
            return;
        }

        if (extension.stopNavigationOnReach() && api.isNavigating(bot)) {
            api.cancelNavigation(bot);
        }

        sesh.tickCooldown();
        if (!sesh.canAttack(swingDelay(ent))) return;

        if (swingAt(bot, ent, tgt)) {
            sesh.resetAttackCooldown();
            if (sesh.once()) stopForBot(sesh.botUuid());
        }
    }

    private void walkToward(FppBot bot, LivingEntity tgt) {
        if (!extension.moveToTarget()) return;
        if (api.isNavigating(bot)) return;

        Location dest = tgt.getLocation().clone();
        // 0.5 under range feels ok in testing, might tweak later
        api.navigateTo(bot, dest, null, null, null, Math.max(1.5, extension.getAttackRange() - 0.5));
    }

    private boolean swingAt(FppBot bot, Player ent, LivingEntity tgt) {
        ent.swingMainHand();

        double dmg = 1.0;
        try {
            var attr = ent.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attr != null) dmg = Math.max(0.1, attr.getValue());
        } catch (Exception ignored) {
            // shrug
        }

        FppBotAttackEvent evt = new FppBotAttackEvent(bot, tgt, dmg);
        Bukkit.getPluginManager().callEvent(evt);
        if (evt.isCancelled()) return false;

        // FIXME kb feels kinda off vs real player hits, pepe might need to expose nms attack
        tgt.damage(evt.getDamage(), ent);

        if (extension.sprintWhileAttacking() && flatDist(ent, tgt) > 2.5) {
            ent.setSprinting(true);
        }

        return true;
    }

    // yeah teleport for look. janky but works for now
    private static void snapLookAt(Player ent, LivingEntity tgt) {
        Location eye = ent.getEyeLocation();
        Location theirEye = tgt.getEyeLocation();
        Vector dir = theirEye.toVector().subtract(eye.toVector());
        if (dir.lengthSquared() < 0.0001) return;
        eye.setDirection(dir);
        ent.teleport(eye);
    }

    private static double flatDist(Player ent, LivingEntity tgt) {
        Location a = ent.getLocation();
        Location b = tgt.getLocation();
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static int swingDelay(Player ent) {
        ItemStack hand = ent.getInventory().getItemInMainHand();
        Material wpn = hand != null ? hand.getType() : Material.AIR;
        if (wpn == Material.AIR) return FIST_DELAY;
        return SWING_DELAY.getOrDefault(wpn, FIST_DELAY);
    }

    private void fireTaskEvent(FppBot bot, FppBotTaskEvent.Action action) {
        Bukkit.getPluginManager().callEvent(new FppBotTaskEvent(bot, "pvp", action));
    }

    public PvpTargetSelector targetSelector() {
        return targetSelector;
    }
}
