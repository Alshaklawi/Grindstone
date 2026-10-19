package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.ManualGoal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class RecurringGoal {
    @Getter
    private RepeatSchedule schedule = RepeatSchedule.NONE;
    @Getter
    private long target = 1;
    @Getter
    private long start;
    @Getter
    private long baselineAt;
    private long lastValue;
    @Getter
    private long progress;
    @Getter
    private long completedAt;
    @Getter
    private long lastObservedAt;
    private final List<Period> history = new ArrayList<>();
    public RecurringGoal copy() {
        RecurringGoal copy = new RecurringGoal();
        copy.schedule = schedule;
        copy.target = target;
        copy.start = start;
        copy.baselineAt = baselineAt;
        copy.lastValue = lastValue;
        copy.progress = progress;
        copy.completedAt = completedAt;
        copy.lastObservedAt = lastObservedAt;
        copy.history.addAll(history);
        return copy;
    }

    public static boolean supports(Goal goal) {
        return goal instanceof ManualGoal || goal instanceof TrackerGoal || goal instanceof SkillExperienceGoal || goal instanceof TotalExperienceGoal;
    }
    public List<Period> getHistory() {
        return Collections.unmodifiableList(history);
    }
    public boolean isActive() {
        return schedule != RepeatSchedule.NONE;
    }
    public void stop(long now, String outcome) {
        if (isActive() && baselineAt > 0) {
            archive(now, outcome);
        }
        schedule = RepeatSchedule.NONE;
        start = 0;
        baselineAt = 0;
        progress = 0;
        completedAt = 0;
        lastObservedAt = 0;
    }
    public void restart(long now) {
        if (!isActive()) {
            throw new IllegalStateException("Only recurring goals have periods.");
        }
        if (baselineAt > 0) {
            archive(now, "Period restarted · last observed");
        }
        start = 0;
        baselineAt = 0;
        progress = 0;
        completedAt = 0;
        lastObservedAt = 0;
    }
    public GoalStatus getStatus() {
        return progress >= target ? GoalStatus.COMPLETE : progress > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED;
    }

    public void configure(RepeatSchedule schedule, long target, long now) {
        if (schedule == null || target < 1 || target > Integer.MAX_VALUE || now <= 0) {
            throw new IllegalArgumentException("Choose a repeat schedule and a target from 1 to 2,147,483,647.");
        }
        if (this.schedule == schedule && this.target == target) {
            return;
        }
        if (isActive() && baselineAt > 0) {
            archive(now, "Schedule changed");
        }
        this.schedule = schedule;
        this.target = target;
        start = 0;
        baselineAt = 0;
        progress = 0;
        completedAt = 0;
        lastObservedAt = 0;
    }

    public void observe(Goal goal, long now) {
        if (!isActive() || !supports(goal) || now <= 0 || now < lastObservedAt) {
            return;
        }
        long periodStart = schedule.start(now);
        if (baselineAt > 0 && periodStart > start) {
            long previousEnd = schedule.next(start);
            archive(previousEnd, completedAt > 0 ? "Completed" : "Incomplete · last observed");
            if (periodStart > previousEnd) {
                append(new Period(schedule, previousEnd, periodStart, 0, target, 0, 0, 0, "Not observed"));
            }
            baselineAt = 0;
            if (goal instanceof ManualGoal) {
                ((ManualGoal) goal).resetProgress();
            }
        }
        long value = Math.max(0, goal.getProgressCurrent());
        if (baselineAt == 0) {
            start = periodStart;
            baselineAt = now;
            progress = 0;
            completedAt = 0;
            lastValue = value;
        }
        if (goal instanceof ManualGoal) {
            progress = value;
        } else if (value >= lastValue) {
            progress = Math.min(Long.MAX_VALUE - (value - lastValue), progress) + (value - lastValue);
        }
        lastValue = value;
        lastObservedAt = now;
        if (progress >= target && completedAt == 0) {
            completedAt = now;
        }
    }

    private void archive(long end, String outcome) {
        append(new Period(schedule, start, Math.max(start + 1, end), progress, target, baselineAt, lastObservedAt, completedAt, outcome));
    }
    private void append(Period period) {
        history.add(period);
        if (history.size() > 500) {
            history.remove(0);
        }
    }

    public void validate() {
        if (schedule == null || target < 1 || target > Integer.MAX_VALUE || start < 0 || baselineAt < 0 || lastValue < 0 || progress < 0 || completedAt < 0 || lastObservedAt < 0 || history == null || history.size() > 500) {
            throw new IllegalArgumentException("Invalid recurring goal");
        }
        for (Period period : history) {
            if (period == null || period.schedule == null || period.start < 0 || period.end <= period.start || period.progress < 0 || period.target < 1 || period.baselineAt < 0 || period.lastObservedAt < 0 || period.completedAt < 0 || period.outcome == null || period.outcome.length() > 100) {
                throw new IllegalArgumentException("Invalid period history");
            }
        }
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Period {
        @Getter
        private final RepeatSchedule schedule;
        @Getter
        private final long start;
        @Getter
        private final long end;
        @Getter
        private final long progress;
        @Getter
        private final long target;
        @Getter
        private final long baselineAt;
        @Getter
        private final long lastObservedAt;
        @Getter
        private final long completedAt;
        @Getter
        private final String outcome;
    }
}
