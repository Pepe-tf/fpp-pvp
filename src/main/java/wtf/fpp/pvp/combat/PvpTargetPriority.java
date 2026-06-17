package wtf.fpp.pvp.combat;

// how bots pick who to swing at
public enum PvpTargetPriority {
    NEAREST,
    LOWEST_HEALTH,
    RANDOM;

    public static PvpTargetPriority fromConfig(String raw) {
        if (raw == null) {
            return NEAREST;
        }
        try {
            return valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            // bad config value, just default to nearest
            return NEAREST;
        }
    }

    public String configValue() {
        return name().toLowerCase().replace('_', '-');
    }
}
