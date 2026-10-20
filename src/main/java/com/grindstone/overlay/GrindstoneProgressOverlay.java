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
import net.runelite.client.ui.FontManager;
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
    private static final int SECTION_HEIGHT = 20;
    private static final Color ROW_BACKGROUND = new Color(255, 255, 255, 8);
    private static final Color COMPLETED_BACKGROUND = new Color(104, 218, 151, 12);
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
        int rowHeight = config.progressOverlayBars() ? 44 : 36;
        int available = Math.max(1, (Math.max(180, canvasHeight) - HEADER_HEIGHT - 44) / (rowHeight + (grouped ? SECTION_HEIGHT : 0)));
        int limit = Math.max(1, Math.min(20, Math.min(config.progressOverlayRows(), available)));
        int completed = (int) visible.stream().filter(ProgressOverlayEntry::isComplete).count();
        snapshot = new Snapshot(List.copyOf(visible.subList(0, Math.min(limit, visible.size()))), view, visible.size(), completed, Math.max(180, Math.min(360, config.progressOverlayWidth())), config.progressOverlayBars(), grouped);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Snapshot current = snapshot;
        if (!config.enabled() || !config.progressOverlayEnabled() || current == null || current.entries.isEmpty()) {
            return null;
        }
        int sections = 0;
        RepeatSchedule previous = null;
        for (ProgressOverlayEntry entry : current.entries) {
            if (current.grouped && previous != entry.getSchedule()) {
                sections++;
            }
            previous = entry.getSchedule();
        }
        int rowHeight = current.bars ? 44 : 36;
        int height = HEADER_HEIGHT + current.entries.size() * rowHeight + sections * SECTION_HEIGHT + (current.total > current.entries.size() ? 24 : 4);
        Graphics2D card = (Graphics2D) graphics.create();
        try {
            OverlayStyle.frame(card, current.width, height, OverlayStyle.GOLD);
            card.drawImage(Icon.MAIN.getImage(), 8, 8, 28, 28, null);
            card.setFont(FontManager.getRunescapeBoldFont());
            card.setColor(OverlayStyle.TEXT);
            card.drawString(current.view.getTitle(), 44, 21);
            card.setFont(FontManager.getRunescapeSmallFont());
            card.setColor(OverlayStyle.MUTED);
            card.drawString("Grindstone", 44, 35);
            long now = System.currentTimeMillis();
            String summary = current.completed + " / " + current.total + " complete";
            if (current.view == ProgressOverlayView.MILESTONES) {
                summary = current.total + " milestones tracked";
            } else if (current.view == ProgressOverlayView.DAILY) {
                summary += " · resets " + OverlayStyle.formatCountdown(RepeatSchedule.DAILY.getNextReset(RepeatSchedule.DAILY.getPeriodStart(now)), now);
            }
            card.drawString(OverlayStyle.fit(summary, card.getFontMetrics(), current.width - 20), 10, 51);
            int y = HEADER_HEIGHT;
            previous = null;
            for (ProgressOverlayEntry entry : current.entries) {
                if (current.grouped && previous != entry.getSchedule()) {
                    card.setFont(FontManager.getRunescapeSmallFont());
                    card.setColor(OverlayStyle.GOLD);
                    String section = entry.getSchedule() == RepeatSchedule.NONE ? "Milestones" : entry.getSchedule().toString();
                    card.drawString(section, 10, y + 11);
                    card.setColor(DIVIDER);
                    int lineStart = 18 + card.getFontMetrics().stringWidth(section);
                    card.drawLine(lineStart, y + 7, current.width - 10, y + 7);
                    y += SECTION_HEIGHT;
                }
                renderEntry(card, entry, current.width, y, current.bars);
                y += rowHeight;
                previous = entry.getSchedule();
            }
            if (current.total > current.entries.size()) {
                card.setFont(FontManager.getRunescapeSmallFont());
                card.setColor(OverlayStyle.MUTED);
                card.drawString((current.total - current.entries.size()) + " more in the sidebar", 10, y + 12);
            }
        } finally {
            card.dispose();
        }
        return new Dimension(current.width, height);
    }

    private static void renderEntry(Graphics2D graphics, ProgressOverlayEntry entry, int width, int y, boolean bars) {
        Color accent = entry.isComplete() ? OverlayStyle.GREEN : entry.isReady() ? OverlayStyle.GOLD : OverlayStyle.MUTED;
        graphics.setColor(entry.isComplete() ? COMPLETED_BACKGROUND : ROW_BACKGROUND);
        graphics.fillRoundRect(6, y, width - 12, bars ? 40 : 32, 6, 6);
        if (entry.isPinned()) {
            graphics.setColor(OverlayStyle.GOLD);
            graphics.fillRoundRect(6, y + 6, 2, bars ? 28 : 20, 2, 2);
        }
        int titleX = 14;
        if (entry.isComplete()) {
            OverlayStyle.check(graphics, 14, y + 4, accent);
            titleX = 28;
        }
        double ratio = entry.getTarget() <= 0 ? 0 : Math.max(0, Math.min(1, entry.getCurrent() / (double) entry.getTarget()));
        String percentage = entry.isReady() && entry.getTarget() > 0 ? Math.round(ratio * 100) + "%" : "";
        graphics.setFont(FontManager.getRunescapeSmallFont());
        int percentageWidth = graphics.getFontMetrics().stringWidth(percentage);
        graphics.setColor(accent);
        graphics.drawString(percentage, width - 14 - percentageWidth, y + 13);
        graphics.setFont(FontManager.getRunescapeBoldFont());
        graphics.setColor(entry.isComplete() ? OverlayStyle.GREEN : OverlayStyle.TEXT);
        graphics.drawString(OverlayStyle.fit(entry.getTitle(), graphics.getFontMetrics(), width - titleX - 14 - (percentage.isEmpty() ? 0 : percentageWidth + 10)), titleX, y + 13);
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(OverlayStyle.MUTED);
        String progress = entry.isReady() ? entry.getTarget() > 0 ? formatProgress(entry.getCurrent()) + " / " + formatProgress(entry.getTarget()) : entry.isComplete() ? "Complete" : "In progress" : "Waiting for game data";
        graphics.drawString(OverlayStyle.fit(progress, graphics.getFontMetrics(), width - 28), 14, y + 27);
        if (bars) {
            OverlayStyle.progress(graphics, 14, y + 33, width - 28, entry.isReady() ? ratio : 0, accent);
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
        List<ProgressOverlayEntry> entries;
        ProgressOverlayView view;
        int total;
        int completed;
        int width;
        boolean bars;
        boolean grouped;
    }
}
