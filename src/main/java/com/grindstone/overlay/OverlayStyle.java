package com.grindstone.overlay;

import com.grindstone.config.OverlayTheme;
import com.grindstone.ui.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import lombok.Value;

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
    public static final Font METRIC_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
    public static final Font DISPLAY_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 21);
    public static final Font GUILD_HEADING_FONT = new Font(Font.SERIF, Font.BOLD, 14);
    public static final Font GUILD_TITLE_FONT = new Font(Font.SERIF, Font.BOLD, 16);
    private static final Palette CLASSIC = new Palette(GOLD, GREEN, TEXT, MUTED, BACKGROUND, SURFACE, TRACK, BORDER);
    private static final Palette SLATE = new Palette(new Color(0x79b7cf), new Color(0x81c8b3), new Color(0xedf2f8), new Color(0xa2aebb), new Color(0x1e252f), new Color(0x2a323e), new Color(0x414b59), new Color(0x414b58));
    private static final Palette GUILD = new Palette(new Color(0xd7b374), new Color(0x9abd8c), new Color(0xf2eadb), new Color(0xb8ad97), new Color(0x201f1c), new Color(0x363025), new Color(0x39372f), new Color(0x786141));

    private OverlayStyle() {
    }

    public static Palette getPalette(OverlayTheme theme) {
        switch (theme) {
            case SLATE:
                return SLATE;
            case GUILD:
                return GUILD;
            default:
                return CLASSIC;
        }
    }

    public static int getHeaderHeight(OverlayTheme theme) {
        switch (theme) {
            case RUNELITE:
                return 48;
            case SLATE:
                return 68;
            case GUILD:
                return 40;
            default:
                return 60;
        }
    }

    public static int getRowHeight(OverlayTheme theme, boolean bars) {
        switch (theme) {
            case RUNELITE:
                return bars ? 60 : 42;
            case SLATE:
                return bars ? 76 : 65;
            case GUILD:
                return bars ? 64 : 54;
            default:
                return bars ? 50 : 42;
        }
    }

    public static void panel(Graphics2D graphics, OverlayTheme theme, int width, int height, boolean completion) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Palette palette = getPalette(theme);
        if (theme == OverlayTheme.CLASSIC) {
            frame(graphics, width, height, completion ? GREEN : GOLD);
        } else if (theme == OverlayTheme.GUILD) {
            graphics.setPaint(new GradientPaint(0, 0, new Color(completion ? 0x3d3222 : 0x302c25), width, height, palette.background));
            graphics.fillRoundRect(0, 0, width, height, 10, 10);
            graphics.setColor(palette.border);
            graphics.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);
            graphics.setColor(new Color(0x494030));
            graphics.drawRoundRect(3, 3, width - 7, height - 7, 5, 5);
        } else if (theme == OverlayTheme.SLATE) {
            graphics.setPaint(new GradientPaint(0, 0, completion ? new Color(0x233e3b) : palette.background, width, height, new Color(0x202832)));
            graphics.fillRoundRect(0, 0, width, height, 24, 24);
            graphics.setColor(completion ? new Color(0x465b61) : palette.border);
            graphics.drawRoundRect(0, 0, width - 1, height - 1, 24, 24);
        }
    }

    public static void icon(Graphics2D graphics, BufferedImage image, int x, int y, int size) {
        BufferedImage source = image == null ? Icon.MAIN.getImage() : image;
        double scale = Math.min(1, size / (double) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        graphics.drawImage(source, x + (size - width) / 2, y + (size - height) / 2, width, height, null);
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
        progress(graphics, x, y, width, 5, fraction, color, TRACK);
    }

    public static void progress(Graphics2D graphics, int x, int y, int width, int height, double fraction, Color color, Color track) {
        graphics.setColor(track);
        graphics.fillRoundRect(x, y, width, height, height, height);
        int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) {
            graphics.setPaint(new GradientPaint(x, y, color.darker(), x + width, y, color));
            graphics.fillRoundRect(x, y, filled, height, height, height);
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

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 11/04/2026
     */
    @Value
    public static class Palette {
        Color accent;
        Color success;
        Color text;
        Color muted;
        Color background;
        Color surface;
        Color track;
        Color border;
    }
}
