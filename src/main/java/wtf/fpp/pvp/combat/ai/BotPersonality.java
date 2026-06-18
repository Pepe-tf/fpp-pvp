package wtf.fpp.pvp.combat.ai;

import java.util.Random;

// rolled when a bot enters pvp, tweaks how aggro they are
public enum BotPersonality {
    RUSHER,
    BAITER,
    TACTICIAN,
    CHAOTIC;

    public static BotPersonality roll(Random rng) {
        BotPersonality[] all = values();
        return all[rng.nextInt(all.length)];
    }

    public double aggression(Random rng) {
        return switch (this) {
            case RUSHER -> 0.9;
            case BAITER -> 0.4;
            case TACTICIAN -> 0.55;
            case CHAOTIC -> 0.5 + (rng.nextDouble() * 0.35);
        };
    }

    public double wtapChance(Random rng) {
        return switch (this) {
            case TACTICIAN -> 0.28;
            case RUSHER -> 0.12;
            case BAITER -> 0.18;
            case CHAOTIC -> 0.15 + (rng.nextDouble() * 0.2);
        };
    }

    public double critJumpChance() {
        return switch (this) {
            case RUSHER -> 0.18;
            case TACTICIAN -> 0.14;
            default -> 0.1;
        };
    }
}
