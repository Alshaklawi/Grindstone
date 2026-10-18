package com.grinder.ui.components;

import com.grinder.content.goal.GoalStatus;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class SegmentedProgressBarTest {
    @Test
    public void activeAndCompleteHaveDistinctRuneLiteColors() {
        assertEquals(ColorScheme.PROGRESS_INPROGRESS_COLOR, SegmentedProgressBar.colorFor(GoalStatus.IN_PROGRESS));
        assertEquals(ColorScheme.PROGRESS_COMPLETE_COLOR, SegmentedProgressBar.colorFor(GoalStatus.COMPLETE));
        assertNotEquals(SegmentedProgressBar.colorFor(GoalStatus.IN_PROGRESS),
                SegmentedProgressBar.colorFor(GoalStatus.COMPLETE));
    }

    @Test
    public void paintsSeparateChunksWithAnUnfilledTrack() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SegmentedProgressBar bar = new SegmentedProgressBar();
            bar.setSize(120, 9);
            bar.setValue(5000);
            bar.setStatus(GoalStatus.IN_PROGRESS);
            BufferedImage image = new BufferedImage(120, 9, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try { bar.paint(graphics); } finally { graphics.dispose(); }
            assertEquals(bar.getForeground().getRGB(), image.getRGB(55, 4));
            assertEquals(bar.getBackground().getRGB(), image.getRGB(65, 4));
            assertEquals(0, image.getRGB(8, 4));
        });
    }
}
