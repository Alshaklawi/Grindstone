package com.grindstone.ui.util;

import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import static java.util.Map.entry;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class SkillWidgets {
    private static final Map<Integer, Skill> SKILLS = Map.ofEntries(
        entry(InterfaceID.Stats.ATTACK, Skill.ATTACK),
        entry(InterfaceID.Stats.STRENGTH, Skill.STRENGTH),
        entry(InterfaceID.Stats.DEFENCE, Skill.DEFENCE),
        entry(InterfaceID.Stats.RANGED, Skill.RANGED),
        entry(InterfaceID.Stats.PRAYER, Skill.PRAYER),
        entry(InterfaceID.Stats.MAGIC, Skill.MAGIC),
        entry(InterfaceID.Stats.RUNECRAFT, Skill.RUNECRAFT),
        entry(InterfaceID.Stats.CONSTRUCTION, Skill.CONSTRUCTION),
        entry(InterfaceID.Stats.HITPOINTS, Skill.HITPOINTS),
        entry(InterfaceID.Stats.AGILITY, Skill.AGILITY),
        entry(InterfaceID.Stats.HERBLORE, Skill.HERBLORE),
        entry(InterfaceID.Stats.THIEVING, Skill.THIEVING),
        entry(InterfaceID.Stats.CRAFTING, Skill.CRAFTING),
        entry(InterfaceID.Stats.FLETCHING, Skill.FLETCHING),
        entry(InterfaceID.Stats.SLAYER, Skill.SLAYER),
        entry(InterfaceID.Stats.HUNTER, Skill.HUNTER),
        entry(InterfaceID.Stats.MINING, Skill.MINING),
        entry(InterfaceID.Stats.SMITHING, Skill.SMITHING),
        entry(InterfaceID.Stats.FISHING, Skill.FISHING),
        entry(InterfaceID.Stats.COOKING, Skill.COOKING),
        entry(InterfaceID.Stats.FIREMAKING, Skill.FIREMAKING),
        entry(InterfaceID.Stats.WOODCUTTING, Skill.WOODCUTTING),
        entry(InterfaceID.Stats.FARMING, Skill.FARMING),
        entry(InterfaceID.Stats.SAILING, Skill.SAILING)
    );

    private SkillWidgets() {
    }

    public static Skill skillFor(int componentId) {
        return SKILLS.get(componentId);
    }
}
