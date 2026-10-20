package com.grindstone.content.milestone.impl.combatachievement;
import com.grindstone.content.combatachievement.CombatAchievementData;
import com.grindstone.content.combatachievement.CombatAchievementTier;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.MilestoneCategory;
import java.util.List;
import java.util.Objects;
import net.runelite.api.Client;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CombatAchievementDifficultyMilestone extends ClientCompletionMilestone {
    private final CombatAchievementTier tier;
    private transient List<Integer> tasks;
    public CombatAchievementDifficultyMilestone(CombatAchievementTier tier) {
        this.tier = Objects.requireNonNull(tier);
    }
    @Override
    public String selectionKey() {
        return Objects.requireNonNull(tier).name();
    }
    @Override
    public String getTitle(Client client) {
        return "All " + tier + " combat tasks";
    }
    @Override
    public String getDescription() {
        return "Complete every task of this difficulty, regardless of your points or reward tier.";
    }
    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.COMBAT_ACHIEVEMENT;
    }
    @Override
    public boolean refresh(Client client) {
        if (tasks == null || tasks.isEmpty()) {
            tasks = CombatAchievementData.taskIds(client, tier);
        }
        long done = tasks.stream().filter(task -> CombatAchievementData.isComplete(client, task)).count();
        return update(done, tasks.size(), done > 0);
    }
    @Override
    public String getProgressText() {
        return hasProgress() ? super.getProgressText() + " tasks" : "Waiting for client task data";
    }
}
