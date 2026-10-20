package com.grindstone.repository;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.CustomMilestone;
import net.runelite.http.api.RuneLiteAPI;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class TestProfiles {
    public static final GrindstoneRepository REPOSITORY = new GrindstoneRepository(RuneLiteAPI.GSON);

    private TestProfiles() {
    }

    public static GrindstoneProfile populated(long hash) {
        GrindstoneProfile profile = new GrindstoneProfile(hash);
        profile.add(new CustomMilestone("Saved goal", "Player notes", MilestoneCategory.OTHER, 3, 10));
        return profile;
    }
}
