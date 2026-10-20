package com.grindstone.content.milestone.impl.combatachievement;
import lombok.Getter;
import com.grindstone.content.combatachievement.CombatAchievementData;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.MilestoneCategory;
import net.runelite.api.Client;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class CombatAchievementTaskMilestone extends ClientCompletionMilestone {
    @Getter
    private final int task;
    private final String name;
    private final String description;
    private transient CombatAchievementData.Task definition;
    public CombatAchievementTaskMilestone(int task, String name, String description) {
        this.task = task;
        this.name = name;
        this.description = description;
        selectionKey();
    }
    @Override
    public String selectionKey() {
        if (!CombatAchievementData.supportsTask(task) || name == null || name.isEmpty() || name.length() > 300 || description == null || description.length() > 4000) {
            throw new IllegalArgumentException("Invalid combat achievement task");
        }
        return Integer.toString(task);
    }
    @Override
    public String getTitle(Client client) {
        return definition == null ? name : definition.getName();
    }
    @Override
    public String getDescription() {
        return definition == null ? description : definition.getDescription();
    }
    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.COMBAT_ACHIEVEMENT;
    }
    @Override
    public boolean refresh(Client client) {
        definition = CombatAchievementData.tasks(client).get(task);
        return definition == null ? update(0, 0, false) : observeCompletion(CombatAchievementData.isComplete(client, task));
    }
    public boolean observeCompletion(boolean complete) {
        return update(complete ? 1 : 0, 1, complete);
    }
    @Override
    public String getProgressText() {
        return !hasProgress() ? "Waiting for native task data" : getProgressCurrent() == 1 ? "Task completed" : "Task not completed";
    }
}
