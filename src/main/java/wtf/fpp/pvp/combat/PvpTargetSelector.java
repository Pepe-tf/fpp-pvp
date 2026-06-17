package wtf.fpp.pvp.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.FppBot;
import wtf.fpp.pvp.PvpExtension;

public final class PvpTargetSelector {

    private final FppApi api;
    private final PvpExtension ext;
    private final Random rng = new Random();

    public PvpTargetSelector(FppApi api, PvpExtension extension) {
        this.api = api;
        this.ext = extension;
    }

    public LivingEntity findTarget(FppBot bot, Player ent, UUID locked) {
        if (locked != null) {
            LivingEntity stuck = playerFromUuid(ent, locked);
            if (stuck != null && isValidTarget(bot, ent, stuck)) {
                return stuck;
            }
            // locked target gone? fall through and pick someone else
        }

        List<LivingEntity> options = grabNearby(bot, ent);
        if (options.isEmpty()) return null;

        return switch (ext.getTargetPriority()) {
            case LOWEST_HEALTH -> options.stream()
                    .min(Comparator.comparingDouble(LivingEntity::getHealth))
                    .orElse(null);
            case RANDOM -> options.get(rng.nextInt(options.size()));
            default -> options.stream()
                    .min(Comparator.comparingDouble(p ->
                            p.getLocation().distanceSquared(ent.getLocation())))
                    .orElse(null);
        };
    }

    private LivingEntity playerFromUuid(Player ent, UUID id) {
        for (Player p : ent.getWorld().getPlayers()) {
            if (p.getUniqueId().equals(id)) return p;
        }
        return null;
    }

    private List<LivingEntity> grabNearby(FppBot bot, Player ent) {
        double r = ext.getScanRange();
        double rSq = r * r;
        List<LivingEntity> out = new ArrayList<>();

        for (Player p : ent.getWorld().getPlayers()) {
            if (!isValidTarget(bot, ent, p)) continue;
            if (ent.getLocation().distanceSquared(p.getLocation()) > rSq) continue;
            out.add(p);
        }

        return out;
    }

    public boolean isValidTarget(FppBot bot, Player botEnt, LivingEntity maybe) {
        if (maybe == null || maybe.equals(botEnt) || maybe.isDead()) return false;
        if (!(maybe instanceof Player p)) return false;

        if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) {
            return false; // dont be that guy
        }
        if (!p.isOnline() || !p.isValid()) return false;

        boolean theyBot = api.isBot(p);
        if (theyBot && !ext.targetBots()) return false;
        if (!theyBot && !ext.targetPlayers()) return false;
        if (theyBot && !ext.allowBotVsBot()) return false;

        // self target check
        if (theyBot && api.asBot(p).map(b -> b.getUuid().equals(bot.getUuid())).orElse(false)) {
            return false;
        }

        return true;
    }

    public UUID parseTargetUuid(String name, Player botEnt) {
        if (name == null || name.isBlank()) return null;
        for (Player p : botEnt.getWorld().getPlayers()) {
            if (p.getName().equalsIgnoreCase(name)) return p.getUniqueId();
        }
        return null;
    }
}
