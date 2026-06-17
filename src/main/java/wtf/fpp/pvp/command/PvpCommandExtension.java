package wtf.fpp.pvp.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.api.FppBot;
import me.bill.fakePlayerPlugin.api.FppCommandExtension;
import wtf.fpp.pvp.combat.PvpCombatService;

public final class PvpCommandExtension implements FppCommandExtension {

    private static final String PERM = "fpp.pvp";

    private final FppApi api;
    private final PvpCombatService fights;

    public PvpCommandExtension(FppApi api, PvpCombatService combatService) {
        this.api = api;
        this.fights = combatService;
    }

    @Override
    public String getCommandName() {
        return "pvp";
    }

    @Override
    public String getUsage() {
        return "<bot|all> [--once|--stop] [target] | --stop";
    }

    @Override
    public String getDescription() {
        return "make bots fight stuff";
    }

    @Override
    public String getPermission() {
        return PERM;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cbro its /fpp pvp " + getUsage());
            return true;
        }

        // global stop
        if (args.length == 1 && args[0].equalsIgnoreCase("--stop")) {
            fights.stopAll();
            sender.sendMessage("§aaight everyones chill now");
            return true;
        }

        String botName = args[0];
        boolean once = false;
        boolean stop = false;
        String targetName = null;

        for (int i = 1; i < args.length; i++) {
            String bit = args[i].toLowerCase(Locale.ROOT);
            if (bit.equals("--once")) {
                once = true;
            } else if (bit.equals("--stop")) {
                stop = true;
            } else if (targetName == null) {
                targetName = args[i];
            } else {
                sender.sendMessage("§ctoo many args lol /fpp pvp " + getUsage());
                return true;
            }
        }

        if (botName.equalsIgnoreCase("all") || botName.equalsIgnoreCase("--all")) {
            if (stop) {
                fights.stopAll();
                sender.sendMessage("§aStopped all bots.");
                return true;
            }
            int n = fights.startAll(once);
            if (n == 0) {
                sender.sendMessage("§cno bots online??");
            } else {
                sender.sendMessage("§ago " + n + " bots are fighting");
            }
            return true;
        }

        FppBot bot = api.getBot(botName).orElse(null);
        if (bot == null) {
            sender.sendMessage("§cwho is " + botName);
            return true;
        }

        if (stop) {
            fights.stopForBot(bot.getUuid());
            sender.sendMessage("§a" + bot.getDisplayName() + " stopped");
            return true;
        }

        UUID locked = null;
        Player ent = bot.getEntity();
        if (targetName != null) {
            if (ent == null) {
                sender.sendMessage("§cbot offline rn");
                return true;
            }
            locked = fights.targetSelector().parseTargetUuid(targetName, ent);
            if (locked == null) {
                sender.sendMessage("§ccant see " + targetName + " near that bot");
                return true;
            }
        }

        if (fights.startForBot(bot, once, locked) == 0) {
            sender.sendMessage("§cnah couldnt start " + bot.getDisplayName());
            return true;
        }

        // message style intentionally inconsistent sorry
        if (targetName != null && once) {
            sender.sendMessage("§e" + bot.getDisplayName() + " → " + targetName + " (one hit)");
        } else if (targetName != null) {
            sender.sendMessage("§a" + bot.getDisplayName() + " locked onto " + targetName);
        } else if (once) {
            sender.sendMessage("§7" + bot.getDisplayName() + " will swing once");
        } else {
            sender.sendMessage("§a" + bot.getDisplayName() + " is on pvp mode");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!canUse(sender)) return List.of();

        if (args.length == 1) {
            String pre = args[0].toLowerCase(Locale.ROOT);
            List<String> stuff = new ArrayList<>();
            for (String opt : List.of("--stop", "all", "--all")) {
                if (opt.startsWith(pre)) stuff.add(opt);
            }
            for (FppBot b : api.getBots()) {
                if (b.getName().toLowerCase(Locale.ROOT).startsWith(pre)) stuff.add(b.getName());
            }
            return stuff;
        }

        Set<String> used = new HashSet<>();
        for (int i = 1; i < args.length - 1; i++) {
            used.add(args[i].toLowerCase(Locale.ROOT));
        }

        String pre = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> stuff = new ArrayList<>();
        for (String flag : List.of("--once", "--stop")) {
            if (!used.contains(flag) && flag.startsWith(pre)) stuff.add(flag);
        }

        if (args.length >= 2 && !used.contains("--stop") && !used.contains("--once")) {
            Player botEnt = api.getBot(args[0]).map(FppBot::getEntity).orElse(null);
            if (botEnt != null) {
                for (Player p : botEnt.getWorld().getPlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(pre)) stuff.add(p.getName());
                }
            }
        }

        return stuff;
    }
}
