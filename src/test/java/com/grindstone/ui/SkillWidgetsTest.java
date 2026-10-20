package com.grindstone.ui;

import com.grindstone.ui.util.SkillWidgets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class SkillWidgetsTest {
    @Test
    public void mapsEverySkillOnceWithoutTreatingTotalAsASkill() {
        Set<Skill> skills = new HashSet<>();
        for (int widget = InterfaceID.Stats.ATTACK; widget <= InterfaceID.Stats.SAILING; widget++) {
            assertTrue(skills.add(SkillWidgets.getSkillFor(widget)));
        }
        assertEquals(new HashSet<>(Arrays.asList(Skill.values())), skills);
        assertEquals(Skill.STRENGTH, SkillWidgets.getSkillFor(InterfaceID.Stats.STRENGTH));
        assertEquals(Skill.DEFENCE, SkillWidgets.getSkillFor(InterfaceID.Stats.DEFENCE));
        assertNull(SkillWidgets.getSkillFor(InterfaceID.Stats.TOTAL));
        assertNull(SkillWidgets.getSkillFor(InterfaceID.Stats.UNIVERSE));
    }
}
