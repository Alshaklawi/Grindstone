package com.grinder.repository;

import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.CustomGoal;
import net.runelite.http.api.RuneLiteAPI;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class TestProfiles {
    public static final GrinderRepository REPOSITORY = new GrinderRepository(RuneLiteAPI.GSON);

    private TestProfiles() {
    }

    public static GrinderProfile populated(long hash) {
        GrinderProfile profile = new GrinderProfile(hash);
        profile.add(new CustomGoal("Saved goal", "Player notes", GoalCategory.OTHER, 3, 10));
        return profile;
    }
}
