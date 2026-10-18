package com.grinder.content.goal.impl;


import com.grinder.content.TrackerEntry;
import com.grinder.content.goal.Goal;
import lombok.Getter;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class TrackerGoal implements Goal {

    @Getter
    private final TrackerEntry tracker;

    @Getter
    private final int count;

    public TrackerGoal(final TrackerEntry entry, final int count) {
        this.tracker = entry;
        this.count = count;
    }
}
