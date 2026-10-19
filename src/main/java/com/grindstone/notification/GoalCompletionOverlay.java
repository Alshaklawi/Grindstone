package com.grindstone.notification;

import com.grindstone.config.GrindstoneConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
@Singleton
public final class GoalCompletionOverlay extends OverlayPanel {
    private final GrindstoneConfig config;
    private volatile List<String> titles = Collections.emptyList();
    private volatile long expiresAt;

    @Inject
    public GoalCompletionOverlay(GrindstoneConfig config) {
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
        panelComponent.setPreferredSize(new Dimension(280, 0));
        panelComponent.setGap(new Point(0, 5));
        panelComponent.setBackgroundColor(new Color(24, 28, 24, 235));
    }

    public void show(List<String> titles) {
        this.titles = List.copyOf(titles);
        expiresAt = System.nanoTime() + Math.max(3, Math.min(15, config.completionPopupSeconds())) * 1_000_000_000L;
    }

    public void clear() {
        titles = Collections.emptyList();
        expiresAt = 0;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<String> current = titles;
        if (!config.enabled() || !config.completionPopup() || current.isEmpty() || System.nanoTime() >= expiresAt) {
            return null;
        }
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder().text(current.size() == 1 ? "Goal complete!" : current.size() + " goals complete!").color(new Color(92, 220, 112)).build());
        for (String title : current.subList(0, Math.min(3, current.size()))) {
            panelComponent.getChildren().add(LineComponent.builder().left(title).leftColor(Color.WHITE).build());
        }
        if (current.size() > 3) {
            panelComponent.getChildren().add(LineComponent.builder().left("And " + (current.size() - 3) + " more").leftColor(Color.LIGHT_GRAY).build());
        }
        return super.render(graphics);
    }
}
