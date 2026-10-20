package com.grindstone.content.milestone.impl.combatachievement;
import com.grindstone.content.combatachievement.CombatAchievementData;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.MilestoneCategory;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.runelite.api.Client;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CombatAchievementBossMilestone extends ClientCompletionMilestone {
    private int boss;
    private final String name;
    private transient List<Integer> tasks;
    public CombatAchievementBossMilestone(int boss, String name) {
        if (boss < 0) {
            throw new IllegalArgumentException("Invalid boss");
        }
        this.boss = boss;
        this.name = Objects.requireNonNull(name);
    }
    public CombatAchievementBossMilestone(String name) {
        this.boss = -1;
        this.name = Objects.requireNonNull(name).trim();
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("Boss name is required");
        }
    }
    public boolean matches(CombatAchievementBossMilestone other) {
        return boss >= 0 && other.boss >= 0 ? boss == other.boss : name.equalsIgnoreCase(other.name);
    }
    @Override
    public String selectionKey() {
        Objects.requireNonNull(name);
        if (boss < -1 || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid boss");
        }
        return boss >= 0 ? Integer.toString(boss) : "name:" + name.toLowerCase(Locale.ROOT);
    }
    @Override
    public String getTitle(Client client) {
        return name + " combat achievements";
    }
    @Override
    public String getDescription() {
        return "Complete every combat achievement for this boss or activity.";
    }
    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.COMBAT_ACHIEVEMENT;
    }
    @Override
    public boolean refresh(Client client) {
        if (boss < 0) {
            for (int candidate : CombatAchievementData.tasksByBoss(client).keySet()) {
                if (name.equalsIgnoreCase(CombatAchievementData.bossName(client, candidate))) {
                    boss = candidate;
                    break;
                }
            }
            if (boss < 0) {
                return update(0, 0, false);
            }
        }
        if (tasks == null || tasks.isEmpty()) {
            tasks = CombatAchievementData.taskIds(client, boss);
        }
        long done = tasks.stream().filter(task -> CombatAchievementData.isComplete(client, task)).count();
        return update(done, tasks.size(), done > 0);
    }
    @Override
    public String getProgressText() {
        return hasProgress() ? super.getProgressText() + " tasks" : "Waiting for client task data";
    }
}
