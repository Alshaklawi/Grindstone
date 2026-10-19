package com.grindstone.content.combatachievement;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum CombatAchievementTier {
    EASY("Easy", VarbitID.CA_THRESHOLD_EASY, 3981),
    MEDIUM("Medium", VarbitID.CA_THRESHOLD_MEDIUM, 3982),
    HARD("Hard", VarbitID.CA_THRESHOLD_HARD, 3983),
    ELITE("Elite", VarbitID.CA_THRESHOLD_ELITE, 3984),
    MASTER("Master", VarbitID.CA_THRESHOLD_MASTER, 3985),
    GRANDMASTER("Grandmaster", VarbitID.CA_THRESHOLD_GRANDMASTER, 3986);
    private final String name;
    @Getter
    private final int threshold;
    @Getter
    private final int taskEnum;
    CombatAchievementTier(String name, int threshold, int taskEnum) {
        this.name = name;
        this.threshold = threshold;
        this.taskEnum = taskEnum;
    }
    @Override
    public String toString() {
        return name;
    }
}
