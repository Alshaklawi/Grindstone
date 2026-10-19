package com.grindstone.config;

import com.grindstone.repository.RepeatSchedule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
@Getter
@RequiredArgsConstructor
public enum ProgressOverlayView {
    DAILY("Daily routines"),
    ROUTINES("All routines"),
    MILESTONES("Milestones"),
    PINNED("Pinned"),
    ALL("All tracked");

    private final String title;

    public boolean includes(RepeatSchedule schedule, boolean pinned) {
        switch (this) {
            case DAILY:
                return schedule == RepeatSchedule.DAILY;
            case ROUTINES:
                return schedule != RepeatSchedule.NONE;
            case MILESTONES:
                return schedule == RepeatSchedule.NONE;
            case PINNED:
                return pinned;
            default:
                return true;
        }
    }

    @Override
    public String toString() {
        return title;
    }
}
