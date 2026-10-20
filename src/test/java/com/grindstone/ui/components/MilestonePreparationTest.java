package com.grindstone.ui.components;

import com.grindstone.content.milestone.Milestone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNotNull;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public class MilestonePreparationTest {
    @Test
    public void capturesModelBeforeCreatingIndependentCardsOnEdt() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        Milestone goal = new Milestone() {
            @Override
            public String getTitle(Client client) {
                assertFalse(SwingUtilities.isEventDispatchThread());
                reads.incrementAndGet();
                return "Captured title";
            }

            @Override
            public long getProgressCurrent() {
                assertFalse(SwingUtilities.isEventDispatchThread());
                reads.incrementAndGet();
                return 5;
            }
        };
        Supplier<MilestoneComponent> prepared = goal.prepare(null, null);
        int capturedReads = reads.get();
        SwingUtilities.invokeAndWait(() -> {
            MilestoneComponent first = prepared.get();
            MilestoneComponent second = prepared.get();
            assertNotNull(first);
            assertNotSame(first, second);
            assertEquals(capturedReads, reads.get());
        });
    }
}
