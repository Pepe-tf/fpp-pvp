package wtf.fpp.pvp;

import java.util.logging.Logger;

public final class PvpLogger {

    private static Logger log;

    private PvpLogger() {}

    public static void init(Logger parent) {
        log = parent;
    }

    public static void info(String msg) {
        if (log != null) log.info("[pvp] " + msg);
    }

    public static void warn(String msg) {
        if (log != null) log.warning("[pvp] " + msg);
    }

    public static void debug(String msg) {
        if (log != null) log.fine("pvp debug: " + msg); // inconsistent on purpose lol
    }
}
