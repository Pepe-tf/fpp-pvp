package wtf.fpp.pvp.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.FppBot;
import wtf.fpp.pvp.combat.PvpCombatService;

public final class PvpRetaliationListener implements Listener {

    private final FppApi api;
    private final PvpCombatService fights;

    public PvpRetaliationListener(FppApi api, PvpCombatService combatService) {
        this.api = api;
        this.fights = combatService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBotDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        FppBot bot = api.asBot(victim).orElse(null);
        if (bot == null) return;

        LivingEntity whoHit = livingOnly(event.getDamager());
        if (whoHit == null) return;

        fights.retaliate(bot, whoHit);
    }

    private static LivingEntity livingOnly(Entity e) {
        if (e instanceof LivingEntity living) return living;
        return null;
    }
}
