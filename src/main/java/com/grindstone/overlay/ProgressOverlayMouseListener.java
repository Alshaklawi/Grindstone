package com.grindstone.overlay;

import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.input.MouseAdapter;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/11/2026
 */
@Singleton
public final class ProgressOverlayMouseListener extends MouseAdapter {
    private final GrindstoneProgressOverlay overlay;
    private volatile boolean active;
    private int blockedButtons;
    private int blockedClicks;

    @Inject
    public ProgressOverlayMouseListener(GrindstoneProgressOverlay overlay) {
        this.overlay = overlay;
    }

    public void setActive(boolean active) {
        this.active = active;
        blockedButtons = 0;
        blockedClicks = 0;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent event) {
        int button = 1 << event.getButton();
        blockedClicks &= ~button;
        if (active && !event.isAltDown() && overlay.isMouseOver(event.getPoint())) {
            blockedButtons |= button;
            blockedClicks |= button;
            event.consume();
        }
        return event;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent event) {
        int button = 1 << event.getButton();
        if (active && (blockedButtons & button) != 0) {
            event.consume();
        }
        blockedButtons &= ~button;
        return event;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent event) {
        int button = 1 << event.getButton();
        if (active && ((blockedClicks & button) != 0 || !event.isAltDown() && overlay.isMouseOver(event.getPoint()))) {
            event.consume();
        }
        blockedClicks &= ~button;
        return event;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent event) {
        if (active && blockedButtons != 0) {
            event.consume();
        }
        return event;
    }
}
