package com.grinder.repository;

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
        populate(profile);
        return profile;
    }

    public static void populate(GrinderProfile profile) {
        new GoalFixtures(profile).populate();
    }
}
