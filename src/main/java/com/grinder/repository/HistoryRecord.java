package com.grinder.repository;

import com.grinder.content.goal.Goal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class HistoryRecord {
    public final String goalId;
    public final String title;
    public final RepeatSchedule schedule;
    public final long start;
    public final long end;
    public final Long progress;
    public final long target;
    public final String outcome;
    public final long baseline;
    public final long observed;
    public final long completed;
    public final boolean lifetime;
    public HistoryRecord(String goalId, String title, RepeatSchedule schedule, long start, long end, Long progress, long target, String outcome, long baseline, long observed, long completed, boolean lifetime) {
        this.goalId = goalId;
        this.title = title;
        this.schedule = schedule;
        this.start = start;
        this.end = end;
        this.progress = progress;
        this.target = target;
        this.outcome = outcome;
        this.baseline = baseline;
        this.observed = observed;
        this.completed = completed;
        this.lifetime = lifetime;
    }
    public boolean matches(String query, RepeatSchedule repeat, long from, long to) {
        return (repeat == null || schedule == repeat) && (query == null || (title + " " + outcome).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).trim())) && (end == 0 || end > from) && start < to;
    }
    public static List<HistoryRecord> snapshot(GrinderProfile profile, Function<Goal, String> title) {
        List<HistoryRecord> records = new ArrayList<>();
        for (Goal goal : profile.getGoals()) {
            GoalMetadata metadata = profile.metadata(goal);
            String name = title.apply(goal);
            append(records, metadata, name, metadata.getId());
        }
        for (GrinderProfile.ArchivedHistory archived : profile.archivedHistory()) {
            append(records, archived.getMetadata(), archived.getTitle() + " (archived)", "");
        }
        records.sort(java.util.Comparator.comparingLong((HistoryRecord value) -> value.start).reversed());
        return records;
    }
    private static void append(List<HistoryRecord> records, GoalMetadata metadata, String name, String id) {
        RecurringGoal recurring = metadata.recurring();
        if (recurring.isActive()) {
            long start = recurring.getStart();
            records.add(new HistoryRecord(id, name, recurring.getSchedule(), start, recurring.getBaselineAt() == 0 ? 0 : recurring.getSchedule().next(start), recurring.getBaselineAt() == 0 ? null : recurring.getProgress(), recurring.getTarget(), recurring.getBaselineAt() == 0 ? "Waiting for data" : recurring.getCompletedAt() > 0 ? "Completed · current period" : "Current period", recurring.getBaselineAt(), recurring.getLastObservedAt(), recurring.getCompletedAt(), false));
        }
        for (RecurringGoal.Period period : recurring.getHistory()) {
            records.add(new HistoryRecord(id, name, period.getSchedule(), period.getStart(), period.getEnd(), period.getBaselineAt() == 0 ? null : period.getProgress(), period.getTarget(), period.getOutcome(), period.getBaselineAt(), period.getLastObservedAt(), period.getCompletedAt(), false));
        }
        for (GoalMetadata.Completion completion : metadata.getCompletions()) {
            records.add(new HistoryRecord(id, completion.getTitle(), RepeatSchedule.NONE, completion.getObservedAt(), completion.getObservedAt() + 1, completion.getTarget(), completion.getTarget(), "Lifetime target reached", 0, completion.getObservedAt(), completion.getObservedAt(), true));
        }
    }
}
