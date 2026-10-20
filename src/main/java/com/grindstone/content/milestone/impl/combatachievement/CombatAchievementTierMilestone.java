package com.grindstone.content.milestone.impl.combatachievement;
import com.grindstone.content.combatachievement.CombatAchievementTier;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.MilestoneCategory;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CombatAchievementTierMilestone extends ClientCompletionMilestone {
    private final CombatAchievementTier tier;
    public CombatAchievementTierMilestone(CombatAchievementTier tier) {
        this.tier = Objects.requireNonNull(tier);
    }
    @Override
    public String selectionKey() {
        return Objects.requireNonNull(tier).name();
    }
    @Override
    public String getTitle(Client client) {
        return tier + " combat achievement tier";
    }
    @Override
    public String getDescription() {
        return "Reach the reward tier using points earned from tasks of any difficulty.";
    }
    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.COMBAT_ACHIEVEMENT;
    }
    @Override
    public boolean refresh(Client client) {
        return refreshValues(client::getVarbitValue);
    }
    public boolean refreshValues(IntUnaryOperator values) {
        int points = values.applyAsInt(VarbitID.CA_POINTS);
        return update(points, values.applyAsInt(tier.getThreshold()), points > 0);
    }
    @Override
    public String getProgressText() {
        return hasProgress() ? super.getProgressText() + " points" : "Waiting for client tier threshold";
    }
}
