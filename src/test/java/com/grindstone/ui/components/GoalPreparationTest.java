package com.grindstone.ui.components;

import com.grindstone.content.goal.Goal;
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
public class GoalPreparationTest {
    @Test
    public void capturesModelBeforeCreatingIndependentCardsOnEdt() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        Goal goal = new Goal() {
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
        Supplier<GoalComponent> prepared = goal.prepare(null, null);
        int capturedReads = reads.get();
        SwingUtilities.invokeAndWait(() -> {
            GoalComponent first = prepared.get();
            GoalComponent second = prepared.get();
            assertNotNull(first);
            assertNotSame(first, second);
            assertEquals(capturedReads, reads.get());
        });
    }
}
