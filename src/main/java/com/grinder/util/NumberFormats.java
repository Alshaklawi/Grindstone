package com.grinder.util;
import java.util.Locale;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class NumberFormats {
    private NumberFormats() {}
    public static String experience(long value) {
        if (value >= 1_000_000) { return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0); }
        if (value >= 1_000) { return String.format(Locale.ROOT, "%.1fK", value / 1_000.0); }
        return Long.toString(value);
    }
    public static String target(long value) {
        if (value >= 1_000_000_000L) { return String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000.0); }
        if (value >= 1_000_000) { return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0); }
        return String.format(Locale.ROOT, "%,d", value);
    }
}
