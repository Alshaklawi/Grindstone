package com.grindstone.notification;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.overlay.OverlayStyle;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
@Singleton
public final class MilestoneCompletionOverlay extends Overlay {
    private static final int WIDTH = 320;
    private final GrindstoneConfig config;
    private volatile Popup popup;

    @Inject
    public MilestoneCompletionOverlay(GrindstoneConfig config) {
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    public void show(List<String> titles) {
        show(titles, false);
    }

    public synchronized void show(List<String> titles, boolean routinesOnly) {
        if (titles.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        Popup previous = popup;
        Set<String> combined = new LinkedHashSet<>();
        if (previous != null && now - previous.startedAt < previous.duration) {
            combined.addAll(previous.titles);
            routinesOnly &= previous.routinesOnly;
        }
        for (String title : titles) {
            if (combined.size() >= 50) {
                break;
            }
            combined.add(Text.removeTags(title).replaceAll("[\\r\\n\\t]", " ").trim());
        }
        popup = new Popup(List.copyOf(combined), routinesOnly, now, Math.max(3, Math.min(15, config.completionPopupSeconds())) * 1_000_000_000L);
    }

    public synchronized void clear() {
        popup = null;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Popup current = popup;
        if (!config.enabled() || !config.completionPopup() || current == null) {
            return null;
        }
        long age = System.nanoTime() - current.startedAt;
        if (age >= current.duration) {
            return null;
        }
        int shown = Math.min(3, current.titles.size());
        int height = 58 + shown * 22 + (current.titles.size() > shown ? 20 : 0);
        float alpha = (float) Math.max(0, Math.min(1, Math.min(age / 160_000_000.0, (current.duration - age) / 450_000_000.0)));
        Graphics2D card = (Graphics2D) graphics.create();
        try {
            card.setComposite(AlphaComposite.SrcOver.derive(alpha));
            OverlayStyle.frame(card, WIDTH, height, OverlayStyle.GREEN);
            card.setColor(new Color(98, 205, 174, 25));
            card.fillRoundRect(12, 12, 28, 28, 8, 8);
            OverlayStyle.check(card, 21, 22, OverlayStyle.GREEN);
            card.setFont(OverlayStyle.SMALL_FONT);
            card.setColor(OverlayStyle.MUTED);
            card.drawString("GRINDSTONE", 50, 22);
            card.setFont(OverlayStyle.HEADING_FONT);
            card.setColor(OverlayStyle.GREEN);
            String heading = current.titles.size() == 1 ? current.routinesOnly ? "Routine complete" : "Milestone reached" : current.titles.size() + (current.routinesOnly ? " routines complete" : " milestones reached");
            card.drawString(heading, 50, 37);
            card.setFont(OverlayStyle.TITLE_FONT);
            card.setColor(OverlayStyle.TEXT);
            for (int index = 0; index < shown; index++) {
                card.drawString(OverlayStyle.fit(current.titles.get(index), card.getFontMetrics(), WIDTH - 24), 12, 68 + index * 22);
            }
            card.setFont(OverlayStyle.SMALL_FONT);
            if (current.titles.size() > shown) {
                card.setColor(OverlayStyle.MUTED);
                card.drawString("And " + (current.titles.size() - shown) + " more in the sidebar", 12, 68 + shown * 22);
            }
        } finally {
            card.dispose();
        }
        return new Dimension(WIDTH, height);
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/20/2026
     */
    @Value
    private static class Popup {
        List<String> titles;
        boolean routinesOnly;
        long startedAt;
        long duration;
    }
}
