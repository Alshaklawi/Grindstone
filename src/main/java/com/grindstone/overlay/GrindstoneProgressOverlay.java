package com.grindstone.overlay;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.config.OverlayTheme;
import com.grindstone.config.ProgressOverlayView;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.Icon;
import com.grindstone.util.NumberFormats;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
@Singleton
public final class GrindstoneProgressOverlay extends Overlay {
    private static final int SECTION_HEIGHT = 24;
    private final GrindstoneConfig config;
    private volatile Snapshot snapshot;

    @Inject
    public GrindstoneProgressOverlay(GrindstoneConfig config) {
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_LOW);
    }

    public void clear() {
        snapshot = null;
    }

    public void update(List<ProgressOverlayEntry> entries, int canvasHeight) {
        if (!config.enabled() || !config.progressOverlayEnabled()) {
            clear();
            return;
        }
        ProgressOverlayView view = config.progressOverlayView();
        List<ProgressOverlayEntry> visible = new ArrayList<>();
        for (ProgressOverlayEntry entry : entries) {
            if (view.includes(entry.getSchedule(), entry.isPinned()) && (!entry.isComplete() || entry.getSchedule() != RepeatSchedule.NONE && config.progressOverlayCompletedRoutines())) {
                visible.add(entry);
            }
        }
        visible.sort(Comparator.comparingInt((ProgressOverlayEntry entry) -> order(entry.getSchedule())).thenComparing(ProgressOverlayEntry::isComplete).thenComparing(entry -> !entry.isPinned()).thenComparing(ProgressOverlayEntry::getTitle, String.CASE_INSENSITIVE_ORDER));
        boolean grouped = view == ProgressOverlayView.ALL || view == ProgressOverlayView.ROUTINES || view == ProgressOverlayView.PINNED;
        boolean bars = config.progressOverlayBars();
        OverlayTheme theme = config.progressOverlayTheme();
        int rowHeight = OverlayStyle.getRowHeight(theme, bars);
        int limit = Math.max(1, Math.min(20, config.progressOverlayRows()));
        int height = OverlayStyle.getHeaderHeight(theme) + getFooterHeight(view, true);
        int budget = Math.max(height + rowHeight + (grouped ? SECTION_HEIGHT : 0), canvasHeight - 32);
        RepeatSchedule previous = null;
        List<Row> rows = new ArrayList<>();
        for (ProgressOverlayEntry entry : visible) {
            int cost = rowHeight + (grouped && previous != entry.getSchedule() ? SECTION_HEIGHT : 0);
            if (rows.size() >= limit || height + cost > budget) {
                break;
            }
            rows.add(new Row(entry));
            height += cost;
            previous = entry.getSchedule();
        }
        int completed = (int) visible.stream().filter(ProgressOverlayEntry::isComplete).count();
        snapshot = new Snapshot(List.copyOf(rows), view, visible.size(), completed, Math.max(180, Math.min(360, config.progressOverlayWidth())), bars, grouped, theme);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Snapshot current = snapshot;
        if (!config.enabled() || !config.progressOverlayEnabled() || current == null || current.entries.isEmpty()) {
            return null;
        }
        int sections = 0;
        RepeatSchedule previous = null;
        for (Row row : current.entries) {
            ProgressOverlayEntry entry = row.entry;
            if (current.grouped && previous != entry.getSchedule()) {
                sections++;
            }
            previous = entry.getSchedule();
        }
        int rowHeight = OverlayStyle.getRowHeight(current.theme, current.bars);
        boolean hidden = current.total > current.entries.size();
        int height = OverlayStyle.getHeaderHeight(current.theme) + current.entries.size() * rowHeight + sections * SECTION_HEIGHT + getFooterHeight(current.view, hidden);
        Graphics2D card = (Graphics2D) graphics.create();
        try {
            OverlayStyle.Palette palette = OverlayStyle.getPalette(current.theme);
            OverlayStyle.panel(card, current.theme, current.width, height, false);
            renderHeader(card, current, palette);
            int y = OverlayStyle.getHeaderHeight(current.theme);
            previous = null;
            for (Row row : current.entries) {
                ProgressOverlayEntry entry = row.entry;
                if (current.grouped && previous != entry.getSchedule()) {
                    card.setFont(OverlayStyle.SMALL_FONT);
                    card.setColor(palette.getMuted());
                    String section = entry.getSchedule() == RepeatSchedule.NONE ? "Milestones" : entry.getSchedule().toString();
                    card.drawString(section, 12, y + 15);
                    card.setColor(palette.getBorder());
                    int lineStart = 20 + card.getFontMetrics().stringWidth(section);
                    card.drawLine(lineStart, y + 11, current.width - 12, y + 11);
                    y += SECTION_HEIGHT;
                }
                switch (current.theme) {
                    case SLATE:
                        renderSlateEntry(card, row, current.width, y, current.bars, palette);
                        break;
                    case GUILD:
                        renderGuildEntry(card, row, current.width, y, current.bars, palette);
                        break;
                    default:
                        renderEntry(card, row, current.width, y, current.bars);
                        break;
                }
                y += rowHeight;
                previous = entry.getSchedule();
            }
            card.setFont(OverlayStyle.SMALL_FONT);
            card.setColor(palette.getMuted());
            if (current.view == ProgressOverlayView.DAILY) {
                long now = System.currentTimeMillis();
                String reset = "Resets in " + OverlayStyle.formatCountdown(RepeatSchedule.DAILY.getNextReset(RepeatSchedule.DAILY.getPeriodStart(now)), now);
                card.drawString(reset, (current.width - card.getFontMetrics().stringWidth(reset)) / 2, y + 16);
                y += 24;
            }
            if (hidden) {
                card.setFont(OverlayStyle.BODY_FONT);
                card.drawString(OverlayStyle.fit((current.total - current.entries.size()) + " more in the sidebar", card.getFontMetrics(), current.width - 24), 12, y + 14);
            }
        } finally {
            card.dispose();
        }
        return new Dimension(current.width, height);
    }

    private static int getFooterHeight(ProgressOverlayView view, boolean hidden) {
        return (view == ProgressOverlayView.DAILY ? 24 : 0) + (hidden ? 24 : 8);
    }

    private static void renderHeader(Graphics2D graphics, Snapshot current, OverlayStyle.Palette palette) {
        if (current.theme == OverlayTheme.CLASSIC) {
            OverlayStyle.icon(graphics, Icon.MAIN.getImage(), 12, 13, 22);
            graphics.setFont(OverlayStyle.HEADING_FONT);
            graphics.setColor(palette.getText());
            graphics.drawString(OverlayStyle.fit(current.view.getTitle(), graphics.getFontMetrics(), current.width - 54), 40, 25);
            graphics.setFont(OverlayStyle.BODY_FONT);
            graphics.setColor(palette.getMuted());
            graphics.drawString(current.view == ProgressOverlayView.MILESTONES ? current.total + " active" : current.completed + " of " + current.total + " complete", 12, 43);
            return;
        }
        boolean guild = current.theme == OverlayTheme.GUILD;
        if (guild) {
            graphics.setColor(palette.getSurface());
            graphics.fillRoundRect(4, 4, current.width - 8, 58, 6, 6);
            graphics.setColor(palette.getBorder());
            graphics.drawLine(4, 63, current.width - 5, 63);
            graphics.drawRoundRect(12, 15, 26, 32, 5, 5);
        }
        OverlayStyle.icon(graphics, Icon.MAIN.getImage(), guild ? 15 : 13, 22, 22);
        int titleX = guild ? 48 : 43;
        graphics.setFont(OverlayStyle.SMALL_FONT);
        graphics.setColor(guild ? palette.getAccent() : palette.getMuted());
        graphics.drawString("GRINDSTONE", titleX, 23);
        String tally = current.view == ProgressOverlayView.MILESTONES ? Integer.toString(current.total) : current.completed + " / " + current.total;
        int tallyWidth = graphics.getFontMetrics().stringWidth(tally) + 16;
        int tallyX = current.width - tallyWidth - 12;
        if (!guild) {
            graphics.setColor(new Color(0x293e3b));
            graphics.fillRoundRect(tallyX, 30, tallyWidth, 22, 22, 22);
        }
        graphics.setColor(palette.getSuccess());
        graphics.drawString(tally, tallyX + 8, 45);
        graphics.setFont(guild ? OverlayStyle.GUILD_HEADING_FONT : OverlayStyle.HEADING_FONT);
        graphics.setColor(guild ? palette.getAccent() : palette.getText());
        graphics.drawString(OverlayStyle.fit(current.view.getTitle(), graphics.getFontMetrics(), tallyX - titleX - 6), titleX, 44);
    }

    private static void renderEntry(Graphics2D graphics, Row row, int width, int y, boolean bars) {
        ProgressOverlayEntry entry = row.entry;
        Color accent = entry.isComplete() ? OverlayStyle.GREEN : entry.isReady() ? OverlayStyle.GOLD : OverlayStyle.MUTED;
        graphics.setColor(OverlayStyle.SURFACE);
        graphics.fillRoundRect(7, y + 1, width - 14, bars ? 46 : 38, 8, 8);
        OverlayStyle.icon(graphics, entry.getIcon(), 12, y + 3, 18);
        int titleX = 36;
        if (entry.isComplete()) {
            OverlayStyle.check(graphics, width - 25, y + 8, accent);
        } else if (entry.isPinned()) {
            graphics.setColor(OverlayStyle.GOLD);
            graphics.fillOval(width - 21, y + 10, 4, 4);
        }
        graphics.setFont(OverlayStyle.TITLE_FONT);
        graphics.setColor(entry.isComplete() ? OverlayStyle.GREEN : OverlayStyle.TEXT);
        graphics.drawString(OverlayStyle.fit(entry.getTitle(), graphics.getFontMetrics(), width - titleX - 30), titleX, y + 16);
        graphics.setFont(OverlayStyle.BODY_FONT);
        int percentageWidth = graphics.getFontMetrics().stringWidth(row.percentage);
        graphics.setColor(OverlayStyle.MUTED);
        graphics.drawString(OverlayStyle.fit(row.progress, graphics.getFontMetrics(), width - 28 - (percentageWidth == 0 ? 0 : percentageWidth + 10)), 14, y + 31);
        graphics.setColor(accent);
        graphics.drawString(row.percentage, width - 14 - percentageWidth, y + 31);
        if (bars) {
            OverlayStyle.progress(graphics, 14, y + 38, width - 28, entry.isReady() ? row.ratio : 0, accent);
        }
    }

    private static void renderGuildEntry(Graphics2D graphics, Row row, int width, int y, boolean bars, OverlayStyle.Palette palette) {
        Color accent = getAccent(row, palette);
        OverlayStyle.icon(graphics, row.entry.getIcon(), 12, y + 2, 24);
        graphics.setFont(OverlayStyle.TITLE_FONT);
        graphics.setColor(row.entry.isComplete() ? palette.getSuccess() : palette.getText());
        graphics.drawString(OverlayStyle.fit(row.entry.getTitle(), graphics.getFontMetrics(), width - 74), 43, y + 19);
        renderStatus(graphics, row, width - 25, y + 11, palette);
        graphics.setFont(OverlayStyle.BODY_FONT);
        graphics.setColor(palette.getMuted());
        int percentageWidth = graphics.getFontMetrics().stringWidth(row.percentage);
        graphics.drawString(OverlayStyle.fit(row.progress, graphics.getFontMetrics(), width - 38 - percentageWidth), 16, y + 36);
        graphics.setColor(accent);
        graphics.drawString(row.percentage, width - 16 - percentageWidth, y + 36);
        if (bars) {
            OverlayStyle.progress(graphics, 16, y + 45, width - 32, 5, row.entry.isReady() ? row.ratio : 0, accent, palette.getTrack());
        }
        graphics.setColor(new Color(0x494235));
        graphics.drawLine(16, y + OverlayStyle.getRowHeight(OverlayTheme.GUILD, bars) - 4, width - 17, y + OverlayStyle.getRowHeight(OverlayTheme.GUILD, bars) - 4);
    }

    private static void renderSlateEntry(Graphics2D graphics, Row row, int width, int y, boolean bars, OverlayStyle.Palette palette) {
        Color accent = getAccent(row, palette);
        graphics.setColor(row.entry.isComplete() ? new Color(0x25332f) : palette.getSurface());
        graphics.fillRoundRect(9, y, width - 18, OverlayStyle.getRowHeight(OverlayTheme.SLATE, bars) - 7, 14, 14);
        OverlayStyle.icon(graphics, row.entry.getIcon(), 16, y + 7, 20);
        graphics.setFont(OverlayStyle.BODY_FONT);
        graphics.setColor(palette.getText());
        graphics.drawString(OverlayStyle.fit(row.entry.getTitle(), graphics.getFontMetrics(), width - 72), 42, y + 21);
        renderStatus(graphics, row, width - 26, y + 13, palette);
        if (row.entry.isReady() && row.entry.getTarget() > 0) {
            graphics.setFont(OverlayStyle.METRIC_FONT);
            graphics.setColor(row.entry.isComplete() ? palette.getSuccess() : palette.getText());
            graphics.drawString(row.current, 19, y + 47);
            int currentWidth = graphics.getFontMetrics().stringWidth(row.current);
            graphics.setFont(OverlayStyle.SMALL_FONT);
            graphics.setColor(palette.getMuted());
            String target = " / " + row.target;
            int percentageWidth = graphics.getFontMetrics().stringWidth(row.percentage);
            graphics.drawString(OverlayStyle.fit(target, graphics.getFontMetrics(), width - currentWidth - percentageWidth - 48), 19 + currentWidth, y + 47);
            graphics.setColor(accent);
            graphics.drawString(row.percentage, width - 19 - percentageWidth, y + 47);
        } else {
            graphics.setFont(OverlayStyle.BODY_FONT);
            graphics.setColor(palette.getMuted());
            graphics.drawString(OverlayStyle.fit(row.progress, graphics.getFontMetrics(), width - 38), 19, y + 47);
        }
        if (bars) {
            OverlayStyle.progress(graphics, 19, y + 57, width - 38, 4, row.entry.isReady() ? row.ratio : 0, accent, palette.getTrack());
        }
    }

    private static Color getAccent(Row row, OverlayStyle.Palette palette) {
        return row.entry.isComplete() ? palette.getSuccess() : row.entry.isReady() ? palette.getAccent() : palette.getMuted();
    }

    private static void renderStatus(Graphics2D graphics, Row row, int x, int y, OverlayStyle.Palette palette) {
        if (row.entry.isComplete()) {
            OverlayStyle.check(graphics, x, y, palette.getSuccess());
        } else if (row.entry.isPinned()) {
            graphics.setColor(palette.getAccent());
            graphics.fillOval(x + 3, y + 2, 4, 4);
        }
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 11/04/2026
     */
    private static final class Row {
        private final ProgressOverlayEntry entry;
        private final double ratio;
        private final String percentage;
        private final String progress;
        private final String current;
        private final String target;

        private Row(ProgressOverlayEntry entry) {
            this.entry = entry;
            current = formatProgress(entry.getCurrent());
            target = formatProgress(entry.getTarget());
            ratio = entry.getTarget() <= 0 ? 0 : Math.max(0, Math.min(1, entry.getCurrent() / (double) entry.getTarget()));
            percentage = entry.isReady() && entry.getTarget() > 0 ? entry.isComplete() ? "Done" : (int) Math.floor(ratio * 100) + "%" : "";
            progress = entry.isReady() ? entry.getTarget() > 0 ? current + " / " + target : entry.isComplete() ? "Complete" : "In progress" : "Waiting for game data";
        }
    }

    private static String formatProgress(long value) {
        return value >= 1_000_000 ? NumberFormats.formatTarget(value) : String.format(Locale.ROOT, "%,d", value);
    }

    private static int order(RepeatSchedule schedule) {
        return schedule == RepeatSchedule.NONE ? 4 : schedule.ordinal();
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/20/2026
     */
    @Value
    private static class Snapshot {
        List<Row> entries;
        ProgressOverlayView view;
        int total;
        int completed;
        int width;
        boolean bars;
        boolean grouped;
        OverlayTheme theme;
    }
}
