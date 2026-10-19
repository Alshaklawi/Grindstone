package com.grinder.ui;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.repository.RepeatSchedule;
import com.grinder.ui.editor.BulkGoalForm;
import com.grinder.ui.editor.GoalEditorPanel;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class BulkAndPreviewUiTest {
    @Test
    public void linkedEditorDoesNotInventTargetsForUnloadedSources() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String id = java.util.UUID.randomUUID().toString();
            com.grinder.ui.editor.LinkedGoalForm.Choice source = new com.grinder.ui.editor.LinkedGoalForm.Choice(id, "Native tracker", 0, false, false);
            com.grinder.ui.editor.LinkedGoalForm form = new com.grinder.ui.editor.LinkedGoalForm(Collections.singletonList(source), null);
            JTable table = findTable(form);
            table.getModel().setValueAt(true, 0, 0);
            assertEquals(Boolean.TRUE, ((javax.swing.JLabel) table.getDefaultRenderer(String.class)).getClientProperty("html.disable"));
            try {
                form.read();
                fail("Unloaded native target should not default silently to one");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("not loaded"));
            } catch (java.text.ParseException failure) {
                throw new AssertionError(failure);
            }
        });
    }
    @Test
    public void newTypeFiltersDistinguishManualLinkedAndDeadlineGoals() {
        Goal checklist = new com.grinder.content.goal.impl.ChecklistGoal("Routine", "", Arrays.asList("One", "Two"));
        assertTrue(GrinderPanel.matchesType(checklist, com.grinder.content.goal.GoalStatus.NOT_STARTED, "Custom", 0, 100));
        assertFalse(GrinderPanel.matchesType(checklist, com.grinder.content.goal.GoalStatus.NOT_STARTED, "Automatic", 0, 100));
        Goal base = new com.grinder.content.goal.impl.level.BaseLevelGoal();
        assertTrue(GrinderPanel.matchesType(base, com.grinder.content.goal.GoalStatus.NOT_STARTED, "Base stats", 0, 100));
        assertTrue(GrinderPanel.matchesType(base, com.grinder.content.goal.GoalStatus.NOT_STARTED, "With deadline", 100, 100));
        assertTrue(GrinderPanel.matchesType(base, com.grinder.content.goal.GoalStatus.NOT_STARTED, "Past due", 100, 100));
        assertFalse(GrinderPanel.matchesType(base, com.grinder.content.goal.GoalStatus.COMPLETE, "Past due", 100, 100));
        assertFalse(GrinderPanel.matchesType(base, com.grinder.content.goal.GoalStatus.NOT_STARTED, "Past due", 101, 100));
    }
    @Test
    public void bulkSearchKeepsHiddenSelectionsAndClearReallyClearsAll() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Goal first = new CustomGoal("Alpha", "", GoalCategory.OTHER, 0, 1);
            Goal second = new CustomGoal("Beta", "", GoalCategory.OTHER, 0, 1);
            BulkGoalForm form = new BulkGoalForm(Arrays.asList(new BulkGoalForm.Row(first, "Alpha", GoalCategory.OTHER, RepeatSchedule.NONE, false), new BulkGoalForm.Row(second, "Beta", GoalCategory.OTHER, RepeatSchedule.NONE, true)));
            JTextField search = findSearch(form);
            JTable table = findTable(form);
            search.setText("Alpha");
            assertEquals(1, table.getRowCount());
            button(form, "Select visible").doClick();
            assertEquals(Collections.singletonList(first), form.selectedGoals());
            search.setText("Beta");
            button(form, "Select visible").doClick();
            assertEquals(Arrays.asList(first, second), form.selectedGoals());
            button(form, "Clear all selections").doClick();
            assertTrue(form.selectedGoals().isEmpty());
        });
    }

    @Test
    public void goalPreviewDistinguishesDuplicatesAndExplainsAvailability() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalEditorPanel form = new GoalEditorPanel(null, (query, callback) -> () -> {
            });
            try {
                SkillLevelGoal goal = new SkillLevelGoal(Skill.ATTACK);
                form.setExistingGoals(Collections.singletonMap(goal, RepeatSchedule.NONE));
                assertTrue(form.isReviewEnabled());
                String preview = form.reviewText(goal);
                assertTrue(preview.contains("matching tracker and schedule"));
                assertTrue(preview.contains("Unavailable readings are not confirmed zero"));
                assertTrue(preview.contains("Target: 99"));
                assertTrue(form.reviewText(new SkillLevelGoal(Skill.DEFENCE)).contains("adds a new goal"));
            } finally {
                form.stopSearch();
            }
        });
    }

    private static JTextField findSearch(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JTextField) {
                return (JTextField) component;
            }
            if (component instanceof Container) {
                JTextField found = findSearch((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JTable findTable(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JTable) {
                return (JTable) component;
            }
            if (component instanceof Container) {
                JTable found = findTable((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton button(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton && ((JButton) component).getText().equals(text)) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton found = button((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
