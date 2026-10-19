package com.grinder.repository;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class RecurringGoal {
    private RepeatSchedule schedule = RepeatSchedule.NONE;
    private long target = 1;
    private long start;
    private long baselineAt;
    private long lastValue;
    private long progress;
    private long completedAt;
    private long lastObservedAt;
    private final List<Period> history = new ArrayList<>();

    public static boolean supports(Goal goal) {
        return goal instanceof CustomGoal || goal instanceof TrackerGoal || goal instanceof SkillExperienceGoal || goal instanceof TotalExperienceGoal;
    }
    public RepeatSchedule getSchedule() {
        return schedule;
    }
    public long getTarget() {
        return target;
    }
    public long getProgress() {
        return progress;
    }
    public long getStart() {
        return start;
    }
    public long getBaselineAt() {
        return baselineAt;
    }
    public long getCompletedAt() {
        return completedAt;
    }
    public long getLastObservedAt() {
        return lastObservedAt;
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
            if (goal instanceof CustomGoal) {
                CustomGoal custom = (CustomGoal) goal;
                custom.update(custom.getTitle(null), custom.getDescription(), custom.getCategory(), 0, custom.getProgressMaximum());
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
        if (goal instanceof CustomGoal) {
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
    public static final class Period {
        private final RepeatSchedule schedule;
        private final long start;
        private final long end;
        private final long progress;
        private final long target;
        private final long baselineAt;
        private final long lastObservedAt;
        private final long completedAt;
        private final String outcome;
        private Period(RepeatSchedule schedule, long start, long end, long progress, long target, long baselineAt, long lastObservedAt, long completedAt, String outcome) {
            this.schedule = schedule;
            this.start = start;
            this.end = end;
            this.progress = progress;
            this.target = target;
            this.baselineAt = baselineAt;
            this.lastObservedAt = lastObservedAt;
            this.completedAt = completedAt;
            this.outcome = outcome;
        }
        public RepeatSchedule getSchedule() {
            return schedule;
        }
        public long getStart() {
            return start;
        }
        public long getEnd() {
            return end;
        }
        public long getProgress() {
            return progress;
        }
        public long getTarget() {
            return target;
        }
        public long getBaselineAt() {
            return baselineAt;
        }
        public long getLastObservedAt() {
            return lastObservedAt;
        }
        public long getCompletedAt() {
            return completedAt;
        }
        public String getOutcome() {
            return outcome;
        }
    }
}
