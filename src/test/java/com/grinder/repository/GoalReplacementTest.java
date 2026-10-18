package com.grinder.repository;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalReplacementTest {
    @Test public void replacingSubjectPreservesPinButDoesNotMisattributeHistory() {
        GrinderProfile profile = new GrinderProfile(42);
        Goal attack = new SkillLevelGoal(Skill.ATTACK);
        profile.replace(Collections.singletonList(attack));
        profile.metadata(attack).setPinned(true);
        Goal defence = new SkillLevelGoal(Skill.DEFENCE);
        assertTrue(profile.replaceGoal(attack, defence));
        assertTrue(profile.metadata(defence).isPinned());
        assertTrue(profile.metadata(defence).getCompletions().isEmpty());
        assertSame(defence, profile.getGoals().get(0));
    }
    @Test public void editingTargetKeepsStableMetadataAndRejectsDuplicates() {
        GrinderProfile profile = new GrinderProfile(42);
        Goal attack = profile.findMatchingGoal(new SkillLevelGoal(Skill.ATTACK));
        String id = profile.metadata(attack).getId();
        SkillLevelGoal edited = new SkillLevelGoal(Skill.ATTACK);
        edited.setTarget(75);
        assertTrue(profile.replaceGoal(attack, edited));
        assertEquals(id, profile.metadata(edited).getId());
        try { profile.replaceGoal(edited, new SkillLevelGoal(Skill.DEFENCE)); fail("Duplicate must be rejected"); }
        catch (IllegalArgumentException expected) { assertTrue(profile.getGoals().contains(edited)); }
    }
}
