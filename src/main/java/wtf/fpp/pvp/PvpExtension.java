package wtf.fpp.pvp;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.FppExtension;
import me.bill.fakePlayerPlugin.api.event.FppBotDespawnEvent;
import me.bill.fakePlayerPlugin.api.event.FppBotSpawnEvent;
import wtf.fpp.pvp.combat.PvpCombatService;
import wtf.fpp.pvp.combat.PvpTargetPriority;
import wtf.fpp.pvp.command.PvpCommandExtension;
import wtf.fpp.pvp.listener.PvpRetaliationListener;

public final class PvpExtension implements FppExtension, Listener {

    private FppApi api;
    private Plugin plugin;
    private PvpCombatService fights;
    private PvpCommandExtension pvpCmd;

    @Override
    public String getName() {
        return "FPP-PvP";
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String getDescription() {
        return "bots fighting players and other bots";
    }

    @Override
    public List<String> getAuthors() {
        return List.of("1rhino2", "El_Pepes");
    }

    @Override
    public List<String> getSoftDependencies() {
        return List.of("FakePlayerPlugin");
    }

    @Override
    public void onEnable(FppApi api) {
        this.api = api;
        this.plugin = api.getPlugin();
        PvpLogger.init(plugin.getLogger());

        saveDefaultConfig();
        reloadConfig();

        fights = new PvpCombatService(plugin, api, this);
        fights.start();

        pvpCmd = new PvpCommandExtension(api, fights);
        api.registerCommandExtension(pvpCmd);

        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);
        pm.registerEvents(new PvpRetaliationListener(api, fights), plugin);

        PvpLogger.info("pvp ext loaded");
    }

    @Override
    public void onDisable() {
        if (api != null && pvpCmd != null) {
            api.unregisterCommandExtension(pvpCmd);
        }
        if (fights != null) {
            fights.stop();
        }
        PvpLogger.info("bye");
    }

    @EventHandler
    public void onBotDespawn(FppBotDespawnEvent event) {
        if (fights != null) {
            fights.stopForBot(event.getBot().getUuid());
        }
    }

    @EventHandler
    public void onBotSpawn(FppBotSpawnEvent event) {
        if (!autoStartOnSpawn()) return;
        if (fights != null) {
            fights.startForBot(event.getBot(), false, null);
        }
    }

    // config getters below, boring but whatever

    public double getAttackRange() {
        return getConfig().getDouble("combat.attack-range", 3.5);
    }

    public double getScanRange() {
        return getConfig().getDouble("combat.scan-range", 24.0);
    }

    public long getTickInterval() {
        return getConfig().getLong("combat.tick-interval-ticks", 2L);
    }

    public PvpTargetPriority getTargetPriority() {
        return PvpTargetPriority.fromConfig(getConfig().getString("combat.target-priority", "nearest"));
    }

    public boolean targetPlayers() {
        return getConfig().getBoolean("targets.players", true);
    }

    public boolean targetBots() {
        return getConfig().getBoolean("targets.bots", true);
    }

    public boolean allowBotVsBot() {
        return getConfig().getBoolean("targets.bot-vs-bot", true);
    }

    public boolean moveToTarget() {
        return getConfig().getBoolean("combat.move-to-target", true);
    }

    public boolean stopNavigationOnReach() {
        return getConfig().getBoolean("combat.stop-navigation-on-reach", true);
    }

    public boolean sprintWhileAttacking() {
        return getConfig().getBoolean("combat.sprint-while-attacking", true);
    }

    public boolean retaliationEnabled() {
        return getConfig().getBoolean("combat.retaliation", true);
    }

    public boolean autoStartOnSpawn() {
        return getConfig().getBoolean("combat.auto-start-on-spawn", false);
    }
}
