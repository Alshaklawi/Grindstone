package com.grindstone.notification;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.config.OverlayTheme;
import com.grindstone.overlay.OverlayStyle;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
 * @since 10/19/2026
 */
@Singleton
public final class MilestoneCompletionOverlay extends Overlay {
    private static final int WIDTH = 320;
    private static final int MAX_ENTRIES = 50;
    private static final int ANIMATION_MARGIN = 14;
    private static final long ENTER_NANOS = 320_000_000L;
    private static final long EXIT_NANOS = 300_000_000L;
    private final GrindstoneConfig config;
    private final LongSupplier clock;
    private final Map<String, CompletionOverlayEntry> pending = new LinkedHashMap<>();
    private boolean pendingRoutinesOnly = true;
    private Popup popup;

    @Inject
    public MilestoneCompletionOverlay(GrindstoneConfig config) {
        this(config, System::nanoTime);
    }

    MilestoneCompletionOverlay(GrindstoneConfig config, LongSupplier clock) {
        this.config = config;
        this.clock = clock;
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    public synchronized void show(List<CompletionOverlayEntry> entries, boolean routinesOnly) {
        if (!config.completionPopup()) {
            clear();
            return;
        }
        if (entries.isEmpty()) {
            return;
        }
        long now = clock.getAsLong();
        Popup current = advancePopup(now);
        boolean added = false;
        for (CompletionOverlayEntry entry : entries) {
            if (current != null && current.entries.stream().anyMatch(active -> active.getId().equals(entry.getId()))) {
                continue;
            }
            if (pending.size() < MAX_ENTRIES || pending.containsKey(entry.getId())) {
                pending.put(entry.getId(), entry);
                added = true;
            }
        }
        if (added) {
            pendingRoutinesOnly &= routinesOnly;
        }
        if (popup == null && !pending.isEmpty()) {
            startNext(now);
        }
    }

    public synchronized void preview(BufferedImage icon) {
        clear();
        List<CompletionOverlayEntry> entries = List.of(new CompletionOverlayEntry("preview", "The Fractured Archive", "5 raids completed", icon));
        popup = createPopup(entries, false, clock.getAsLong(), true);
    }

    public synchronized void clear() {
        popup = null;
        pending.clear();
        pendingRoutinesOnly = true;
    }

    private synchronized Popup advancePopup(long now) {
        if (!config.completionPopup() && (popup == null || !popup.preview)) {
            clear();
            return null;
        }
        if (popup != null && now - popup.startedAt >= popup.duration) {
            popup = null;
        }
        if (popup == null && !pending.isEmpty()) {
            startNext(now);
        }
        return popup;
    }

    private void startNext(long now) {
        popup = createPopup(List.copyOf(pending.values()), pendingRoutinesOnly, now, false);
        pending.clear();
        pendingRoutinesOnly = true;
    }

    private Popup createPopup(List<CompletionOverlayEntry> entries, boolean routinesOnly, long now, boolean preview) {
        BufferedImage image = new BufferedImage(WIDTH, getHeight(entries.size()), BufferedImage.TYPE_INT_ARGB);
        Popup next = new Popup(entries, routinesOnly, now, Math.max(3, Math.min(15, config.completionPopupSeconds())) * 1_000_000_000L, image, config.completionPopupAnimations(), preview);
        renderContent(next);
        for (CompletionOverlayEntry entry : entries.subList(0, Math.min(3, entries.size()))) {
            if (entry.getIcon() instanceof AsyncBufferedImage) {
                ((AsyncBufferedImage) entry.getIcon()).onLoaded(() -> next.iconDirty.set(true));
            }
        }
        return next;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        long now = clock.getAsLong();
        Popup current = advancePopup(now);
        if (current == null) {
            return null;
        }
        if (current.iconDirty.getAndSet(false)) {
            renderContent(current);
        }
        long age = Math.max(0, now - current.startedAt);
        int height = current.image.getHeight();
        double entering = Math.min(1, age / (double) (current.animated ? ENTER_NANOS : 80_000_000L));
        double leaving = Math.max(0, Math.min(1, (age - current.duration + (current.animated ? EXIT_NANOS : 80_000_000L)) / (double) (current.animated ? EXIT_NANOS : 80_000_000L)));
        float alpha = (float) (smoothStep(entering) * (1 - smoothStep(leaving)));
        Graphics2D card = (Graphics2D) graphics.create();
        try {
            card.setComposite(AlphaComposite.SrcOver.derive(alpha));
            card.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (current.animated) {
                animate(card, height, entering, leaving);
            } else {
                card.translate(ANIMATION_MARGIN, ANIMATION_MARGIN);
            }
            card.drawImage(current.image, 0, 0, null);
        } finally {
            card.dispose();
        }
        return new Dimension(WIDTH + ANIMATION_MARGIN * 2, height + ANIMATION_MARGIN * 2);
    }

    private static int getHeight(int entries) {
        int shown = Math.min(3, entries);
        return 84 + (shown - 1) * 50 + (entries > shown ? 20 : 0);
    }

    private static void renderContent(Popup current) {
        int shown = Math.min(3, current.entries.size());
        int height = current.image.getHeight();
        Graphics2D card = current.image.createGraphics();
        try {
            card.setComposite(AlphaComposite.Clear);
            card.fillRect(0, 0, WIDTH, height);
            card.setComposite(AlphaComposite.SrcOver);
            OverlayStyle.panel(card, OverlayTheme.GUILD, WIDTH, height, true);
            OverlayStyle.Palette palette = OverlayStyle.getPalette(OverlayTheme.GUILD);
            renderGuild(card, current, shown, palette);
            if (current.entries.size() > shown) {
                card.setFont(OverlayStyle.SMALL_FONT);
                card.setColor(palette.getMuted());
                card.drawString("And " + (current.entries.size() - shown) + " more in the sidebar", 14, height - 10);
            }
        } finally {
            card.dispose();
        }
    }

    private static double smoothStep(double progress) {
        return progress * progress * (3 - 2 * progress);
    }

    private static void animate(Graphics2D graphics, int height, double entering, double leaving) {
        double entrance = (1 - entering) * (1 - entering) * (1 - entering);
        double exit = leaving * leaving;
        double offsetY = -10 * entrance - 8 * exit;
        double scale = 1 - 0.025 * entrance - 0.015 * exit;
        graphics.translate(ANIMATION_MARGIN + WIDTH * (1 - scale) / 2, ANIMATION_MARGIN + offsetY + height * (1 - scale) / 2);
        graphics.scale(scale, scale);
    }

    private static String getHeading(Popup current) {
        return current.entries.size() == 1 ? current.routinesOnly ? "Routine complete" : "Milestone reached" : current.entries.size() + (current.routinesOnly ? " routines complete" : " milestones reached");
    }

    private static void renderGuild(Graphics2D card, Popup current, int shown, OverlayStyle.Palette palette) {
        card.setColor(palette.getAccent());
        card.drawOval(13, 23, 38, 38);
        card.setColor(new Color(0x77613e));
        card.drawOval(17, 27, 30, 30);
        OverlayStyle.icon(card, current.entries.get(0).getIcon(), 18, 28, 28);
        card.setFont(OverlayStyle.SMALL_FONT);
        card.setColor(palette.getAccent());
        card.drawString(getHeading(current), 65, 24);
        card.setFont(OverlayStyle.GUILD_TITLE_FONT);
        card.setColor(palette.getText());
        card.drawString(OverlayStyle.fit(current.entries.get(0).getTitle(), card.getFontMetrics(), WIDTH - 91), 65, 44);
        card.setFont(OverlayStyle.BODY_FONT);
        card.setColor(palette.getMuted());
        card.drawString(OverlayStyle.fit(current.entries.get(0).getDetail(), card.getFontMetrics(), WIDTH - 91), 65, 62);
        OverlayStyle.check(card, WIDTH - 25, 37, palette.getSuccess());
        for (int index = 1; index < shown; index++) {
            CompletionOverlayEntry entry = current.entries.get(index);
            int y = 84 + (index - 1) * 50;
            card.setColor(new Color(0x494235));
            card.drawLine(16, y - 6, WIDTH - 17, y - 6);
            OverlayStyle.icon(card, entry.getIcon(), 19, y + 2, 24);
            card.setFont(OverlayStyle.GUILD_TITLE_FONT);
            card.setColor(palette.getText());
            card.drawString(OverlayStyle.fit(entry.getTitle(), card.getFontMetrics(), WIDTH - 72), 54, y + 16);
            card.setFont(OverlayStyle.BODY_FONT);
            card.setColor(palette.getMuted());
            card.drawString(OverlayStyle.fit(entry.getDetail(), card.getFontMetrics(), WIDTH - 72), 54, y + 34);
        }
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/20/2026
     */
    @Value
    private static class Popup {
        private final AtomicBoolean iconDirty = new AtomicBoolean();
        List<CompletionOverlayEntry> entries;
        boolean routinesOnly;
        long startedAt;
        long duration;
        BufferedImage image;
        boolean animated;
        boolean preview;
    }
}
