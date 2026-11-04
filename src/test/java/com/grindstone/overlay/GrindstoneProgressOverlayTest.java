package com.grindstone.overlay;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.config.ProgressOverlayView;
import com.grindstone.repository.RepeatSchedule;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public class GrindstoneProgressOverlayTest {
    @Test
    public void groupedAndCompactViewsFitSmallAndLargeCanvases() {
        List<ProgressOverlayEntry> entries = new ArrayList<>();
        for (int index = 0; index < 40; index++) {
            entries.add(new ProgressOverlayEntry("A long milestone title that exceeds narrow overlay widths " + index, RepeatSchedule.values()[index % RepeatSchedule.values().length], index, 100, false, true, true));
        }
        Graphics2D graphics = new BufferedImage(400, 1100, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            for (ProgressOverlayView view : ProgressOverlayView.values()) {
                for (boolean bars : new boolean[]{true, false}) {
                    for (int width : new int[]{180, 260, 360}) {
                        GrindstoneProgressOverlay overlay = new GrindstoneProgressOverlay(createConfig(view, bars, width));
                        for (int canvasHeight : new int[]{180, 334, 503, 1000}) {
                            overlay.update(entries, canvasHeight);
                            Dimension size = overlay.render(graphics);
                            assertNotNull(view + " should retain at least one entry", size);
                            assertEquals(width, size.width);
                            assertTrue(view + " extends beyond the canvas", size.height <= canvasHeight);
                        }
                    }
                }
            }
        } finally {
            graphics.dispose();
        }
    }

    @Test
    public void clearedAndCompletedOneTimeMilestonesDoNotLeaveAnEmptyPanel() {
        GrindstoneProgressOverlay overlay = new GrindstoneProgressOverlay(createConfig(ProgressOverlayView.ALL, true, 260));
        Graphics2D graphics = new BufferedImage(400, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            overlay.update(List.of(new ProgressOverlayEntry("Finished", RepeatSchedule.NONE, 10, 10, true, true, true)), 600);
            assertNull(overlay.render(graphics));
            overlay.update(List.of(new ProgressOverlayEntry("Active", RepeatSchedule.DAILY, 1, 10, false, true, true)), 600);
            assertNotNull(overlay.render(graphics));
            overlay.clear();
            assertNull(overlay.render(graphics));
        } finally {
            graphics.dispose();
        }
    }

    private GrindstoneConfig createConfig(ProgressOverlayView view, boolean bars, int width) {
        return new GrindstoneConfig() {
            @Override
            public boolean progressOverlayEnabled() {
                return true;
            }

            @Override
            public ProgressOverlayView progressOverlayView() {
                return view;
            }

            @Override
            public boolean progressOverlayBars() {
                return bars;
            }

            @Override
            public int progressOverlayWidth() {
                return width;
            }

            @Override
            public int progressOverlayRows() {
                return 20;
            }
        };
    }
}
