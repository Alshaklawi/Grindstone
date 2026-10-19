package com.grinder.ui;

import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.GoalStatus;
import com.grinder.repository.RepeatSchedule;
import com.grinder.ui.components.GoalListPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class DashboardFiltersTest {
    @Test
    public void searchMatchesAllWordsAcrossFieldsWithoutDependingOnOrderOrCase() {
        assertTrue(GrinderPanel.matchesSearch("  SKILLING   wood ", "Woodcutting level", "Reach 90", "Skilling"));
        assertTrue(GrinderPanel.matchesSearch("90 wood", "Woodcutting level", "Reach 90", "Skilling"));
        assertFalse(GrinderPanel.matchesSearch("wood quest", "Woodcutting level", "Reach 90", "Skilling"));
        assertTrue(GrinderPanel.matchesSearch(" ", null, null, null));
        assertFalse(GrinderPanel.matchesSearch("slayer", null, null, null));
    }

    @Test
    public void schedulesDistinguishOneOffDailyWeeklyAndMonthlyGoals() {
        for (RepeatSchedule schedule : RepeatSchedule.values()) {
            assertTrue(GrinderPanel.matchesSchedule(schedule, "All schedules"));
            assertEquals(schedule == RepeatSchedule.NONE, GrinderPanel.matchesSchedule(schedule, "One-off goals"));
            assertEquals(schedule == RepeatSchedule.DAILY, GrinderPanel.matchesSchedule(schedule, "Daily"));
            assertEquals(schedule == RepeatSchedule.WEEKLY, GrinderPanel.matchesSchedule(schedule, "Weekly"));
            assertEquals(schedule == RepeatSchedule.MONTHLY, GrinderPanel.matchesSchedule(schedule, "Monthly"));
        }
        assertTrue(GrinderPanel.matchesSchedule(null, "One-off goals"));
    }

    @Test
    public void waitingReadingsAreNotMisrepresentedAsNewOrComplete() {
        assertTrue(GrinderPanel.matchesStatus(GoalStatus.NOT_STARTED, "Waiting for data", false, false));
        assertFalse(GrinderPanel.matchesStatus(GoalStatus.NOT_STARTED, "Not started", false, false));
        assertFalse(GrinderPanel.matchesStatus(GoalStatus.COMPLETE, "Complete", false, false));
        assertFalse(GrinderPanel.matchesStatus(GoalStatus.IN_PROGRESS, "Waiting for data", false, true));
        assertTrue(GrinderPanel.matchesStatus(GoalStatus.COMPLETE, "Complete", true, true));
        assertFalse(GrinderPanel.matchesStatus(GoalStatus.COMPLETE, "All statuses", true, true));
    }

    @Test
    public void standaloneControlsExposeQuickFiltersAndResetEverySelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GrinderPanel panel = new GrinderPanel(null, new ClientThread() {
                @Override
                public void invokeLater(Runnable action) {
                }
            }, new GrinderConfig() {
            });
            List<Component> controls = descendants(panel);
            assertNotNull(button(controls, "Waiting 0"));
            button(controls, "Filters").doClick();
            JComboBox<?> schedule = controls.stream().filter(component -> component instanceof JComboBox && ((JComboBox<?>) component).getItemCount() == 5 && "All schedules".equals(((JComboBox<?>) component).getItemAt(0))).map(component -> (JComboBox<?>) component).findFirst().get();
            schedule.setSelectedItem("Weekly");
            JCheckBox pinned = controls.stream().filter(component -> component instanceof JCheckBox && "Pinned goals only".equals(((JCheckBox) component).getText())).map(component -> (JCheckBox) component).findFirst().get();
            pinned.doClick();
            button(controls, "Waiting 0").doClick();
            button(controls, "Clear filters").doClick();
            assertEquals("All schedules", schedule.getSelectedItem());
            assertFalse(pinned.isSelected());
            assertFalse(button(controls, "Waiting 0").isSelected());
            panel.setSize(new Dimension(250, 800));
            layout(panel);
            assertTrue(controls.stream().filter(component -> component instanceof JScrollPane && component.isVisible()).anyMatch(component -> component.getHeight() <= 180 && component.getHeight() > 0));
            JScrollPane goals = controls.stream().filter(component -> component instanceof JScrollPane && ((JScrollPane) component).getViewport().getView() instanceof GoalListPanel).map(component -> (JScrollPane) component).findFirst().get();
            assertTrue("Expanded filters must leave room for the goal list", goals.getHeight() >= 100);
        });
    }

    private static AbstractButton button(List<Component> components, String text) {
        return components.stream().filter(component -> component instanceof AbstractButton && text.equals(((AbstractButton) component).getText())).map(component -> (AbstractButton) component).findFirst().get();
    }

    private static List<Component> descendants(Container container) {
        List<Component> result = new ArrayList<>();
        for (Component component : container.getComponents()) {
            result.add(component);
            if (component instanceof Container) {
                result.addAll(descendants((Container) component));
            }
        }
        return result;
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                layout((Container) component);
            }
        }
    }
}
