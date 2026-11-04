package com.grindstone.content.milestone;

import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import com.grindstone.repository.GrindstoneProfile;
import java.util.EnumSet;
import java.util.Set;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@Singleton
public final class MilestoneUpdates {
    @Getter
    private boolean variablesDirty;
    private boolean storageDirty;
    private final Set<Skill> skills = EnumSet.noneOf(Skill.class);

    public void markVariablesChanged() {
        variablesDirty = true;
    }

    public void markStorageChanged() {
        storageDirty = true;
    }

    public void markSkillChanged(Skill skill) {
        skills.add(skill);
    }

    public void clear() {
        variablesDirty = false;
        storageDirty = false;
        skills.clear();
    }

    public boolean refresh(GrindstoneProfile profile, Client client) {
        if (!variablesDirty && !storageDirty && skills.isEmpty()) {
            return false;
        }
        boolean changed = storageDirty;
        if (variablesDirty || storageDirty) {
            changed |= profile.getPlayerStorage().refreshVariables(client);
        }
        for (Milestone goal : profile.getGoals()) {
            boolean affected = variablesDirty && (goal instanceof TrackerMilestone || goal instanceof ClientCompletionMilestone);
            affected |= (variablesDirty || storageDirty) && goal instanceof PossessedItemMilestone;
            if (!skills.isEmpty()) {
                affected |= goal instanceof SkillLevelMilestone && skills.contains(((SkillLevelMilestone) goal).getSkill());
                affected |= goal instanceof SkillExperienceMilestone && skills.contains(((SkillExperienceMilestone) goal).getSkill());
                affected |= goal instanceof TotalLevelMilestone || goal instanceof TotalExperienceMilestone || goal instanceof CombatLevelMilestone || goal instanceof BaseSkillMilestone;
                affected |= goal instanceof SessionExperienceMilestone && (((SessionExperienceMilestone) goal).isTotal() || skills.contains(((SessionExperienceMilestone) goal).getSkill()));
            }
            if (affected) {
                changed |= goal.refresh(client);
            }
        }
        clear();
        if (changed) {
            profile.markDirty();
        }
        return changed;
    }
}
