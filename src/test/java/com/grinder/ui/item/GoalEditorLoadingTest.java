package com.grinder.ui.item;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.repository.GrinderProfile;
import com.grinder.ui.editor.CompletionGoalChoices;
import com.grinder.ui.editor.GoalEditorPanel;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalEditorLoadingTest {
    @Test public void everyDefaultTrackerCanBeLoadedWithoutChangingItsSubjectOrTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Map<Integer, Map<String, Supplier<Goal>>> choices = CompletionGoalChoices.create(null);
            choices.get(13).put("Barrows", () -> new CombatAchievementBossGoal(1, "Barrows"));
            choices.get(17).put("Combat task 0", () -> new com.grinder.content.goal.impl.combatachievement.CombatAchievementTaskGoal(0, "Combat task 0", ""));
            ItemLookup lookup = (query, callback) -> {
                int id = Integer.parseInt(query);
                callback.accept(new ItemSearchService.Response(Collections.singletonList(new ItemSearchService.Result(id, "Item " + id, null)), "Found", true));
                return () -> {
                };
            };
            GoalEditorPanel form = new GoalEditorPanel(null, lookup, choices);
            try {
                GrinderProfile profile = new GrinderProfile(42);
                for (Goal original : profile.getGoals()) {
                    if (original instanceof com.grinder.content.goal.impl.LinkedGoal) {
                        java.util.List<com.grinder.ui.editor.LinkedGoalForm.Choice> sources = new java.util.ArrayList<>();
                        for (Goal source : profile.getGoals()) {
                            if (!(source instanceof com.grinder.content.goal.impl.LinkedGoal)) {
                                sources.add(new com.grinder.ui.editor.LinkedGoalForm.Choice(profile.metadata(source).getId(), source.getTitle(null), source.getProgressMaximum(), source instanceof com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal, false));
                            }
                        }
                        com.grinder.content.goal.impl.LinkedGoal edited = new com.grinder.ui.editor.LinkedGoalForm(sources, (com.grinder.content.goal.impl.LinkedGoal) original).read();
                        assertTrue(GrinderProfile.sameSelection(original, edited));
                        continue;
                    }
                    form.loadGoal(original);
                    assertTrue(form.isReady());
                    Goal edited = form.createGoal();
                    assertEquals(original.getClass(), edited.getClass());
                    if (!(original instanceof CustomGoal)) {
                        assertTrue(GrinderProfile.sameSelection(original, edited));
                    }
                    if (original.supportsTarget()) {
                        assertEquals(original.getProgressMaximum(), edited.getProgressMaximum());
                    }
                }
            } catch (java.text.ParseException exception) {
                throw new AssertionError(exception);
            } finally {
                form.stopSearch();
            }
        });
    }
}
