package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.ChecklistGoal;
import com.grinder.content.goal.impl.LinkedGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
final class GoalExamples {
    private GoalExamples() {
    }

    static void addLinkedExamples(GrinderProfile profile) {
        Goal attack = profile.findMatchingGoal(new SkillLevelGoal(Skill.ATTACK));
        Goal strength = profile.findMatchingGoal(new SkillLevelGoal(Skill.STRENGTH));
        Goal defence = profile.findMatchingGoal(new SkillLevelGoal(Skill.DEFENCE));
        add(profile, LinkedGoal.Mode.CHECKLIST, Arrays.asList(attack, strength), new long[]{50, 50}, 1);
        add(profile, LinkedGoal.Mode.ANY_OF, Arrays.asList(attack, strength, defence), new long[]{50, 50, 50}, 2);
        add(profile, LinkedGoal.Mode.DEPENDENCY, Arrays.asList(attack, strength), new long[]{75, 50}, 1);
        Goal pickaxe = profile.findMatchingGoal(new CollectionLogItemGoal(ItemID.DRAGON_PICKAXE, 1));
        Goal bow = profile.findMatchingGoal(new CollectionLogItemGoal(ItemID.TWISTED_BOW, 1));
        add(profile, LinkedGoal.Mode.COLLECTION_SET, Arrays.asList(pickaxe, bow), new long[]{1, 1}, 1);
        Goal coins = profile.findMatchingGoal(new PossessedItemGoal(ItemID.COINS));
        add(profile, LinkedGoal.Mode.ACHIEVED, Collections.singletonList(coins), new long[]{100_000_000}, 1);
        ChecklistGoal routine = new ChecklistGoal(id("daily-checklist"), "Example: daily personal checklist", "A manual daily routine example. The UTC period requires every step; it does not represent a game's daily reset rules.", Arrays.asList("Choose an activity", "Prepare supplies", "Complete the activity"), new boolean[3]);
        GoalMetadata metadata = new GoalMetadata();
        metadata.recurring().configure(RepeatSchedule.DAILY, 3, System.currentTimeMillis());
        profile.add(routine, metadata);
        Goal source = profile.findMatchingGoal(routine, RepeatSchedule.DAILY);
        add(profile, LinkedGoal.Mode.REPEAT_COMPLETIONS, Collections.singletonList(source), new long[]{3}, 3);
    }

    private static void add(GrinderProfile profile, LinkedGoal.Mode mode, List<Goal> goals, long[] targets, int required) {
        List<String> sources = goals.stream().map(goal -> profile.metadata(goal).getId()).collect(Collectors.toList());
        LinkedGoal example = new LinkedGoal(id("linked-" + mode.name()), "Example: " + mode, "A demonstration using existing source trackers. Edit its sources or use it as a real goal from its actions menu.", mode, sources, targets, required);
        GoalMetadata metadata = new GoalMetadata();
        profile.add(example, metadata);
    }

    private static String id(String name) {
        return UUID.nameUUIDFromBytes(("grinder-default-" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
