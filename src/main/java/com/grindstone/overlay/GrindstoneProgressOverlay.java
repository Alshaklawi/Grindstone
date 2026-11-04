package com.grindstone.overlay;

import com.grindstone.config.GrindstoneConfig;
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
    private static final int HEADER_HEIGHT = 60;
    private static final int SECTION_HEIGHT = 24;
    private static final Color DIVIDER = new Color(163, 168, 176, 40);
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
        int rowHeight = bars ? 50 : 42;
        int limit = Math.max(1, Math.min(20, config.progressOverlayRows()));
        int budget = Math.max(164, canvasHeight - 32);
        int height = HEADER_HEIGHT + 24;
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
        snapshot = new Snapshot(List.copyOf(rows), view, visible.size(), completed, Math.max(180, Math.min(360, config.progressOverlayWidth())), bars, grouped);
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
        int rowHeight = current.bars ? 50 : 42;
        int height = HEADER_HEIGHT + current.entries.size() * rowHeight + sections * SECTION_HEIGHT + (current.total > current.entries.size() ? 24 : 8);
        Graphics2D card = (Graphics2D) graphics.create();
        try {
            OverlayStyle.frame(card, current.width, height, OverlayStyle.GOLD);
            card.drawImage(Icon.MAIN.getImage(), 12, 13, 22, 22, null);
            card.setFont(OverlayStyle.HEADING_FONT);
            card.setColor(OverlayStyle.TEXT);
            card.drawString(OverlayStyle.fit(current.view.getTitle(), card.getFontMetrics(), current.width - 54), 40, 25);
            card.setFont(OverlayStyle.BODY_FONT);
            card.setColor(OverlayStyle.MUTED);
            long now = System.currentTimeMillis();
            String summary = current.view == ProgressOverlayView.MILESTONES ? current.total + " active" : current.completed + " of " + current.total + " complete";
            card.drawString(summary, 12, 43);
            if (current.view == ProgressOverlayView.DAILY) {
                String reset = "Resets in " + OverlayStyle.formatCountdown(RepeatSchedule.DAILY.getNextReset(RepeatSchedule.DAILY.getPeriodStart(now)), now);
                if (card.getFontMetrics().stringWidth(summary) + card.getFontMetrics().stringWidth(reset) + 36 <= current.width) {
                    card.drawString(reset, current.width - 12 - card.getFontMetrics().stringWidth(reset), 43);
                }
            }
            int y = HEADER_HEIGHT;
            previous = null;
            for (Row row : current.entries) {
                ProgressOverlayEntry entry = row.entry;
                if (current.grouped && previous != entry.getSchedule()) {
                    card.setFont(OverlayStyle.BODY_FONT);
                    card.setFont(OverlayStyle.SMALL_FONT);
                    card.setColor(OverlayStyle.MUTED);
                    String section = entry.getSchedule() == RepeatSchedule.NONE ? "Milestones" : entry.getSchedule().toString();
                    card.drawString(section, 12, y + 15);
                    card.setColor(DIVIDER);
                    int lineStart = 20 + card.getFontMetrics().stringWidth(section);
                    card.drawLine(lineStart, y + 11, current.width - 12, y + 11);
                    y += SECTION_HEIGHT;
                }
                renderEntry(card, row, current.width, y, current.bars);
                y += rowHeight;
                previous = entry.getSchedule();
            }
            if (current.total > current.entries.size()) {
                card.setFont(OverlayStyle.BODY_FONT);
                card.setColor(OverlayStyle.MUTED);
                card.drawString((current.total - current.entries.size()) + " more in the sidebar", 12, y + 14);
            }
        } finally {
            card.dispose();
        }
        return new Dimension(current.width, height);
    }

    private static void renderEntry(Graphics2D graphics, Row row, int width, int y, boolean bars) {
        ProgressOverlayEntry entry = row.entry;
        Color accent = entry.isComplete() ? OverlayStyle.GREEN : entry.isReady() ? OverlayStyle.GOLD : OverlayStyle.MUTED;
        graphics.setColor(OverlayStyle.SURFACE);
        graphics.fillRoundRect(7, y + 1, width - 14, bars ? 46 : 38, 8, 8);
        int titleX = 14;
        if (entry.isComplete()) {
            OverlayStyle.check(graphics, 12, y + 6, accent);
            titleX = 27;
        } else if (entry.isPinned()) {
            graphics.setColor(OverlayStyle.GOLD);
            graphics.fillOval(13, y + 9, 4, 4);
            titleX = 24;
        }
        graphics.setFont(OverlayStyle.TITLE_FONT);
        graphics.setColor(entry.isComplete() ? OverlayStyle.GREEN : OverlayStyle.TEXT);
        graphics.drawString(OverlayStyle.fit(entry.getTitle(), graphics.getFontMetrics(), width - titleX - 12), titleX, y + 15);
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

        private Row(ProgressOverlayEntry entry) {
            this.entry = entry;
            ratio = entry.getTarget() <= 0 ? 0 : Math.max(0, Math.min(1, entry.getCurrent() / (double) entry.getTarget()));
            percentage = entry.isReady() && entry.getTarget() > 0 ? entry.isComplete() ? "Done" : (int) Math.floor(ratio * 100) + "%" : "";
            progress = entry.isReady() ? entry.getTarget() > 0 ? formatProgress(entry.getCurrent()) + " / " + formatProgress(entry.getTarget()) : entry.isComplete() ? "Complete" : "In progress" : "Waiting for game data";
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
    }
}
