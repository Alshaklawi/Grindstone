package com.grinder.ui.item;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.TrackingState;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.repository.GrinderProfile;
import com.grinder.repository.RecurringGoal;
import com.grinder.repository.RepeatSchedule;
import com.grinder.ui.components.GoalComponent;
import com.grinder.ui.editor.GoalEditorPanel;
import com.grinder.ui.editor.RoutinePresetForm;
import com.grinder.ui.util.GoalCardCache;
import java.awt.Component;
import java.awt.Container;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class RoutineWorkflowTest {
    @Test
    public void recurringCustomEditorUsesOnlyThePeriodTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalEditorPanel form = new GoalEditorPanel(null, (query, callback) -> () -> {
            });
            try {
                form.loadGoal(new CustomGoal("Routine", "", GoalCategory.OTHER, 0, 5));
                form.loadRepeat(RepeatSchedule.DAILY, 100);
                assertEquals(100, form.createGoal().getProgressMaximum());
                form.loadRepeat(RepeatSchedule.NONE, 100);
                assertEquals(5, form.createGoal().getProgressMaximum());
            } catch (ParseException exception) {
                throw new AssertionError(exception);
            } finally {
                form.stopSearch();
            }
        });
    }
    @Test
    public void routinePreviewsProduceIndependentSchedulesAndSkipDuplicates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GrinderProfile profile = new GrinderProfile(42);
            profile.replace(Collections.emptyList());
            try {
                for (boolean weekly : new boolean[]{false, true}) {
                    RoutinePresetForm form = new RoutinePresetForm(weekly);
                    assertEquals(4, form.selectedGoals().size());
                    for (Goal goal : form.selectedGoals()) {
                        assertTrue(profile.add(goal, form.metadata(goal, 1000)));
                    }
                    RoutinePresetForm duplicate = new RoutinePresetForm(weekly);
                    for (Goal goal : duplicate.selectedGoals()) {
                        assertFalse(profile.add(goal, duplicate.metadata(goal, 1000)));
                    }
                }
                assertEquals(8, profile.getGoals().size());
            } catch (ParseException exception) {
                throw new AssertionError(exception);
            }
        });
    }
    @Test
    public void cacheReusesOnlyMatchingCardsAndReleasesRemovedGoals() {
        Goal goal = new CustomGoal("Routine", "", GoalCategory.OTHER, 0, 5);
        GoalCardCache<Object> cache = new GoalCardCache<>();
        Object card = new Object();
        cache.put(goal, Arrays.asList(1, "state"), card);
        assertSame(card, cache.get(goal, Arrays.asList(1, "state")));
        assertNull(cache.get(goal, Arrays.asList(2, "state")));
        cache.invalidate(goal);
        assertNull(cache.get(goal, Arrays.asList(1, "state")));
        cache.put(goal, Collections.singletonList(1), card);
        cache.retain(Collections.emptyList());
        assertNull(cache.get(goal, Collections.singletonList(1)));
    }
    @Test
    public void reliabilityDoesNotLabelUnavailableReadingsAsLiveOrCompleteStorage() {
        RecurringGoal recurring = new RecurringGoal();
        Goal xp = new SkillExperienceGoal(Skill.WOODCUTTING);
        assertEquals(TrackingState.WAITING, TrackingState.of(xp, recurring, false, false));
        assertEquals(TrackingState.LIVE, TrackingState.of(xp, recurring, true, false));
        assertEquals(TrackingState.PARTIAL_STORAGE, TrackingState.of(new PossessedItemGoal(ItemID.COINS), recurring, true, false));
        assertEquals(TrackingState.SNAPSHOT, TrackingState.of(new PossessedItemGoal(ItemID.COINS), recurring, true, true));
        recurring.configure(RepeatSchedule.DAILY, 100, 100_000);
        assertEquals(TrackingState.WAITING, TrackingState.of(xp, recurring, true, false));
        recurring.observe(xp, 100_000);
        assertEquals(TrackingState.LATE_BASELINE, TrackingState.of(xp, recurring, true, false));
    }
    @Test
    public void manualControlsUseCheckboxesAndDisableIncrementAtTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GoalComponent component = new GoalComponent();
            component.setManualActions(1, 1, () -> fail("No increment"), () -> {
            }, () -> {
            });
            assertTrue(hasControl(component, JCheckBox.class, "Completed", true));
            component.setManualActions(5, 5, () -> {
            }, () -> {
            }, () -> {
            });
            assertTrue(hasControl(component, JButton.class, "+1", false));
        });
    }
    private boolean hasControl(Container parent, Class<?> type, String label, boolean enabled) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child) && child instanceof AbstractButton && ((AbstractButton) child).getText().equals(label) && child.isEnabled() == enabled) {
                return true;
            }
            if (child instanceof Container && hasControl((Container) child, type, label, enabled)) {
                return true;
            }
        }
        return false;
    }
}
