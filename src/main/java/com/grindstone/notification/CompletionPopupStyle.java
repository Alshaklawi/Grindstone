package com.grindstone.notification;

import com.grindstone.config.OverlayTheme;
import com.grindstone.overlay.OverlayStyle;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/11/2026
 */
final class CompletionPopupStyle {
    static final int WIDTH = 320;
    private static final int HEADER_HEIGHT = 30;
    private static final int ROW_HEIGHT = 62;

    private CompletionPopupStyle() {
    }

    static int getHeight(int entries, OverlayTheme theme, Font font) {
        return getHeaderHeight(theme, font) + getNoticeHeight(theme, font) + entries * getRowHeight(theme, font) + 6;
    }

    static void render(Graphics2D graphics, OverlayTheme theme, List<CompletionOverlayEntry> entries, boolean routinesOnly, Font nativeFont) {
        int height = getHeight(entries.size(), theme, nativeFont);
        int headerHeight = getHeaderHeight(theme, nativeFont);
        int noticeHeight = getNoticeHeight(theme, nativeFont);
        int rowHeight = getRowHeight(theme, nativeFont);
        boolean nativeStyle = theme == OverlayTheme.RUNELITE;
        Font headingFont = nativeStyle ? nativeFont : FontManager.getRunescapeBoldFont();
        Font bodyFont = nativeStyle ? nativeFont : FontManager.getRunescapeFont();
        Color accent = theme == OverlayTheme.CLASSIC ? new Color(0xff981f) : OverlayStyle.getPalette(theme).getAccent();
        Color text = nativeStyle ? Color.WHITE : OverlayStyle.getPalette(theme).getText();
        Color muted = nativeStyle ? Color.LIGHT_GRAY : OverlayStyle.getPalette(theme).getMuted();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, nativeStyle ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        renderFrame(graphics, theme, height);
        graphics.setFont(headingFont);
        if (nativeStyle) {
            TitleComponent.builder().text("Grindstone").color(accent).preferredLocation(new Point(8, 6)).preferredSize(new Dimension(WIDTH - 16, 0)).build().render(graphics);
        } else {
            drawCentered(graphics, "Grindstone", WIDTH / 2, 21, accent);
        }
        graphics.setFont(bodyFont);
        String heading = entries.size() == 1 ? routinesOnly ? "Routine complete" : "Milestone complete" : entries.size() + (routinesOnly ? " routines complete" : " milestones complete");
        drawCentered(graphics, heading, WIDTH / 2, headerHeight + noticeHeight - 6, accent);
        for (int index = 0; index < entries.size(); index++) {
            CompletionOverlayEntry entry = entries.get(index);
            int top = headerHeight + noticeHeight + index * rowHeight;
            if (index > 0) {
                graphics.setColor(theme == OverlayTheme.SLATE ? new Color(0x414b58) : new Color(0x494235));
                graphics.drawLine(14, top - 4, WIDTH - 15, top - 4);
            }
            graphics.setFont(headingFont);
            List<String> lines = wrapTitle(entry.getTitle(), graphics, WIDTH - 86);
            int titleWidth = lines.stream().mapToInt(line -> graphics.getFontMetrics().stringWidth(line)).max().orElse(0);
            int groupWidth = 32 + 10 + titleWidth;
            int left = Math.max(16, (WIDTH - groupWidth) / 2);
            OverlayStyle.icon(graphics, entry.getIcon(), left, top + 2, 32);
            int titleX = left + 42;
            int lineHeight = nativeStyle ? nativeFont.getSize() + 4 : 16;
            int baseline = top + (lines.size() == 1 ? lineHeight + 7 : lineHeight - 1);
            for (String line : lines) {
                drawShadowed(graphics, line, titleX, baseline, text);
                baseline += lineHeight;
            }
            graphics.setFont(bodyFont);
            String detail = OverlayStyle.fit(entry.getDetail(), graphics.getFontMetrics(), WIDTH - 24);
            drawCentered(graphics, detail, WIDTH / 2, top + rowHeight - 11, muted);
        }
    }

    private static int getHeaderHeight(OverlayTheme theme, Font font) {
        return theme == OverlayTheme.RUNELITE ? Math.max(HEADER_HEIGHT, font.getSize() + 14) : HEADER_HEIGHT;
    }

    private static int getNoticeHeight(OverlayTheme theme, Font font) {
        return theme == OverlayTheme.RUNELITE ? Math.max(24, font.getSize() + 10) : 24;
    }

    private static int getRowHeight(OverlayTheme theme, Font font) {
        return theme == OverlayTheme.RUNELITE ? Math.max(ROW_HEIGHT, (font.getSize() + 4) * 3 + 12) : ROW_HEIGHT;
    }

    private static void renderFrame(Graphics2D graphics, OverlayTheme theme, int height) {
        if (theme == OverlayTheme.RUNELITE) {
            BackgroundComponent background = new BackgroundComponent();
            background.setRectangle(new Rectangle(0, 0, WIDTH, height));
            background.render(graphics);
            return;
        }
        if (theme == OverlayTheme.SLATE) {
            graphics.setColor(new Color(0x1e252f));
            graphics.fillRoundRect(0, 0, WIDTH, height, 8, 8);
            graphics.setColor(new Color(0x566b7c));
            graphics.drawRoundRect(0, 0, WIDTH - 1, height - 1, 8, 8);
            graphics.setColor(new Color(0x414b58));
            graphics.drawLine(8, HEADER_HEIGHT, WIDTH - 9, HEADER_HEIGHT);
            return;
        }
        boolean guild = theme == OverlayTheme.GUILD;
        graphics.setColor(new Color(0x080807));
        graphics.fillRect(0, 0, WIDTH, height);
        graphics.setColor(guild ? new Color(0x806b47) : new Color(0x5c5c56));
        graphics.drawRect(1, 1, WIDTH - 3, height - 3);
        graphics.setColor(guild ? new Color(0x494035) : new Color(0x3d3d38));
        graphics.drawRect(3, 3, WIDTH - 7, height - 7);
        graphics.setColor(guild ? new Color(0x26231e) : new Color(0x242424));
        graphics.fillRect(6, 6, WIDTH - 12, height - 12);
        graphics.setColor(guild ? new Color(0x302b22) : new Color(0x2c2c29));
        graphics.fillRect(7, 7, WIDTH - 14, HEADER_HEIGHT - 8);
        graphics.setColor(guild ? new Color(0x806b47) : new Color(0x5c5c56));
        graphics.drawRect(6, 6, WIDTH - 13, HEADER_HEIGHT - 6);
    }

    private static List<String> wrapTitle(String title, Graphics2D graphics, int width) {
        List<String> lines = new ArrayList<>(2);
        String remaining = title;
        if (graphics.getFontMetrics().stringWidth(remaining) > width) {
            int split = remaining.lastIndexOf(' ');
            while (split > 0 && graphics.getFontMetrics().stringWidth(remaining.substring(0, split)) > width) {
                split = remaining.lastIndexOf(' ', split - 1);
            }
            if (split > 0) {
                lines.add(remaining.substring(0, split));
                remaining = remaining.substring(split + 1);
            }
        }
        lines.add(OverlayStyle.fit(remaining, graphics.getFontMetrics(), width));
        return lines;
    }

    private static void drawCentered(Graphics2D graphics, String text, int center, int baseline, Color color) {
        drawShadowed(graphics, text, center - graphics.getFontMetrics().stringWidth(text) / 2, baseline, color);
    }

    private static void drawShadowed(Graphics2D graphics, String text, int x, int y, Color color) {
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x + 1, y + 1);
        graphics.setColor(color);
        graphics.drawString(text, x, y);
    }
}
