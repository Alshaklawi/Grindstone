package com.grinder.content.goal.impl.quest;
import com.grinder.content.goal.ClientCompletionGoal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.quest.QuestSeries;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class QuestSeriesGoal extends ClientCompletionGoal {
    private final QuestSeries series;
    public QuestSeriesGoal(QuestSeries series) {
        this.series = Objects.requireNonNull(series);
    }
    @Override
    public String selectionKey() {
        return Objects.requireNonNull(series).name();
    }
    @Override
    public String getTitle(Client client) {
        return series + " quest series";
    }
    @Override
    public String getDescription() {
        return series.getQuests().stream().map(Quest::getName).collect(Collectors.joining(", "));
    }
    @Override
    public GoalCategory getCategory() {
        return GoalCategory.QUEST;
    }
    @Override
    public boolean refresh(Client client) {
        return refreshStates(quest -> quest.getState(client));
    }
    public boolean refreshStates(Function<Quest, QuestState> states) {
        int done = 0;
        boolean started = false;
        for (Quest quest : series.getQuests()) {
            QuestState state = states.apply(quest);
            if (state == QuestState.FINISHED) {
                done++;
            }
            started |= state != QuestState.NOT_STARTED;
        }
        return update(done, series.getQuests().size(), started);
    }
    @Override
    public String getProgressText() {
        return super.getProgressText() + " quests";
    }
}
