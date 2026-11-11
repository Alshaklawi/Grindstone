package com.grindstone.notification;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.config.OverlayTheme;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
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
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.FontManager;
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
    private static final int WIDTH = CompletionPopupStyle.WIDTH;
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
        List<CompletionOverlayEntry> entries = List.of(new CompletionOverlayEntry("preview", "The Fractured Archive", config.completionNumbers() ? "5 raids completed" : "Raid target reached", icon));
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
        List<CompletionOverlayEntry> entries = List.copyOf(pending.values()).subList(0, Math.min(3, pending.size()));
        popup = createPopup(entries, pendingRoutinesOnly, now, false);
        for (CompletionOverlayEntry entry : entries) {
            pending.remove(entry.getId());
        }
        if (pending.isEmpty()) {
            pendingRoutinesOnly = true;
        }
    }

    private Popup createPopup(List<CompletionOverlayEntry> entries, boolean routinesOnly, long now, boolean preview) {
        boolean animated = config.completionPopupAnimations();
        long duration = Math.max(3, Math.min(15, config.completionPopupSeconds())) * 1_000_000_000L + (animated ? ENTER_NANOS + EXIT_NANOS : 160_000_000L);
        Popup next = new Popup(entries, routinesOnly, now, duration, animated, preview, config.completionPopupTheme());
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
        if (current.theme == OverlayTheme.RUNELITE && !graphics.getFont().equals(current.font)) {
            current.font = graphics.getFont();
            current.iconDirty.set(true);
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
            card.clipRect(0, 0, WIDTH + ANIMATION_MARGIN * 2, height + ANIMATION_MARGIN * 2);
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

    private static void renderContent(Popup current) {
        int height = CompletionPopupStyle.getHeight(current.entries.size(), current.theme, current.font);
        BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D card = image.createGraphics();
        try {
            card.setComposite(AlphaComposite.Clear);
            card.fillRect(0, 0, WIDTH, height);
            card.setComposite(AlphaComposite.SrcOver);
            CompletionPopupStyle.render(card, current.theme, current.entries, current.routinesOnly, current.font);
        } finally {
            card.dispose();
        }
        current.image = image;
    }

    private static double smoothStep(double progress) {
        return progress * progress * (3 - 2 * progress);
    }

    private static void animate(Graphics2D graphics, int height, double entering, double leaving) {
        double entrance = 1 - smoothStep(entering);
        double exit = smoothStep(leaving);
        int offsetY = (int) Math.round(-(height + ANIMATION_MARGIN) * Math.max(entrance, exit));
        graphics.translate(ANIMATION_MARGIN, ANIMATION_MARGIN + offsetY);
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/20/2026
     */
    @RequiredArgsConstructor
    private static class Popup {
        private final AtomicBoolean iconDirty = new AtomicBoolean();
        private final List<CompletionOverlayEntry> entries;
        private final boolean routinesOnly;
        private final long startedAt;
        private final long duration;
        private final boolean animated;
        private final boolean preview;
        private final OverlayTheme theme;
        private BufferedImage image;
        private Font font = FontManager.getRunescapeFont();
    }
}
