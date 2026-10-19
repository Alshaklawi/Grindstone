package com.grinder.notification;

import com.grinder.config.GrinderConfig;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class GoalCompletionOverlayTest {
    @Test
    public void popupRendersGroupedCompletionsAndClearsWithoutGameInput() {
        GoalCompletionOverlay overlay = new GoalCompletionOverlay(new GrinderConfig() {
        });
        Graphics2D graphics = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            assertNull(overlay.render(graphics));
            overlay.show(Arrays.asList("Slayer points", "Normal streak", "Wilderness streak", "Checklist"));
            assertNotNull(overlay.render(graphics));
            overlay.clear();
            assertNull(overlay.render(graphics));
        } finally {
            graphics.dispose();
        }
    }

    @Test
    public void popupToggleHidesTheOverlayIndependentlyOfChat() {
        GrinderConfig config = new GrinderConfig() {
            @Override
            public boolean completionPopup() {
                return false;
            }
        };
        GoalCompletionOverlay overlay = new GoalCompletionOverlay(config);
        overlay.show(Collections.singletonList("Checklist"));
        Graphics2D graphics = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            assertNull(overlay.render(graphics));
            assertTrue(config.completionChat());
            assertFalse(config.completionNotification().isEnabled());
        } finally {
            graphics.dispose();
        }
    }
}
