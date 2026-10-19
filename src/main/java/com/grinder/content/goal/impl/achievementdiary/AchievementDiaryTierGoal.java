package com.grinder.content.goal.impl.achievementdiary;
import com.grinder.content.diary.AchievementDiaryArea;
import com.grinder.content.diary.DiaryTier;
import com.grinder.content.goal.ClientCompletionGoal;
import com.grinder.content.goal.GoalCategory;
import java.util.Objects;
import net.runelite.api.Client;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class AchievementDiaryTierGoal extends ClientCompletionGoal {
    private final DiaryTier tier;
    public AchievementDiaryTierGoal(DiaryTier tier) {
        this.tier = Objects.requireNonNull(tier);
    }
    @Override public String selectionKey() {
        return "" + ":" + Objects.requireNonNull(tier).name();
    }
    @Override public String getTitle(Client client) {
        return "All " + tier + " diaries";
    }
    @Override public String getDescription() {
        return "Complete this diary tier in every region.";
    }
    @Override public GoalCategory getCategory() {
        return GoalCategory.ACHIEVEMENT_DIARY;
    }
    @Override public boolean refresh(Client client) {
        int done = 0;
        boolean started = false;
        for (AchievementDiaryArea area : AchievementDiaryArea.values()) {
            if (area.isComplete(client, tier)) {
                done++;
            }
            started |= area.taskCount(client, tier) > 0;
        }
        return update(done, AchievementDiaryArea.values().length, started);
    }
    @Override public String getProgressText() {
        return super.getProgressText() + " regions";
    }
}
