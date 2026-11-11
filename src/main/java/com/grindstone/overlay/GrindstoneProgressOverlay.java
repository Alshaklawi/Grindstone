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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
@Singleton
public final class GrindstoneProgressOverlay extends Overlay {
    private static final int SECTION_HEIGHT = 24;
    private static final int SOURCE_HEIGHT = 24;
    private static final int MAX_CACHE_PIXELS = 4_000_000;
    private final GrindstoneConfig config;
    private final LongSupplier clock;
    private volatile Snapshot snapshot;

    @Inject
    public GrindstoneProgressOverlay(GrindstoneConfig config) {
        this(config, System::currentTimeMillis);
    }

    GrindstoneProgressOverlay(GrindstoneConfig config, LongSupplier clock) {
        this.config = config;
        this.clock = clock;
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
        List<ProgressOverlayEntry> selected = new ArrayList<>();
        for (ProgressOverlayEntry entry : visible) {
            int cost = rowHeight + (grouped && previous != entry.getSchedule() ? SECTION_HEIGHT : 0);
            if (selected.size() >= limit || height + cost > budget) {
                break;
            }
            selected.add(entry);
            height += cost;
            previous = entry.getSchedule();
        }
        List<Row> rows = new ArrayList<>(selected.size());
        for (ProgressOverlayEntry entry : selected) {
            int detailHeight = entry.getSources().isEmpty() ? 0 : 8 + entry.getSources().size() * SOURCE_HEIGHT + (entry.getRemainingSources() > 0 ? 14 : 0);
            if (height + detailHeight > budget) {
                detailHeight = 0;
            }
            rows.add(new Row(entry, detailHeight));
            height += detailHeight;
        }
        int completed = (int) visible.stream().filter(ProgressOverlayEntry::isComplete).count();
        Snapshot next = new Snapshot(List.copyOf(rows), view, visible.size(), completed, Math.max(180, Math.min(360, config.progressOverlayWidth())), bars, grouped, theme);
        for (Row row : rows) {
            watchIcon(row.entry.getIcon(), next);
            if (row.detailHeight > 0) {
                for (ProgressOverlayEntry.Source source : row.entry.getSources()) {
                    watchIcon(source.getIcon(), next);
                }
            }
        }
        snapshot = next;
    }

    private static void watchIcon(BufferedImage image, Snapshot snapshot) {
        if (image instanceof AsyncBufferedImage) {
            ((AsyncBufferedImage) image).onLoaded(snapshot.imageVersion::incrementAndGet);
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Snapshot current = snapshot;
        if (!config.enabled() || !config.progressOverlayEnabled() || current == null || current.entries.isEmpty()) {
            return null;
        }
        RenderCache cache = current.cache;
        long now = clock.getAsLong();
        if (current.view == ProgressOverlayView.DAILY && now / 1000 != cache.countdownSecond) {
            cache.countdownSecond = now / 1000;
            cache.countdown = "Resets in " + OverlayStyle.formatCountdown(RepeatSchedule.DAILY.getNextReset(RepeatSchedule.DAILY.getPeriodStart(now)), now);
        }
        AffineTransform transform = graphics.getTransform();
        double scaleX = Math.max(1, Math.hypot(transform.getScaleX(), transform.getShearY()));
        double scaleY = Math.max(1, Math.hypot(transform.getScaleY(), transform.getShearX()));
        if (cache.height == 0) {
            cache.height = getHeight(current);
        }
        double imageWidth = Math.ceil(current.width * scaleX);
        double imageHeight = Math.ceil(cache.height * scaleY);
        if (!Double.isFinite(imageWidth * imageHeight) || imageWidth * imageHeight > MAX_CACHE_PIXELS || transform.getShearX() != 0 || transform.getShearY() != 0) {
            cache.image = null;
            Graphics2D card = (Graphics2D) graphics.create();
            try {
                renderContent(card, current, cache.height, cache.countdown);
            } finally {
                card.dispose();
            }
            return new Dimension(current.width, cache.height);
        }
        int imageVersion = current.imageVersion.get();
        if (cache.image == null || cache.imageVersion != imageVersion || cache.scaleX != scaleX || cache.scaleY != scaleY || !cache.countdown.equals(cache.renderedCountdown)) {
            BufferedImage image = new BufferedImage((int) imageWidth, (int) imageHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D card = image.createGraphics();
            try {
                card.setRenderingHints(graphics.getRenderingHints());
                card.scale(scaleX, scaleY);
                renderContent(card, current, cache.height, cache.countdown);
            } finally {
                card.dispose();
            }
            cache.image = image;
            cache.imageVersion = imageVersion;
            cache.scaleX = scaleX;
            cache.scaleY = scaleY;
            cache.inverseScale = AffineTransform.getScaleInstance(1 / scaleX, 1 / scaleY);
            cache.renderedCountdown = cache.countdown;
        }
        graphics.drawImage(cache.image, cache.inverseScale, null);
        return new Dimension(current.width, cache.height);
    }

    private static int getHeight(Snapshot current) {
        int sections = 0;
        int details = 0;
        RepeatSchedule previous = null;
        for (Row row : current.entries) {
            ProgressOverlayEntry entry = row.entry;
            if (current.grouped && previous != entry.getSchedule()) {
                sections++;
            }
            previous = entry.getSchedule();
            details += row.detailHeight;
        }
        boolean hidden = current.total > current.entries.size();
        return OverlayStyle.getHeaderHeight(current.theme) + current.entries.size() * OverlayStyle.getRowHeight(current.theme, current.bars) + sections * SECTION_HEIGHT + details + getFooterHeight(current.view, hidden);
    }

    private static void renderContent(Graphics2D card, Snapshot current, int height, String countdown) {
        int rowHeight = OverlayStyle.getRowHeight(current.theme, current.bars);
        boolean hidden = current.total > current.entries.size();
        OverlayStyle.Palette palette = OverlayStyle.getPalette(current.theme);
        OverlayStyle.panel(card, current.theme, current.width, height, false);
        renderHeader(card, current, palette);
        int y = OverlayStyle.getHeaderHeight(current.theme);
        RepeatSchedule previous = null;
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
            if (row.detailHeight > 0) {
                renderSources(card, row, current.width, y, current.bars, palette);
                y += row.detailHeight;
            }
            previous = entry.getSchedule();
        }
        card.setFont(OverlayStyle.SMALL_FONT);
        card.setColor(palette.getMuted());
        if (current.view == ProgressOverlayView.DAILY) {
            card.drawString(countdown, (current.width - card.getFontMetrics().stringWidth(countdown)) / 2, y + 16);
            y += 24;
        }
        if (hidden) {
            card.setFont(OverlayStyle.BODY_FONT);
            card.drawString(OverlayStyle.fit((current.total - current.entries.size()) + " more in the sidebar", card.getFontMetrics(), current.width - 24), 12, y + 14);
        }
    }

    private static void renderSources(Graphics2D graphics, Row row, int width, int y, boolean bars, OverlayStyle.Palette palette) {
        graphics.setColor(palette.getSurface());
        graphics.fillRoundRect(12, y + 1, width - 24, row.detailHeight - 5, 6, 6);
        graphics.setFont(OverlayStyle.SMALL_FONT);
        int top = y + 4;
        for (ProgressOverlayEntry.Source source : row.entry.getSources()) {
            if (source.getIcon() != null) {
                OverlayStyle.icon(graphics, source.getIcon(), 17, top + 2, 14);
            }
            String value = source.isReady() ? formatSourceProgress(source.getCurrent()) + " / " + formatSourceProgress(source.getTarget()) : "Waiting";
            int valueWidth = graphics.getFontMetrics().stringWidth(value);
            graphics.setColor(palette.getText());
            graphics.drawString(OverlayStyle.fit(source.getTitle(), graphics.getFontMetrics(), width - 61 - valueWidth), 36, top + 13);
            graphics.setColor(palette.getMuted());
            graphics.drawString(value, width - 18 - valueWidth, top + 13);
            if (bars && source.isReady() && source.getTarget() > 0) {
                OverlayStyle.progress(graphics, 36, top + 17, width - 54, 2, source.getCurrent() / (double) source.getTarget(), palette.getAccent(), palette.getTrack());
            }
            top += SOURCE_HEIGHT;
        }
        if (row.entry.getRemainingSources() > 0) {
            graphics.setColor(palette.getMuted());
            graphics.drawString("+" + row.entry.getRemainingSources() + " more in sidebar", 36, top + 9);
        }
    }

    private static String formatSourceProgress(long value) {
        return value >= 1_000_000_000L ? NumberFormats.formatTarget(value) : NumberFormats.formatExperience(value);
    }

    private static int getFooterHeight(ProgressOverlayView view, boolean hidden) {
        return (view == ProgressOverlayView.DAILY ? 24 : 0) + (hidden ? 24 : 8);
    }

    private static void renderHeader(Graphics2D graphics, Snapshot current, OverlayStyle.Palette palette) {
        if (current.theme == OverlayTheme.GUILD) {
            renderGuildHeader(graphics, current, palette);
            return;
        }
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
        OverlayStyle.icon(graphics, Icon.MAIN.getImage(), 13, 22, 22);
        int titleX = 43;
        graphics.setFont(OverlayStyle.SMALL_FONT);
        graphics.setColor(palette.getMuted());
        graphics.drawString("GRINDSTONE", titleX, 23);
        String tally = current.view == ProgressOverlayView.MILESTONES ? Integer.toString(current.total) : current.completed + " / " + current.total;
        int tallyWidth = graphics.getFontMetrics().stringWidth(tally) + 16;
        int tallyX = current.width - tallyWidth - 12;
        graphics.setColor(new Color(0x293e3b));
        graphics.fillRoundRect(tallyX, 30, tallyWidth, 22, 22, 22);
        graphics.setColor(palette.getSuccess());
        graphics.drawString(tally, tallyX + 8, 45);
        graphics.setFont(OverlayStyle.HEADING_FONT);
        graphics.setColor(palette.getText());
        graphics.drawString(OverlayStyle.fit(current.view.getTitle(), graphics.getFontMetrics(), tallyX - titleX - 6), titleX, 44);
    }

    private static void renderGuildHeader(Graphics2D graphics, Snapshot current, OverlayStyle.Palette palette) {
        graphics.setColor(palette.getSurface());
        graphics.fillRoundRect(4, 4, current.width - 8, 30, 6, 6);
        graphics.setColor(palette.getBorder());
        graphics.drawLine(4, 35, current.width - 5, 35);
        OverlayStyle.icon(graphics, Icon.MAIN.getImage(), 12, 10, 18);
        String tally = current.view == ProgressOverlayView.MILESTONES ? Integer.toString(current.total) : current.completed + " / " + current.total;
        graphics.setFont(OverlayStyle.SMALL_FONT);
        int tallyWidth = graphics.getFontMetrics().stringWidth(tally);
        graphics.setColor(palette.getSuccess());
        graphics.drawString(tally, current.width - 12 - tallyWidth, 24);
        graphics.setFont(OverlayStyle.GUILD_HEADING_FONT);
        graphics.setColor(palette.getAccent());
        graphics.drawString(OverlayStyle.fit(current.view.getTitle(), graphics.getFontMetrics(), current.width - tallyWidth - 62), 38, 24);
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
        private final int detailHeight;

        private Row(ProgressOverlayEntry entry, int detailHeight) {
            this.entry = entry;
            this.detailHeight = detailHeight;
            current = formatProgress(entry.getCurrent());
            target = formatProgress(entry.getTarget()) + (entry.getUnit().isEmpty() ? "" : " " + entry.getUnit());
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
        private final AtomicInteger imageVersion = new AtomicInteger();
        private final RenderCache cache = new RenderCache();
        List<Row> entries;
        ProgressOverlayView view;
        int total;
        int completed;
        int width;
        boolean bars;
        boolean grouped;
        OverlayTheme theme;
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 11/04/2026
     */
    private static final class RenderCache {
        private BufferedImage image;
        private int imageVersion;
        private int height;
        private double scaleX;
        private double scaleY;
        private AffineTransform inverseScale;
        private long countdownSecond = Long.MIN_VALUE;
        private String countdown = "";
        private String renderedCountdown = "";
    }
}
