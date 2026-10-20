package com.grindstone.ui.editor;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTotalMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.BaseLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import com.grindstone.ui.util.UiTooltips;
import java.util.Locale;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.runelite.api.Skill;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class MilestoneTargetModels {
    private MilestoneTargetModels() {
    }
    public static SpinnerNumberModel create(Milestone goal) {
        if (goal instanceof SessionExperienceMilestone) {
            long cap = ((SessionExperienceMilestone) goal).isTotal() ? Skill.values().length * 200_000_000L : 200_000_000L;
            return new SpinnerNumberModel(Long.valueOf(goal.getProgressMaximum()), Long.valueOf(1), Long.valueOf(cap), Long.valueOf(1));
        }
        if (goal instanceof CollectionLogTotalMilestone) {
            int knownMaximum = ((CollectionLogTotalMilestone) goal).getCollectionMaximum();
            long cap = knownMaximum > 0 ? knownMaximum : Integer.MAX_VALUE;
            long value = Math.min(goal.getProgressMaximum(), cap);
            return new SpinnerNumberModel(Long.valueOf(value), Long.valueOf(1), Long.valueOf(cap), Long.valueOf(1));
        }
        long minimum = goal instanceof CombatLevelMilestone ? 3 : 1;
        long maximum = goal instanceof SkillLevelMilestone || goal instanceof BaseLevelMilestone ? 99 : goal instanceof CombatLevelMilestone ? 126 : goal instanceof TotalLevelMilestone ? Skill.values().length * 99L : goal instanceof SkillExperienceMilestone || goal instanceof BaseExperienceMilestone ? 200_000_000L : goal instanceof TotalExperienceMilestone ? Skill.values().length * 200_000_000L : goal instanceof PossessedItemMilestone ? PossessedItemMilestone.MAX_TARGET : Integer.MAX_VALUE;
        return new SpinnerNumberModel(Long.valueOf(goal.getProgressMaximum()), Long.valueOf(minimum), Long.valueOf(maximum), Long.valueOf(1));
    }

    public static SpinnerNumberModel createNextTarget(Milestone goal) {
        SpinnerNumberModel model = create(goal);
        long minimum = Math.max(((Number) model.getMinimum()).longValue(), goal.getProgressCurrent() + 1);
        if (minimum > ((Number) model.getMaximum()).longValue()) {
            throw new IllegalArgumentException("This milestone is already fully achieved.");
        }
        model.setMinimum(minimum);
        if (((Number) model.getValue()).longValue() < minimum) {
            model.setValue(minimum);
        }
        return model;
    }

    public static void describe(JSpinner spinner) {
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        UiTooltips.set(spinner, String.format(Locale.ROOT, "Target range: %,d to %,d. Type a number or use the arrows. This changes your milestone, not your game stats.", model.getMinimum(), model.getMaximum()));
    }
}
