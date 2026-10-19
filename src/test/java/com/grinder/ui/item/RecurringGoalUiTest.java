package com.grinder.ui.item;

import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.repository.GoalMetadata;
import com.grinder.repository.RepeatSchedule;
import com.grinder.ui.components.GoalHistoryPanel;
import com.grinder.ui.editor.GoalEditorPanel;
import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.function.Supplier;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class RecurringGoalUiTest {
    @Test public void editorKeepsRepeatSettingsForSupportedTypesOnly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalEditorPanel form = new GoalEditorPanel(null, (query, callback) -> () -> {
            });
            try {
                form.loadGoal(new SkillExperienceGoal(Skill.WOODCUTTING));
                form.loadRepeat(RepeatSchedule.DAILY, 100_000);
                assertEquals(RepeatSchedule.DAILY, form.getRepeatSchedule());
                assertEquals(100_000, form.getRepeatTarget());
                form.loadGoal(new CombatLevelGoal());
                assertEquals(RepeatSchedule.NONE, form.getRepeatSchedule());
            } finally {
                form.stopSearch();
            }
        });
    }
    @Test public void historyUsesReadOnlySnapshotAndSeparatesPeriodsFromLifetimeMilestones() throws Exception {
        CustomGoal goal = new CustomGoal("Routine", "", GoalCategory.OTHER, 0, 5);
        GoalMetadata metadata = new GoalMetadata();
        long now = Instant.parse("2026-10-19T10:00:00Z").toEpochMilli();
        metadata.recurring().configure(RepeatSchedule.DAILY, 5, now);
        metadata.recurring().observe(goal, now);
        goal.update("Routine", "", GoalCategory.OTHER, 5, 5);
        metadata.recurring().observe(goal, now + 1000);
        metadata.observe(goal, "Routine", now + 1000);
        metadata.recurring().observe(goal, now + 86_400_000);
        Supplier<GoalHistoryPanel> snapshot = GoalHistoryPanel.snapshot(metadata);
        metadata.recurring().configure(RepeatSchedule.NONE, 1, now + 86_400_001);
        SwingUtilities.invokeAndWait(() -> {
            GoalHistoryPanel panel = snapshot.get();
            java.util.List<JTable> tables = new java.util.ArrayList<>();
            findTables(panel, tables);
            assertEquals(2, tables.size());
            JTable periods = tables.get(0);
            assertEquals(2, periods.getRowCount());
            assertEquals("Current period", periods.getValueAt(0, 5));
            assertEquals("Completed", periods.getValueAt(1, 5));
            assertEquals(5L, periods.getValueAt(1, 3));
            assertEquals(5L, periods.getValueAt(1, 4));
            assertEquals(Long.class, periods.getColumnClass(3));
            assertTrue(periods.getValueAt(1, 8).toString().contains("UTC"));
            assertFalse(periods.isCellEditable(0, 0));
            assertEquals(1, tables.get(1).getRowCount());
        });
    }
    private void findTables(Container parent, java.util.List<JTable> tables) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTable) {
                tables.add((JTable) child);
            } else if (child instanceof Container) {
                findTables((Container) child, tables);
            }
        }
    }
}
