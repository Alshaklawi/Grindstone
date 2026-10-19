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
        assertNotEquals(SegmentedProgressBar.colorFor(GoalStatus.IN_PROGRESS), SegmentedProgressBar.colorFor(GoalStatus.COMPLETE));
    }

    @Test
    public void paintsSeparateChunksWithAnUnfilledTrack() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SegmentedProgressBar bar = new SegmentedProgressBar();
            bar.setSize(120, 9);
            bar.setProgress(50, 100);
            bar.setStatus(GoalStatus.IN_PROGRESS);
            BufferedImage image = new BufferedImage(120, 9, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                bar.paint(graphics);
            } finally {
                graphics.dispose();
            }
            assertEquals(bar.getForeground().getRGB(), image.getRGB(55, 4));
            assertEquals(bar.getBackground().getRGB(), image.getRGB(67, 4));
            assertEquals(0, image.getRGB(5, 4));
        });
    }
    @Test
    public void chunksRepresentWholeTargetUnitsAndNeverExceedTwenty() {
        SegmentedProgressBar bar = new SegmentedProgressBar();
        for (long target : new long[]{1, 5, 19, 20, 21, 23, 100, 1_500_000_000L, Long.MAX_VALUE}) {
            bar.setProgress(0, target);
            assertTrue(bar.getChunkCount() >= 1 && bar.getChunkCount() <= 20);
            assertTrue(bar.getChunkSize() >= 1);
        }
        bar.setProgress(0, 100);
        assertEquals(20, bar.getChunkCount());
        assertEquals(5, bar.getChunkSize());
        bar.setProgress(0, 7);
        assertEquals(7, bar.getChunkCount());
        assertEquals(1, bar.getChunkSize());
    }
}
