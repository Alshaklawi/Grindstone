package com.grinder.content.goal.impl.achievement_diary;
import com.grinder.content.AchievementDiaryArea;
import com.grinder.content.DiaryTier;
import com.grinder.content.goal.ClientCompletionGoal;
import com.grinder.content.goal.GoalCategory;
import java.util.Objects;
import net.runelite.api.Client;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class AchievementDiaryTierAreaGoal extends ClientCompletionGoal
{
    private final AchievementDiaryArea area;
    private final DiaryTier tier;
    public AchievementDiaryTierAreaGoal(AchievementDiaryArea area, DiaryTier tier)
    { this.area = Objects.requireNonNull(area); this.tier = Objects.requireNonNull(tier); }
    @Override public String selectionKey() { return Objects.requireNonNull(area).name() + ":" + Objects.requireNonNull(tier).name(); }
    @Override public String getTitle(Client client) { return area + " — " + tier + " diary"; }
    @Override public String getDescription() { return "Complete all tasks in this diary tier. Claiming its reward is not required."; }
    @Override public GoalCategory getCategory() { return GoalCategory.ACHIEVEMENT_DIARY; }
    @Override public boolean refresh(Client client)
    {
        return update(area.isComplete(client, tier) ? 1 : 0, 1, area.taskCount(client, tier) > 0);
    }
    @Override public String getProgressText() { return getProgressCurrent() == 1 ? "Diary tier complete" : "Diary tier incomplete"; }
}
