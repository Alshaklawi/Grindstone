package com.grinder.content.combatachievement;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;
import net.runelite.api.gameval.VarPlayerID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CombatAchievementData {
    private static final int BOSS_NAMES_ENUM = 3971;
    private static final int TASK_ID_PARAM = 1306;
    private static final int TASK_BOSS_PARAM = 1312;
    private static final int[] COMPLETED = {
        VarPlayerID.CA_TASK_COMPLETED_0,
        VarPlayerID.CA_TASK_COMPLETED_1,
        VarPlayerID.CA_TASK_COMPLETED_2,
        VarPlayerID.CA_TASK_COMPLETED_3,
        VarPlayerID.CA_TASK_COMPLETED_4,
        VarPlayerID.CA_TASK_COMPLETED_5,
        VarPlayerID.CA_TASK_COMPLETED_6,
        VarPlayerID.CA_TASK_COMPLETED_7,
        VarPlayerID.CA_TASK_COMPLETED_8,
        VarPlayerID.CA_TASK_COMPLETED_9,
        VarPlayerID.CA_TASK_COMPLETED_10,
        VarPlayerID.CA_TASK_COMPLETED_11,
        VarPlayerID.CA_TASK_COMPLETED_12,
        VarPlayerID.CA_TASK_COMPLETED_13,
        VarPlayerID.CA_TASK_COMPLETED_14,
        VarPlayerID.CA_TASK_COMPLETED_15,
        VarPlayerID.CA_TASK_COMPLETED_16,
        VarPlayerID.CA_TASK_COMPLETED_17,
        VarPlayerID.CA_TASK_COMPLETED_18,
        VarPlayerID.CA_TASK_COMPLETED_19,
        VarPlayerID.CA_TASK_COMPLETED_20
    };
    private CombatAchievementData() {}
    public static Map<Integer, List<Integer>> tasksByBoss(Client client) {
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        for (CombatAchievementTier tier : CombatAchievementTier.values()) {
            EnumComposition tasks = client.getEnum(tier.getTaskEnum());
            if (tasks == null || tasks.getIntVals() == null) { continue; }
            for (int structId : tasks.getIntVals()) {
                StructComposition task = client.getStructComposition(structId);
                if (task == null) { continue; }
                int id = task.getIntValue(TASK_ID_PARAM);
                int boss = task.getIntValue(TASK_BOSS_PARAM);
                if (id < 0 || id >= COMPLETED.length * 32 || boss < 0) { continue; }
                List<Integer> ids = result.computeIfAbsent(boss, ignored -> new ArrayList<>());
                if (!ids.contains(id)) { ids.add(id); }
            }
        }
        return result;
    }
    public static String bossName(Client client, int boss) {
        EnumComposition names = client.getEnum(BOSS_NAMES_ENUM);
        String name = names == null ? null : names.getStringValue(boss);
        return name == null || name.isEmpty() || "null".equals(name) ? "Other combat tasks" : name;
    }
    public static List<Integer> taskIds(Client client, int boss)
    { return tasksByBoss(client).getOrDefault(boss, Collections.emptyList()); }
    public static List<Integer> taskIds(Client client, CombatAchievementTier tier) {
        List<Integer> ids = new ArrayList<>();
        EnumComposition tasks = client.getEnum(tier.getTaskEnum());
        if (tasks == null || tasks.getIntVals() == null) { return ids; }
        for (int structId : tasks.getIntVals()) {
            StructComposition task = client.getStructComposition(structId);
            if (task == null) { continue; }
            int id = task.getIntValue(TASK_ID_PARAM);
            if (id >= 0 && id < COMPLETED.length * 32 && !ids.contains(id)) { ids.add(id); }
        }
        return ids;
    }
    public static boolean isComplete(Client client, int task)
    { return isComplete(client::getVarpValue, task); }
    public static boolean isComplete(java.util.function.IntUnaryOperator values, int task)
    { return task >= 0 && task < COMPLETED.length * 32 && (values.applyAsInt(COMPLETED[task / 32]) & (1 << (task % 32))) != 0; }
}
