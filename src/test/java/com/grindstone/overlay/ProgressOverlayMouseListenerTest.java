package com.grindstone.overlay;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.Icon;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/11/2026
 */
public class ProgressOverlayMouseListenerTest {
    private final Canvas canvas = new Canvas();
    private final GrindstoneProgressOverlay overlay = new GrindstoneProgressOverlay(new GrindstoneConfig() {
        @Override
        public boolean progressOverlayEnabled() {
            return true;
        }
    });
    private final ProgressOverlayMouseListener listener = new ProgressOverlayMouseListener(overlay);

    private void showOverlay() {
        overlay.update(List.of(new ProgressOverlayEntry("Amoxliatl", RepeatSchedule.DAILY, 0, 100, false, true, true, Icon.BOSSING.getImage())), 720);
        Graphics2D graphics = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB).createGraphics();
        Dimension size;
        try {
            size = overlay.render(graphics);
        } finally {
            graphics.dispose();
        }
        overlay.setBounds(new Rectangle(50, 50, size.width, size.height));
        overlay.endFrame();
        listener.setActive(true);
    }

    private MouseEvent event(int type, int x, int y, int button, boolean alt) {
        return new MouseEvent(canvas, type, 1, alt ? InputEvent.ALT_DOWN_MASK : 0, x, y, 1, false, button);
    }

    @Test
    public void blocksClicksAndCapturedGesturesForEachMouseButton() {
        showOverlay();
        for (int button : new int[]{MouseEvent.BUTTON1, MouseEvent.BUTTON2, MouseEvent.BUTTON3}) {
            assertTrue(listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 60, 60, button, false)).isConsumed());
            assertTrue(listener.mouseDragged(event(MouseEvent.MOUSE_DRAGGED, 400, 400, MouseEvent.NOBUTTON, false)).isConsumed());
            assertTrue(listener.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 400, 400, button, false)).isConsumed());
            assertTrue(listener.mouseClicked(event(MouseEvent.MOUSE_CLICKED, 400, 400, button, false)).isConsumed());
            assertFalse(listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 400, 400, button, false)).isConsumed());
            assertFalse(listener.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 400, 400, button, false)).isConsumed());
        }
    }

    @Test
    public void allowsRuneLiteAltDragging() {
        showOverlay();
        assertFalse(listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 60, 60, MouseEvent.BUTTON1, true)).isConsumed());
        assertFalse(listener.mouseDragged(event(MouseEvent.MOUSE_DRAGGED, 80, 80, MouseEvent.NOBUTTON, true)).isConsumed());
        assertFalse(listener.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 80, 80, MouseEvent.BUTTON1, true)).isConsumed());
        assertFalse(listener.mouseClicked(event(MouseEvent.MOUSE_CLICKED, 80, 80, MouseEvent.BUTTON1, true)).isConsumed());
    }

    @Test
    public void ignoresHiddenEmptyAndClearedOverlays() {
        showOverlay();
        overlay.endFrame();
        assertFalse(listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 60, 60, MouseEvent.BUTTON1, false)).isConsumed());
        showOverlay();
        overlay.update(List.of(), 720);
        assertFalse(listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 60, 60, MouseEvent.BUTTON1, false)).isConsumed());
        showOverlay();
        overlay.clear();
        assertFalse(listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 60, 60, MouseEvent.BUTTON1, false)).isConsumed());
    }

    @Test
    public void stoppingReleasesCapturedInputAndRestartingStartsFresh() {
        showOverlay();
        assertTrue(listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, 60, 60, MouseEvent.BUTTON1, false)).isConsumed());
        listener.setActive(false);
        assertFalse(listener.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 60, 60, MouseEvent.BUTTON1, false)).isConsumed());
        assertFalse(listener.mouseClicked(event(MouseEvent.MOUSE_CLICKED, 60, 60, MouseEvent.BUTTON1, false)).isConsumed());
        listener.setActive(true);
        assertFalse(listener.mouseDragged(event(MouseEvent.MOUSE_DRAGGED, 400, 400, MouseEvent.NOBUTTON, false)).isConsumed());
    }
}
