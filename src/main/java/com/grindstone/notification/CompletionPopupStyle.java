package com.grindstone.notification;

import com.grindstone.config.OverlayTheme;
import com.grindstone.overlay.OverlayStyle;
import com.grindstone.ui.Icon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
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
        return getHeaderHeight(theme, font) + getNoticeHeight(theme, font) + entries * getRowHeight(theme, font) + 8;
    }

    static void render(Graphics2D graphics, OverlayTheme theme, List<CompletionOverlayEntry> entries, boolean routinesOnly, Font nativeFont) {
        if (theme != OverlayTheme.RUNELITE) {
            renderStyled(graphics, theme, entries, routinesOnly, nativeFont);
            return;
        }
        int height = getHeight(entries.size(), theme, nativeFont);
        int headerHeight = getHeaderHeight(theme, nativeFont);
        int noticeHeight = getNoticeHeight(theme, nativeFont);
        int rowHeight = getRowHeight(theme, nativeFont);
        Color accent = OverlayStyle.getPalette(theme).getAccent();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        BackgroundComponent background = new BackgroundComponent();
        background.setRectangle(new Rectangle(0, 0, WIDTH, height));
        background.render(graphics);
        graphics.setFont(nativeFont);
        TitleComponent.builder().text("Grindstone").color(accent).preferredLocation(new Point(8, 6)).preferredSize(new Dimension(WIDTH - 16, 0)).build().render(graphics);
        String heading = getHeading(entries.size(), routinesOnly);
        drawCentered(graphics, heading, WIDTH / 2, headerHeight + noticeHeight - 6, accent);
        for (int index = 0; index < entries.size(); index++) {
            CompletionOverlayEntry entry = entries.get(index);
            int top = headerHeight + noticeHeight + index * rowHeight;
            if (index > 0) {
                graphics.setColor(theme == OverlayTheme.SLATE ? new Color(0x414b58) : new Color(0x494235));
                graphics.drawLine(14, top - 4, WIDTH - 15, top - 4);
            }
            graphics.setFont(nativeFont);
            List<String> lines = wrapTitle(entry.getTitle(), graphics, WIDTH - 86);
            int titleWidth = lines.stream().mapToInt(line -> graphics.getFontMetrics().stringWidth(line)).max().orElse(0);
            int groupWidth = 32 + 10 + titleWidth;
            int left = Math.max(16, (WIDTH - groupWidth) / 2);
            OverlayStyle.icon(graphics, entry.getIcon(), left, top + 2, 32);
            int titleX = left + 42;
            int lineHeight = nativeFont.getSize() + 4;
            int baseline = top + (lines.size() == 1 ? lineHeight + 7 : lineHeight - 1);
            for (String line : lines) {
                drawShadowed(graphics, line, titleX, baseline, Color.WHITE);
                baseline += lineHeight;
            }
            graphics.setFont(nativeFont);
            String detail = OverlayStyle.fit(entry.getDetail(), graphics.getFontMetrics(), WIDTH - 24);
            drawCentered(graphics, detail, WIDTH / 2, top + rowHeight - 11, Color.LIGHT_GRAY);
        }
    }

    private static int getHeaderHeight(OverlayTheme theme, Font font) {
        return theme == OverlayTheme.RUNELITE ? Math.max(HEADER_HEIGHT, font.getSize() + 14) : theme == OverlayTheme.GUILD ? 40 : 60;
    }

    private static int getNoticeHeight(OverlayTheme theme, Font font) {
        return theme == OverlayTheme.RUNELITE ? Math.max(24, font.getSize() + 10) : 0;
    }

    private static int getRowHeight(OverlayTheme theme, Font font) {
        return theme == OverlayTheme.RUNELITE ? Math.max(ROW_HEIGHT, (font.getSize() + 4) * 3 + 12) : 76;
    }

    private static String getHeading(int entries, boolean routinesOnly) {
        return entries == 1 ? routinesOnly ? "Routine complete" : "Milestone complete" : entries + (routinesOnly ? " routines complete" : " milestones complete");
    }

    private static void renderStyled(Graphics2D graphics, OverlayTheme theme, List<CompletionOverlayEntry> entries, boolean routinesOnly, Font font) {
        int height = getHeight(entries.size(), theme, font);
        OverlayStyle.Palette palette = OverlayStyle.getPalette(theme);
        renderStyledFrame(graphics, theme, height, palette);
        String heading = getHeading(entries.size(), routinesOnly);
        if (theme == OverlayTheme.GUILD) {
            graphics.setColor(palette.getSurface());
            graphics.fillRoundRect(8, 8, WIDTH - 16, 26, 6, 6);
            graphics.setColor(palette.getBorder());
            graphics.drawLine(4, 35, WIDTH - 5, 35);
            OverlayStyle.icon(graphics, Icon.MAIN.getImage(), 12, 10, 18);
            graphics.setFont(OverlayStyle.GUILD_HEADING_FONT);
            drawShadowed(graphics, "Grindstone", 38, 24, palette.getAccent());
            graphics.setFont(OverlayStyle.SMALL_FONT);
            drawShadowed(graphics, heading, WIDTH - 12 - graphics.getFontMetrics().stringWidth(heading), 24, palette.getSuccess());
        } else {
            OverlayStyle.icon(graphics, Icon.MAIN.getImage(), 12, 13, 22);
            graphics.setFont(OverlayStyle.HEADING_FONT);
            drawShadowed(graphics, "Grindstone", 42, 26, palette.getText());
            graphics.setFont(OverlayStyle.SMALL_FONT);
            drawShadowed(graphics, heading, 42, 44, palette.getMuted());
            if (theme == OverlayTheme.SLATE) {
                graphics.setColor(new Color(0x293e3b));
                graphics.fillRoundRect(WIDTH - 45, 18, 30, 25, 20, 20);
                OverlayStyle.check(graphics, WIDTH - 35, 26, palette.getSuccess());
            } else {
                OverlayStyle.check(graphics, WIDTH - 28, 20, palette.getSuccess());
            }
        }
        int rowHeight = getRowHeight(theme, font);
        for (int index = 0; index < entries.size(); index++) {
            int top = getHeaderHeight(theme, font) + index * rowHeight;
            renderStyledEntry(graphics, theme, entries.get(index), top, rowHeight, palette);
            if (theme == OverlayTheme.GUILD && index < entries.size() - 1) {
                graphics.setColor(palette.getBorder());
                graphics.drawLine(12, top + rowHeight - 1, WIDTH - 13, top + rowHeight - 1);
            }
        }
    }

    private static void renderStyledFrame(Graphics2D graphics, OverlayTheme theme, int height, OverlayStyle.Palette palette) {
        OverlayStyle.panel(graphics, theme, WIDTH, height, true);
        int radius = theme == OverlayTheme.GUILD ? 10 : theme == OverlayTheme.SLATE ? 24 : 14;
        graphics.setColor(palette.getAccent().darker());
        graphics.drawRoundRect(0, 0, WIDTH - 1, height - 1, radius, radius);
        graphics.setColor(new Color(palette.getAccent().getRed(), palette.getAccent().getGreen(), palette.getAccent().getBlue(), 70));
        graphics.drawRoundRect(3, 3, WIDTH - 7, height - 7, Math.max(4, radius - 4), Math.max(4, radius - 4));
        if (theme == OverlayTheme.GUILD) {
            graphics.setColor(palette.getAccent());
            for (int x : new int[]{5, WIDTH - 6}) {
                for (int y : new int[]{5, height - 6}) {
                    int directionX = x < WIDTH / 2 ? 1 : -1;
                    int directionY = y < height / 2 ? 1 : -1;
                    graphics.drawLine(x, y, x + directionX * 8, y);
                    graphics.drawLine(x, y, x, y + directionY * 8);
                }
            }
        }
    }

    private static void renderStyledEntry(Graphics2D graphics, OverlayTheme theme, CompletionOverlayEntry entry, int top, int rowHeight, OverlayStyle.Palette palette) {
        boolean guild = theme == OverlayTheme.GUILD;
        if (!guild) {
            graphics.setColor(palette.getSurface());
            graphics.fillRoundRect(9, top + 2, WIDTH - 18, rowHeight - 6, theme == OverlayTheme.SLATE ? 14 : 8, theme == OverlayTheme.SLATE ? 14 : 8);
            graphics.setColor(new Color(palette.getSuccess().getRed(), palette.getSuccess().getGreen(), palette.getSuccess().getBlue(), 85));
            graphics.drawRoundRect(9, top + 2, WIDTH - 19, rowHeight - 7, theme == OverlayTheme.SLATE ? 14 : 8, theme == OverlayTheme.SLATE ? 14 : 8);
        }
        if (guild) {
            graphics.setColor(palette.getBorder());
            graphics.fillOval(16, top + 15, 42, 42);
            graphics.setColor(palette.getBackground());
            graphics.fillOval(18, top + 17, 38, 38);
            graphics.setColor(palette.getAccent());
            graphics.drawOval(16, top + 15, 42, 42);
            graphics.setColor(palette.getBorder());
            graphics.drawOval(20, top + 19, 34, 34);
        } else {
            graphics.setColor(theme == OverlayTheme.SLATE ? new Color(0x293e3b) : new Color(0x263b3a));
            graphics.fillRoundRect(17, top + 17, 40, 40, theme == OverlayTheme.SLATE ? 12 : 6, theme == OverlayTheme.SLATE ? 12 : 6);
        }
        OverlayStyle.icon(graphics, entry.getIcon(), 21, top + 21, 32);
        graphics.setFont(guild ? OverlayStyle.GUILD_TITLE_FONT : OverlayStyle.TITLE_FONT);
        List<String> lines = wrapTitle(entry.getTitle(), graphics, WIDTH - 94);
        int baseline = top + (lines.size() == 1 ? 31 : 23);
        int lineHeight = guild ? 19 : 17;
        for (String line : lines) {
            drawShadowed(graphics, line, 70, baseline, palette.getText());
            baseline += lineHeight;
        }
        graphics.setFont(OverlayStyle.BODY_FONT);
        String detail = OverlayStyle.fit(entry.getDetail(), graphics.getFontMetrics(), WIDTH - 86);
        drawShadowed(graphics, detail, 70, top + 59, theme == OverlayTheme.SLATE ? palette.getSuccess() : palette.getMuted());
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
