package com.grinder.content.goal.impl;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.TrackingState;
import com.grinder.repository.GrinderProfile;
import com.grinder.repository.GoalMetadata;
import com.grinder.repository.RecurringGoal;
import com.grinder.repository.RepeatSchedule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.runelite.api.Client;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class LinkedGoal implements Goal {
    private String id;
    private String title;
    private String description;
    private Mode mode;
    private List<String> sources;
    private long[] targets;
    private int required;
    private boolean[] achieved;
    private Map<RepeatSchedule, Long> completedThrough = new HashMap<>();
    private long completedPeriods;
    private transient long current;
    private transient boolean available;
    private transient String details = "Waiting for source goals.";

    public LinkedGoal(String title, String description, Mode mode, List<String> sources, long[] targets, int required) {
        this(UUID.randomUUID().toString(), title, description, mode, sources, targets, required);
    }

    public LinkedGoal(String id, String title, String description, Mode mode, List<String> sources, long[] targets, int required) {
        this.id = id;
        this.title = title == null ? "" : title.trim();
        this.description = description == null ? "" : description.trim();
        this.mode = mode;
        this.sources = new ArrayList<>(sources);
        this.targets = targets.clone();
        this.required = required;
        this.achieved = new boolean[sources.size()];
        validate();
    }

    public void validate() {
        UUID.fromString(id);
        if (title == null || title.trim().isEmpty() || title.length() > 80 || description == null || description.length() > 2000 || mode == null || sources == null || sources.isEmpty() || sources.size() > 100 || targets == null || targets.length == 0 || targets.length > 100 || required < 1 || required > 1_000_000 || achieved == null || achieved.length != sources.size() || completedThrough == null || completedThrough.size() > RepeatSchedule.values().length || completedPeriods < 0) {
            throw new IllegalArgumentException("Invalid linked goal settings.");
        }
        for (String source : sources) {
            UUID.fromString(source);
        }
        if (sources.stream().distinct().count() != sources.size()) {
            throw new IllegalArgumentException("Select each source only once.");
        }
        if (mode == Mode.MILESTONES || mode == Mode.REPEAT_COMPLETIONS) {
            if (sources.size() != 1) {
                throw new IllegalArgumentException("This goal needs exactly one source.");
            }
        } else if (targets.length != sources.size() || required > sources.size()) {
            throw new IllegalArgumentException("Choose a valid number of source goals.");
        }
        long previous = 0;
        for (long target : targets) {
            if (target < 1 || mode == Mode.MILESTONES && target <= previous) {
                throw new IllegalArgumentException("Use positive targets; milestones must be strictly increasing.");
            }
            previous = target;
        }
        for (Map.Entry<RepeatSchedule, Long> entry : completedThrough.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Invalid observed period watermark.");
            }
        }
    }

    public void update(GrinderProfile profile, boolean loggedIn) {
        Map<String, Goal> sourceIndex = new HashMap<>();
        for (Goal goal : profile.getGoals()) {
            if (!(goal instanceof LinkedGoal)) {
                sourceIndex.put(profile.metadata(goal).getId(), goal);
            }
        }
        update(profile, loggedIn, sourceIndex);
    }

    public void update(GrinderProfile profile, boolean loggedIn, Map<String, Goal> sourceIndex) {
        current = 0;
        available = true;
        StringBuilder text = new StringBuilder();
        int met = 0;
        boolean prerequisites = true;
        for (int i = 0; i < sources.size(); i++) {
            String sourceId = sources.get(i);
            Goal source = sourceIndex.get(sourceId);
            if (source == null) {
                available = false;
                text.append("Missing source · restore it or edit this goal.\n");
                continue;
            }
            GoalMetadata metadata = profile.metadata(source);
            if (mode == Mode.COLLECTION_SET && !(source instanceof com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal)) {
                available = false;
                text.append("Invalid collection source · choose individual collection items.\n");
                continue;
            }
            RecurringGoal recurring = metadata.recurring();
            boolean known = TrackingState.of(source, recurring, loggedIn, profile.getPlayerStorage().hasBankSnapshot()) != TrackingState.WAITING;
            long value = recurring.isActive() ? recurring.getProgress() : source.getProgressCurrent();
            long threshold = targets[Math.min(i, targets.length - 1)];
            boolean complete = known && value >= threshold;
            if (mode == Mode.ACHIEVED) {
                achieved[i] |= complete || metadata.getCompletions().stream().anyMatch(event -> event.getTarget() >= threshold);
                complete = achieved[i];
                known |= complete;
            }
            if (mode == Mode.REPEAT_COMPLETIONS) {
                recurring.getHistory().stream().filter(period -> period.getCompletedAt() > 0).sorted(java.util.Comparator.comparingLong(RecurringGoal.Period::getStart)).forEach(period -> observePeriod(period.getSchedule(), period.getStart()));
                if (recurring.getCompletedAt() > 0) {
                    observePeriod(recurring.getSchedule(), recurring.getStart());
                }
                current = completedPeriods;
                known = true;
            } else if (mode == Mode.MILESTONES) {
                for (long milestone : targets) {
                    if (known && value >= milestone) {
                        current++;
                    }
                }
                text.append("Current reading: ").append(value).append(". Milestones: ").append(Arrays.toString(targets)).append('\n');
            } else if (mode == Mode.DEPENDENCY && i > 0) {
                prerequisites &= complete;
            }
            if (complete) {
                met++;
            }
            available &= known;
            text.append(known ? complete ? "✓ " : "○ " : "? ").append(source.getTitle(null)).append(" · target ").append(threshold).append('\n');
        }
        if (mode != Mode.MILESTONES && mode != Mode.REPEAT_COMPLETIONS) {
            current = mode == Mode.DEPENDENCY ? prerequisites && met == sources.size() ? 1 : 0 : met;
        }
        if (mode == Mode.ANY_OF && met >= required) {
            available = true;
        }
        if (mode == Mode.DEPENDENCY && !prerequisites) {
            text.insert(0, "Prerequisites pending. This only gates the personal goal, not gameplay.\n");
        }
        details = text.toString().trim();
    }

    private void observePeriod(RepeatSchedule schedule, long start) {
        if (schedule != RepeatSchedule.NONE && start > completedThrough.getOrDefault(schedule, 0L)) {
            completedThrough.put(schedule, start);
            completedPeriods++;
        }
    }

    public LinkedGoal revise(String title, String description, Mode mode, List<String> sources, long[] targets, int required) {
        LinkedGoal result = new LinkedGoal(title, description, mode, sources, targets, required);
        if (this.mode == mode && this.sources.equals(sources) && Arrays.equals(this.targets, targets) && this.required == required) {
            result.id = id;
            result.achieved = achieved.clone();
            result.completedThrough.putAll(completedThrough);
            result.completedPeriods = completedPeriods;
        }
        return result;
    }

    public void remapSources(Map<String, String> mapping) {
        sources.replaceAll(source -> mapping.getOrDefault(source, source));
        validate();
    }

    public void clearObservations() {
        Arrays.fill(achieved, false);
        completedThrough.clear();
        completedPeriods = 0;
    }

    public String getId() {
        return id;
    }

    public Mode getMode() {
        return mode;
    }

    public List<String> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public long[] getTargets() {
        return targets.clone();
    }

    public int getRequired() {
        return required;
    }

    public String getNotes() {
        return description;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getTitle(Client client) {
        return title;
    }

    @Override
    public String getDescription() {
        return (description.isEmpty() ? "" : description + "\n") + mode + "\n" + (details == null ? "Waiting for source goals." : details);
    }

    @Override
    public GoalCategory getCategory() {
        return mode == Mode.COLLECTION_SET ? GoalCategory.COLLECTION_LOG : GoalCategory.OTHER;
    }

    @Override
    public boolean hasProgress() {
        return true;
    }

    @Override
    public long getProgressCurrent() {
        return current;
    }

    @Override
    public long getProgressMaximum() {
        return mode == Mode.MILESTONES ? targets.length : mode == Mode.ANY_OF || mode == Mode.REPEAT_COMPLETIONS ? required : mode == Mode.DEPENDENCY ? 1 : sources.size();
    }

    @Override
    public GoalStatus getStatus() {
        return available && current >= getProgressMaximum() ? GoalStatus.COMPLETE : current > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED;
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    public enum Mode {
        CHECKLIST("All-of checklist"),
        ANY_OF("Any-of goal"),
        MILESTONES("Milestone ladder"),
        COLLECTION_SET("Selected collection items"),
        ACHIEVED("Spending-safe achievements"),
        REPEAT_COMPLETIONS("Completed recurring periods"),
        DEPENDENCY("Goal with prerequisites");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
