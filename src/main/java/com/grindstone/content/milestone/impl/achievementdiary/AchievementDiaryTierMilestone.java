package com.grindstone.content.milestone.impl.achievementdiary;
import com.grindstone.content.diary.AchievementDiaryArea;
import com.grindstone.content.diary.DiaryTier;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.MilestoneCategory;
import java.util.Objects;
import net.runelite.api.Client;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class AchievementDiaryTierMilestone extends ClientCompletionMilestone {
    private final DiaryTier tier;
    public AchievementDiaryTierMilestone(DiaryTier tier) {
        this.tier = Objects.requireNonNull(tier);
    }
    @Override
    public String selectionKey() {
        return "" + ":" + Objects.requireNonNull(tier).name();
    }
    @Override
    public String getTitle(Client client) {
        return "All " + tier + " diaries";
    }
    @Override
    public String getDescription() {
        return "Complete this diary tier in every region.";
    }
    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.ACHIEVEMENT_DIARY;
    }
    @Override
    public boolean refresh(Client client) {
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
    @Override
    public String getProgressText() {
        return super.getProgressText() + " regions";
    }
}
