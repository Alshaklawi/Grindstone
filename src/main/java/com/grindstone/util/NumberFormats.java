package com.grindstone.util;
import java.math.BigInteger;
import java.util.Locale;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class NumberFormats {
    private NumberFormats() {
    }
    public static int calculatePercentage(long current, long target) {
        if (target <= 0) {
            return 0;
        }
        long progress = Math.max(0, Math.min(current, target));
        BigInteger numerator = BigInteger.valueOf(progress).multiply(BigInteger.valueOf(100));
        return numerator.divide(BigInteger.valueOf(target)).intValue();
    }
    public static String formatExperience(long value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000.0);
        }
        return Long.toString(value);
    }
    public static String formatTarget(long value) {
        if (value >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        return String.format(Locale.ROOT, "%,d", value);
    }
}
