package com.grinder.ui.item;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.LinkedGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTaskGoal;
import com.grinder.repository.GrinderProfile;
import com.grinder.ui.editor.CompletionGoalChoices;
import com.grinder.ui.editor.GoalEditorPanel;
import com.grinder.ui.editor.LinkedGoalForm;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            choices.get(17).put("Combat task 0", () -> new CombatAchievementTaskGoal(0, "Combat task 0", ""));
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
                    if (original instanceof LinkedGoal) {
                        List<LinkedGoalForm.Choice> sources = new ArrayList<>();
                        for (Goal source : profile.getGoals()) {
                            if (!(source instanceof LinkedGoal)) {
                                sources.add(new LinkedGoalForm.Choice(profile.metadata(source).getId(), source.getTitle(null), source.getProgressMaximum(), source instanceof CollectionLogItemGoal, profile.metadata(source).recurring().isActive()));
                            }
                        }
                        LinkedGoal edited = new LinkedGoalForm(sources, (LinkedGoal) original).read();
                        assertTrue(((LinkedGoal) original).getMode().toString(), GrinderProfile.sameSelection(original, edited));
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
            } catch (ParseException exception) {
                throw new AssertionError(exception);
            } finally {
                form.stopSearch();
            }
        });
    }
}
