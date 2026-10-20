package com.grindstone.content.milestone.impl.level;

import com.grindstone.content.milestone.BaseSkillMilestone;
import net.runelite.api.Client;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class BaseLevelMilestone extends BaseSkillMilestone {
    @Override
    protected boolean tracksExperience() {
        return false;
    }

    @Override
    public String getTitle(Client client) {
        return "Base level";
    }
}
