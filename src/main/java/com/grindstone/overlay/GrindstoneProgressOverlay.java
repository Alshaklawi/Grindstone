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
        int rowHeight = config.progressOverlayBars() ? 44 : 35;
        int available = Math.max(1, (Math.max(180, canvasHeight) - 110) / (rowHeight + (grouped ? 18 : 0)));
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
        int height = 62 + current.entries.size() * (current.bars ? 44 : 35) + sections * 18 + (current.total > current.entries.size() ? 19 : 0);
        Graphics2D card = (Graphics2D) graphics.create();
        try {
            OverlayStyle.frame(card, current.width, height, OverlayStyle.GOLD);
            card.drawImage(Icon.MAIN.getImage(), 6, 6, 20, 20, null);
            card.setFont(FontManager.getRunescapeSmallFont());
            card.setColor(OverlayStyle.MUTED);
            card.drawString("GRINDSTONE", 34, 16);
            card.setFont(FontManager.getRunescapeBoldFont());
            card.setColor(OverlayStyle.TEXT);
            card.drawString(current.view.getTitle(), 34, 32);
            card.setFont(FontManager.getRunescapeSmallFont());
            card.setColor(OverlayStyle.MUTED);
            long now = System.currentTimeMillis();
            String summary = current.completed + " of " + current.total + " done";
            if (current.view == ProgressOverlayView.MILESTONES) {
                summary = current.total + " milestones tracked";
            } else if (current.view == ProgressOverlayView.DAILY) {
                summary += " · resets in " + OverlayStyle.formatCountdown(RepeatSchedule.DAILY.getNextReset(RepeatSchedule.DAILY.getPeriodStart(now)), now);
            }
            card.drawString(OverlayStyle.fit(summary, card.getFontMetrics(), current.width - 12), 6, 48);
            card.setColor(new Color(80, 83, 88, 100));
            card.drawLine(6, 56, current.width - 6, 56);
            int y = 64;
            previous = null;
            for (ProgressOverlayEntry entry : current.entries) {
                if (current.grouped && previous != entry.getSchedule()) {
                    card.setFont(FontManager.getRunescapeSmallFont());
                    card.setColor(OverlayStyle.GOLD);
                    String section = entry.getSchedule() == RepeatSchedule.NONE ? "MILESTONES" : entry.getSchedule().toString().toUpperCase(Locale.ROOT);
                    card.drawString(section, 6, y + 8);
                    y += 18;
                }
                renderEntry(card, entry, current.width, y, current.bars);
                y += current.bars ? 44 : 35;
                previous = entry.getSchedule();
            }
            if (current.total > current.entries.size()) {
                card.setFont(FontManager.getRunescapeSmallFont());
                card.setColor(OverlayStyle.MUTED);
                card.drawString((current.total - current.entries.size()) + " more in the sidebar", 6, y + 8);
            }
        } finally {
            card.dispose();
        }
        return new Dimension(current.width, height);
    }

    private static void renderEntry(Graphics2D graphics, ProgressOverlayEntry entry, int width, int y, boolean bars) {
        Color accent = entry.isComplete() ? OverlayStyle.GREEN : entry.isReady() ? OverlayStyle.GOLD : OverlayStyle.MUTED;
        if (entry.isComplete()) {
            OverlayStyle.check(graphics, 6, y + 1, accent);
        } else {
            graphics.setColor(accent);
            graphics.fillOval(8, y + 3, 5, 5);
        }
        graphics.setFont(FontManager.getRunescapeBoldFont());
        graphics.setColor(entry.isComplete() ? OverlayStyle.GREEN : OverlayStyle.TEXT);
        graphics.drawString(OverlayStyle.fit(entry.getTitle(), graphics.getFontMetrics(), width - 28), 21, y + 10);
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(OverlayStyle.MUTED);
        String progress = entry.isReady() ? entry.getTarget() > 0 ? NumberFormats.formatExperience(entry.getCurrent()) + " / " + NumberFormats.formatExperience(entry.getTarget()) : entry.isComplete() ? "Complete" : "In progress" : "Waiting for client data";
        graphics.drawString(OverlayStyle.fit(progress, graphics.getFontMetrics(), width - 64), 21, y + 24);
        double ratio = entry.getTarget() <= 0 ? 0 : Math.min(1, entry.getCurrent() / (double) entry.getTarget());
        String percentage = entry.isReady() && entry.getTarget() > 0 ? Math.round(ratio * 100) + "%" : "";
        graphics.setColor(accent);
        graphics.drawString(percentage, width - 6 - graphics.getFontMetrics().stringWidth(percentage), y + 24);
        if (bars) {
            OverlayStyle.progress(graphics, 21, y + 31, width - 27, entry.isReady() ? ratio : 0, accent);
        }
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
