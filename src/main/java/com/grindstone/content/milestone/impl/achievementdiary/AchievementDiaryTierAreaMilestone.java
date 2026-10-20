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
public final class AchievementDiaryTierAreaMilestone extends ClientCompletionMilestone {
    private final AchievementDiaryArea area;
    private final DiaryTier tier;
    public AchievementDiaryTierAreaMilestone(AchievementDiaryArea area, DiaryTier tier) {
        this.area = Objects.requireNonNull(area);
        this.tier = Objects.requireNonNull(tier);
    }
    @Override
    public String selectionKey() {
        return Objects.requireNonNull(area).name() + ":" + Objects.requireNonNull(tier).name();
    }
    @Override
    public String getTitle(Client client) {
        return area + " — " + tier + " diary";
    }
    @Override
    public String getDescription() {
        return "Complete all tasks in this diary tier. Claiming its reward is not required.";
    }
    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.ACHIEVEMENT_DIARY;
    }
    @Override
    public boolean refresh(Client client) {
        return update(area.isComplete(client, tier) ? 1 : 0, 1, area.getTaskCount(client, tier) > 0);
    }
    @Override
    public String getProgressText() {
        return getProgressCurrent() == 1 ? "Diary tier complete" : "Diary tier incomplete";
    }
}
