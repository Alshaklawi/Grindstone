package com.grindstone.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class OverlayStyle {
    public static final Color BACKGROUND = new Color(23, 25, 29, 238);
    public static final Color BORDER = new Color(96, 88, 66, 210);
    public static final Color GOLD = new Color(230, 180, 83);
    public static final Color TEXT = new Color(238, 237, 229);
    public static final Color MUTED = new Color(163, 168, 176);
    public static final Color GREEN = new Color(104, 218, 151);
    public static final Color TRACK = new Color(55, 59, 65);

    private OverlayStyle() {
    }

    public static void frame(Graphics2D graphics, int width, int height, Color accent) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(0, 0, 0, 60));
        graphics.fillRoundRect(2, 3, width - 3, height - 4, 12, 12);
        graphics.setColor(BACKGROUND);
        graphics.fillRoundRect(0, 0, width - 3, height - 4, 10, 10);
        graphics.setColor(BORDER);
        graphics.drawRoundRect(0, 0, width - 4, height - 5, 10, 10);
        graphics.setColor(accent);
        graphics.fillRoundRect(11, 0, width - 26, 2, 2, 2);
    }

    public static void progress(Graphics2D graphics, int x, int y, int width, double fraction, Color color) {
        graphics.setColor(TRACK);
        graphics.fillRoundRect(x, y, width, 4, 4, 4);
        int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) {
            graphics.setColor(color);
            graphics.fillRoundRect(x, y, filled, 4, 4, 4);
        }
    }

    public static void check(Graphics2D graphics, int x, int y, Color color) {
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(x, y + 4, x + 3, y + 7);
        graphics.drawLine(x + 3, y + 7, x + 9, y);
    }

    public static String fit(String text, FontMetrics metrics, int width) {
        if (metrics.stringWidth(text) <= width) {
            return text;
        }
        if (metrics.stringWidth("...") > width) {
            return "";
        }
        int start = 0;
        int end = text.length();
        while (start < end) {
            int middle = (start + end + 1) / 2;
            if (metrics.stringWidth(text.substring(0, middle) + "...") <= width) {
                start = middle;
            } else {
                end = middle - 1;
            }
        }
        end = start;
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end) + "...";
    }

    public static String countdown(long resetAt, long now) {
        long seconds = Math.max(0, (resetAt - now + 999) / 1000);
        long days = seconds / 86_400;
        long hours = seconds / 3600 % 24;
        long minutes = seconds / 60 % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        return hours > 0 ? hours + "h " + minutes + "m" : minutes > 0 ? minutes + "m" : seconds + "s";
    }
}
