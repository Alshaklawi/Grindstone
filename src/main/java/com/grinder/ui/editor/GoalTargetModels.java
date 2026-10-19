package com.grinder.ui.editor;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.experience.BaseExperienceGoal;
import com.grinder.content.goal.impl.experience.SessionExperienceGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.content.goal.impl.level.BaseLevelGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.ui.util.UiTooltips;
import java.util.Locale;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.runelite.api.Skill;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class GoalTargetModels {
    private GoalTargetModels() {
    }
    public static SpinnerNumberModel create(Goal goal) {
        if (goal instanceof SessionExperienceGoal) {
            long cap = ((SessionExperienceGoal) goal).isTotal() ? Skill.values().length * 200_000_000L : 200_000_000L;
            return new SpinnerNumberModel(Long.valueOf(goal.getProgressMaximum()), Long.valueOf(1), Long.valueOf(cap), Long.valueOf(1));
        }
        long minimum = goal instanceof CombatLevelGoal ? 3 : 1;
        long maximum = goal instanceof SkillLevelGoal || goal instanceof BaseLevelGoal ? 99 : goal instanceof CombatLevelGoal ? 126 : goal instanceof TotalLevelGoal ? Skill.values().length * 99L : goal instanceof SkillExperienceGoal || goal instanceof BaseExperienceGoal ? 200_000_000L : goal instanceof TotalExperienceGoal ? Skill.values().length * 200_000_000L : goal instanceof PossessedItemGoal ? PossessedItemGoal.MAX_TARGET : Integer.MAX_VALUE;
        return new SpinnerNumberModel(Long.valueOf(goal.getProgressMaximum()), Long.valueOf(minimum), Long.valueOf(maximum), Long.valueOf(1));
    }

    public static void describe(JSpinner spinner) {
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        UiTooltips.set(spinner, String.format(Locale.ROOT, "Target range: %,d to %,d. Type a number or use the arrows. This changes your goal, not your game stats.", model.getMinimum(), model.getMaximum()));
    }
}
