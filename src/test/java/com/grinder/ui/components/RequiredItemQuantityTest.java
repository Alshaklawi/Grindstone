package com.grinder.ui.components;

import com.grinder.content.goal.impl.item.PossessedItemGoal;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class RequiredItemQuantityTest {
    @Test
    public void largeRequiredQuantityRemainsVisibleWhenDetailsAreCollapsed() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalComponent component = new PossessedItemGoal(ItemID.COINS, 1_500_000_000L).build(null);
            JLabel quantity = label(component, "Required: 1,500,000,000");
            assertNotNull(quantity);
            component.setExpanded(false);
            JLabel progress = label(component, "0 / 1,500,000,000 owned (0.0%)");
            assertNotNull(progress);
            for (Component current = progress; current != null; current = current.getParent()) {
                assertTrue(current.isVisible());
            }
            component.setProgressBarVisible(false);
            for (Component current = quantity; current != null; current = current.getParent()) {
                assertTrue(current.isVisible());
            }
            assertTrue(quantity.getToolTipText().contains("not the amount currently owned"));
        });
    }

    @Test
    public void rebuildingAfterEditingShowsTheNewTargetIncludingOneItem() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PossessedItemGoal goal = new PossessedItemGoal(ItemID.DRAGON_PICKAXE, 1);
            assertNotNull(label(goal.build(null), "Required: 1"));
            goal.setTarget(3_000_000_000L);
            assertNotNull(label(goal.build(null), "Required: 3,000,000,000"));
        });
    }

    private JLabel label(Container container, String text) {
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel && text.equals(((JLabel) child).getText())) {
                return (JLabel) child;
            }
            if (child instanceof Container) {
                JLabel match = label((Container) child, text);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
