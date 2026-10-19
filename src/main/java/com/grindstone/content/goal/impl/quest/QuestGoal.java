package com.grindstone.content.goal.impl.quest;
import com.grindstone.content.goal.ClientCompletionGoal;
import com.grindstone.content.goal.GoalCategory;
import java.util.Objects;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class QuestGoal extends ClientCompletionGoal {
    private final Quest quest;
    public QuestGoal(Quest quest) {
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
    public GoalCategory getCategory() {
        return GoalCategory.QUEST;
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
