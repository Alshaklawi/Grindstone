package com.grindstone.content.milestone.impl.quest;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.MilestoneCategory;
import java.util.Objects;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class QuestMilestone extends ClientCompletionMilestone {
    private final Quest quest;
    public QuestMilestone(Quest quest) {
        this.quest = Objects.requireNonNull(quest);
    }
    @Override
    public String selectionKey() {
        return Objects.requireNonNull(quest).name();
    }
    @Override
    public String getTitle(Client client) {
        return quest.getName();
    }
    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.QUEST;
    }
    @Override
    public boolean refresh(Client client) {
        return refreshState(quest.getState(client));
    }
    public boolean refreshState(QuestState state) {
        return update(state == QuestState.FINISHED ? 1 : 0, 1, state == QuestState.IN_PROGRESS);
    }
    @Override
    public String getProgressText() {
        return getStatus().toString().replace('_', ' ');
    }
}
