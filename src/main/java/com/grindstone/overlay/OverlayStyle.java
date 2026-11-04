package com.grindstone.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class OverlayStyle {
    public static final Color BACKGROUND = new Color(22, 26, 33, 245);
    public static final Color BORDER = new Color(115, 132, 151, 85);
    public static final Color GOLD = new Color(225, 186, 112);
    public static final Color TEXT = new Color(236, 241, 246);
    public static final Color MUTED = new Color(153, 168, 184);
    public static final Color GREEN = new Color(98, 205, 174);
    public static final Color TRACK = new Color(47, 58, 71);
    public static final Color SURFACE = new Color(255, 255, 255, 7);
    public static final Font HEADING_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    public static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    private OverlayStyle() {
    }

    public static void frame(Graphics2D graphics, int width, int height, Color accent) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setPaint(new GradientPaint(0, 0, new Color(34, 41, 51, 248), 0, height, BACKGROUND));
        graphics.fillRoundRect(0, 0, width, height, 14, 14);
        graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 10));
        graphics.fillRoundRect(1, 1, width - 2, 51, 12, 12);
        graphics.setColor(BORDER);
        graphics.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);
        graphics.setColor(new Color(255, 255, 255, 12));
        graphics.drawLine(12, 52, width - 13, 52);
    }

    public static void progress(Graphics2D graphics, int x, int y, int width, double fraction, Color color) {
        graphics.setColor(TRACK);
        graphics.fillRoundRect(x, y, width, 5, 5, 5);
        int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) {
            graphics.setPaint(new GradientPaint(x, y, color.darker(), x + width, y, color));
            graphics.fillRoundRect(x, y, filled, 5, 5, 5);
        }
    }

    public static void check(Graphics2D graphics, int x, int y, Color color) {
        graphics.setColor(color);
        Stroke previous = graphics.getStroke();
        graphics.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(x, y + 4, x + 3, y + 7);
        graphics.drawLine(x + 3, y + 7, x + 9, y);
        graphics.setStroke(previous);
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

    public static String formatCountdown(long resetAt, long now) {
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
