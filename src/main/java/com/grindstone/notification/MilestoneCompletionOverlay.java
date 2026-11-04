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
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

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
        if (!config.enabled() || !config.completionPopup()) {
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
            if (current != null && current.entries.stream().anyMatch(active -> active.getTitle().equals(entry.getTitle()))) {
                continue;
            }
            if (pending.size() < MAX_ENTRIES || pending.containsKey(entry.getTitle())) {
                pending.put(entry.getTitle(), entry);
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

    public synchronized void clear() {
        popup = null;
        pending.clear();
        pendingRoutinesOnly = true;
    }

    private synchronized Popup advancePopup(long now) {
        if (!config.enabled() || !config.completionPopup()) {
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
        OverlayTheme theme = config.completionPopupTheme();
        BufferedImage image = new BufferedImage(WIDTH, getHeight(theme, pending.size()), BufferedImage.TYPE_INT_ARGB);
        popup = new Popup(List.copyOf(pending.values()), pendingRoutinesOnly, now, Math.max(3, Math.min(15, config.completionPopupSeconds())) * 1_000_000_000L, theme, image);
        pending.clear();
        pendingRoutinesOnly = true;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        long now = clock.getAsLong();
        Popup current = advancePopup(now);
        if (current == null) {
            return null;
        }
        long age = Math.max(0, now - current.startedAt);
        int height = current.image.getHeight();
        double entering = Math.min(1, age / (double) ENTER_NANOS);
        double leaving = Math.max(0, Math.min(1, (age - current.duration + EXIT_NANOS) / (double) EXIT_NANOS));
        float alpha = (float) (smoothStep(entering) * (1 - smoothStep(leaving)));
        renderContent(current);
        Graphics2D card = (Graphics2D) graphics.create();
        try {
            card.setComposite(AlphaComposite.SrcOver.derive(alpha));
            card.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            animate(card, current.theme, height, entering, leaving);
            card.drawImage(current.image, 0, 0, null);
        } finally {
            card.dispose();
        }
        return new Dimension(WIDTH + ANIMATION_MARGIN * 2, height + ANIMATION_MARGIN * 2);
    }

    private static int getHeight(OverlayTheme theme, int entries) {
        int shown = Math.min(3, entries);
        int height;
        switch (theme) {
            case GUILD:
                height = 84 + (shown - 1) * 50;
                break;
            case FOCUS:
                height = 60 + (shown - 1) * 50;
                break;
            case SLATE:
                height = shown == 1 ? 128 : 42 + shown * 62;
                break;
            default:
                height = 58 + shown * 22;
                break;
        }
        return height + (entries > shown ? 20 : 0);
    }

    private static void renderContent(Popup current) {
        int shown = Math.min(3, current.entries.size());
        int height = current.image.getHeight();
        OverlayTheme theme = current.theme;
        Graphics2D card = current.image.createGraphics();
        try {
            card.setComposite(AlphaComposite.Clear);
            card.fillRect(0, 0, WIDTH, height);
            card.setComposite(AlphaComposite.SrcOver);
            OverlayStyle.panel(card, theme, WIDTH, height, true);
            OverlayStyle.Palette palette = OverlayStyle.getPalette(theme);
            switch (theme) {
                case GUILD:
                    renderGuild(card, current, shown, palette);
                    break;
                case FOCUS:
                    renderFocus(card, current, shown, palette);
                    break;
                case SLATE:
                    renderSlate(card, current, shown, palette);
                    break;
                default:
                    renderClassic(card, current, shown);
                    break;
            }
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

    private static void animate(Graphics2D graphics, OverlayTheme theme, int height, double entering, double leaving) {
        double entrance = (1 - entering) * (1 - entering) * (1 - entering);
        double exit = leaving * leaving;
        double offsetX = 0;
        double offsetY;
        double scale = 1;
        switch (theme) {
            case SLATE:
                offsetY = 12 * entrance + 8 * exit;
                scale = 1 - 0.04 * entrance - 0.02 * exit;
                break;
            case GUILD:
                offsetY = -10 * entrance - 8 * exit;
                scale = 1 - 0.025 * entrance - 0.015 * exit;
                break;
            case FOCUS:
                offsetX = 12 * entrance + 10 * exit;
                offsetY = 0;
                break;
            default:
                offsetY = -8 * entrance - 6 * exit;
                break;
        }
        graphics.translate(ANIMATION_MARGIN + offsetX + WIDTH * (1 - scale) / 2, ANIMATION_MARGIN + offsetY + height * (1 - scale) / 2);
        graphics.scale(scale, scale);
    }

    private static String getHeading(Popup current) {
        return current.entries.size() == 1 ? current.routinesOnly ? "Routine complete" : "Milestone reached" : current.entries.size() + (current.routinesOnly ? " routines complete" : " milestones reached");
    }

    private static void renderClassic(Graphics2D card, Popup current, int shown) {
        card.setColor(new Color(98, 205, 174, 25));
        card.fillRoundRect(12, 12, 28, 28, 8, 8);
        OverlayStyle.check(card, 21, 22, OverlayStyle.GREEN);
        card.setFont(OverlayStyle.SMALL_FONT);
        card.setColor(OverlayStyle.MUTED);
        card.drawString("GRINDSTONE", 50, 22);
        card.setFont(OverlayStyle.HEADING_FONT);
        card.setColor(OverlayStyle.GREEN);
        card.drawString(getHeading(current), 50, 37);
        card.setFont(OverlayStyle.TITLE_FONT);
        card.setColor(OverlayStyle.TEXT);
        for (int index = 0; index < shown; index++) {
            CompletionOverlayEntry entry = current.entries.get(index);
            OverlayStyle.icon(card, entry.getIcon(), 12, 54 + index * 22, 18);
            card.drawString(OverlayStyle.fit(entry.getTitle(), card.getFontMetrics(), WIDTH - 48), 36, 68 + index * 22);
        }
    }

    private static void renderGuild(Graphics2D card, Popup current, int shown, OverlayStyle.Palette palette) {
        card.setColor(palette.getAccent());
        card.drawOval(13, 23, 38, 38);
        card.setColor(new Color(0x77613e));
        card.drawOval(17, 27, 30, 30);
        OverlayStyle.icon(card, current.entries.get(0).getIcon(), 22, 32, 20);
        card.setFont(OverlayStyle.SMALL_FONT);
        card.setColor(palette.getAccent());
        card.drawString(getHeading(current).toUpperCase(Locale.ROOT), 65, 24);
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

    private static void renderFocus(Graphics2D card, Popup current, int shown, OverlayStyle.Palette palette) {
        for (int index = 0; index < shown; index++) {
            CompletionOverlayEntry entry = current.entries.get(index);
            int y = 12 + index * 50;
            OverlayStyle.icon(card, entry.getIcon(), 12, y + 5, 22);
            card.setFont(OverlayStyle.TITLE_FONT);
            card.setColor(palette.getText());
            card.drawString(OverlayStyle.fit(entry.getTitle(), card.getFontMetrics(), WIDTH - 70), 45, y + 13);
            card.setFont(OverlayStyle.SMALL_FONT);
            card.setColor(palette.getMuted());
            String detail = (current.routinesOnly ? "Routine complete" : "Milestone complete") + (entry.getDetail().isEmpty() ? "" : " · " + entry.getDetail());
            card.drawString(OverlayStyle.fit(detail, card.getFontMetrics(), WIDTH - 70), 45, y + 30);
            OverlayStyle.check(card, WIDTH - 24, y + 12, palette.getSuccess());
        }
    }

    private static void renderSlate(Graphics2D card, Popup current, int shown, OverlayStyle.Palette palette) {
        card.setColor(new Color(0x314d46));
        card.fillOval(15, 12, 23, 23);
        OverlayStyle.check(card, 22, 20, palette.getSuccess());
        card.setFont(OverlayStyle.BODY_FONT);
        card.setColor(palette.getSuccess());
        card.drawString(getHeading(current), 46, 28);
        if (shown == 1) {
            CompletionOverlayEntry entry = current.entries.get(0);
            OverlayStyle.icon(card, entry.getIcon(), 17, 51, 28);
            card.setFont(OverlayStyle.DISPLAY_FONT);
            card.setColor(palette.getText());
            String title = entry.getTitle();
            String first = OverlayStyle.fit(title, card.getFontMetrics(), WIDTH - 76);
            if (first.equals(title)) {
                card.drawString(title, 59, 72);
            } else {
                int split = first.length() - 3;
                int space = title.lastIndexOf(' ', split);
                if (space > split / 2) {
                    split = space;
                }
                card.drawString(title.substring(0, split).trim(), 59, 65);
                card.drawString(OverlayStyle.fit(title.substring(split).trim(), card.getFontMetrics(), WIDTH - 76), 59, 89);
            }
            card.setFont(OverlayStyle.BODY_FONT);
            card.setColor(palette.getMuted());
            card.drawString(OverlayStyle.fit(entry.getDetail(), card.getFontMetrics(), WIDTH - 76), 59, 112);
            return;
        }
        for (int index = 0; index < shown; index++) {
            CompletionOverlayEntry entry = current.entries.get(index);
            int y = 46 + index * 62;
            OverlayStyle.icon(card, entry.getIcon(), 17, y + 5, 24);
            card.setFont(OverlayStyle.TITLE_FONT);
            card.setColor(palette.getText());
            card.drawString(OverlayStyle.fit(entry.getTitle(), card.getFontMetrics(), WIDTH - 76), 53, y + 17);
            card.setFont(OverlayStyle.BODY_FONT);
            card.setColor(palette.getMuted());
            card.drawString(OverlayStyle.fit(entry.getDetail(), card.getFontMetrics(), WIDTH - 76), 53, y + 36);
        }
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/20/2026
     */
    @Value
    private static class Popup {
        List<CompletionOverlayEntry> entries;
        boolean routinesOnly;
        long startedAt;
        long duration;
        OverlayTheme theme;
        BufferedImage image;
    }
}
