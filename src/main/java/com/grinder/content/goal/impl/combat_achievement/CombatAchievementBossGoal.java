package com.grinder.content.goal.impl.combat_achievement;
import com.grinder.content.CombatAchievementData;
import com.grinder.content.goal.ClientCompletionGoal;
import com.grinder.content.goal.GoalCategory;
import java.util.List;
import java.util.Objects;
import net.runelite.api.Client;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CombatAchievementBossGoal extends ClientCompletionGoal
{
    private final int boss;
    private final String name;
    private transient List<Integer> tasks;
    public CombatAchievementBossGoal(int boss, String name)
    { if (boss < 0) { throw new IllegalArgumentException("Invalid boss"); } this.boss = boss; this.name = Objects.requireNonNull(name); }
    @Override public String selectionKey()
    { Objects.requireNonNull(name); if (boss < 0) { throw new IllegalArgumentException("Invalid boss"); } return Integer.toString(boss); }
    @Override public String getTitle(Client client) { return name + " combat achievements"; }
    @Override public String getDescription() { return "Complete every combat achievement for this boss or activity."; }
    @Override public GoalCategory getCategory() { return GoalCategory.COMBAT_ACHIEVEMENT; }
    @Override public boolean refresh(Client client)
    {
        if (tasks == null || tasks.isEmpty()) { tasks = CombatAchievementData.taskIds(client, boss); }
        long done = tasks.stream().filter(task -> CombatAchievementData.isComplete(client, task)).count();
        return update(done, tasks.size(), done > 0);
    }
    @Override public String getProgressText() { return hasProgress() ? super.getProgressText() + " tasks" : "Waiting for client task data"; }
}
