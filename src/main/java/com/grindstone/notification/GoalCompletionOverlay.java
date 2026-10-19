package com.grindstone.notification;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.overlay.OverlayStyle;
import com.grindstone.ui.Icon;
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
import net.runelite.client.ui.FontManager;
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
public final class GoalCompletionOverlay extends Overlay {
    private static final int WIDTH = 300;
    private final GrindstoneConfig config;
    private volatile Popup popup;

    @Inject
    public GoalCompletionOverlay(GrindstoneConfig config) {
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
        int height = 100 + shown * 20 + (current.titles.size() > shown ? 18 : 0);
        float alpha = (float) Math.max(0, Math.min(1, Math.min(age / 160_000_000.0, (current.duration - age) / 450_000_000.0)));
        Graphics2D card = (Graphics2D) graphics.create();
        try {
            card.setComposite(AlphaComposite.SrcOver.derive(alpha));
            OverlayStyle.frame(card, WIDTH, height, OverlayStyle.GREEN);
            card.drawImage(Icon.MAIN.getImage(), 15, 16, 22, 22, null);
            card.setFont(FontManager.getRunescapeSmallFont());
            card.setColor(OverlayStyle.MUTED);
            card.drawString("GRINDSTONE", 46, 23);
            card.setFont(FontManager.getRunescapeBoldFont());
            card.setColor(OverlayStyle.GOLD);
            String heading = current.titles.size() == 1 ? current.routinesOnly ? "Routine complete!" : "Milestone reached!" : current.titles.size() + " targets reached!";
            card.drawString(heading, 46, 40);
            card.setColor(new Color(37, 69, 49));
            card.fillOval(WIDTH - 40, 16, 23, 23);
            OverlayStyle.check(card, WIDTH - 33, 23, OverlayStyle.GREEN);
            card.setColor(new Color(80, 83, 88, 100));
            card.drawLine(14, 49, WIDTH - 18, 49);
            card.setFont(FontManager.getRunescapeBoldFont());
            card.setColor(OverlayStyle.TEXT);
            for (int index = 0; index < shown; index++) {
                card.drawString(OverlayStyle.fit(current.titles.get(index), card.getFontMetrics(), WIDTH - 32), 15, 68 + index * 20);
            }
            card.setFont(FontManager.getRunescapeSmallFont());
            if (current.titles.size() > shown) {
                card.setColor(OverlayStyle.MUTED);
                card.drawString("And " + (current.titles.size() - shown) + " more", 15, 68 + shown * 20);
            }
            card.setColor(OverlayStyle.GREEN);
            card.drawString("Complete", 15, height - 29);
            card.drawString("100%", WIDTH - 18 - card.getFontMetrics().stringWidth("100%"), height - 29);
            OverlayStyle.progress(card, 15, height - 19, WIDTH - 34, 1, OverlayStyle.GREEN);
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
