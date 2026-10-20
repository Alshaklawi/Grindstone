package com.grindstone.content.milestone.impl;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.TrackingState;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogItemMilestone;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.RecurringMilestone;
import com.grindstone.repository.RepeatSchedule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import net.runelite.api.Client;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class LinkedMilestone implements Milestone {
    @Getter
    private String id;
    private String title;
    private String description;
    @Getter
    private Mode mode;
    private List<String> sources;
    private long[] targets;
    @Getter
    private int required;
    private boolean[] achieved;
    private Map<RepeatSchedule, Long> completedThrough = new HashMap<>();
    private long completedPeriods;
    private transient long current;
    @Getter
    private transient boolean available;
    private transient String details = "Waiting for source milestones.";
    private transient boolean started;

    public LinkedMilestone(String title, String description, Mode mode, List<String> sources, long[] targets, int required) {
        this(UUID.randomUUID().toString(), title, description, mode, sources, targets, required);
    }

    public LinkedMilestone(String id, String title, String description, Mode mode, List<String> sources, long[] targets, int required) {
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
            throw new IllegalArgumentException("Invalid linked milestone settings.");
        }
        for (String source : sources) {
            UUID.fromString(source);
        }
        if (sources.stream().distinct().count() != sources.size()) {
            throw new IllegalArgumentException("Select each source only once.");
        }
        if (mode == Mode.MILESTONES || mode == Mode.REPEAT_COMPLETIONS) {
            if (sources.size() != 1) {
                throw new IllegalArgumentException("This milestone needs exactly one source.");
            }
        } else if (targets.length != sources.size() || required > sources.size()) {
            throw new IllegalArgumentException("Choose a valid number of source milestones.");
        }
        if (mode == Mode.REPEAT_COMPLETIONS && targets.length != 1 || mode == Mode.DEPENDENCY && sources.size() < 2) {
            throw new IllegalArgumentException("Choose one repeat source, or a main milestone plus at least one prerequisite.");
        }
        long previous = 0;
        for (long target : targets) {
            if (target < 1 || mode == Mode.MILESTONES && target <= previous || mode == Mode.COLLECTION_SET && target != 1) {
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

    public void update(GrindstoneProfile profile, boolean loggedIn) {
        Map<String, Milestone> sourceIndex = new HashMap<>();
        for (Milestone goal : profile.getGoals()) {
            if (!(goal instanceof LinkedMilestone)) {
                sourceIndex.put(profile.getMetadata(goal).getId(), goal);
            }
        }
        update(profile, loggedIn, sourceIndex);
    }

    public void update(GrindstoneProfile profile, boolean loggedIn, Map<String, Milestone> sourceIndex) {
        current = 0;
        started = false;
        available = true;
        StringBuilder text = new StringBuilder();
        int met = 0;
        boolean prerequisites = true;
        for (int i = 0; i < sources.size(); i++) {
            String sourceId = sources.get(i);
            Milestone source = sourceIndex.get(sourceId);
            if (source == null) {
                available = false;
                text.append("Missing source · restore it or edit this milestone.\n");
                continue;
            }
            MilestoneMetadata metadata = profile.getMetadata(source);
            if (mode == Mode.COLLECTION_SET && !(source instanceof CollectionLogItemMilestone)) {
                available = false;
                text.append("Invalid collection source · choose individual collection items.\n");
                continue;
            }
            RecurringMilestone recurring = metadata.getRecurring();
            boolean known = TrackingState.of(source, recurring, loggedIn, profile.getPlayerStorage().hasBankSnapshot()) != TrackingState.WAITING;
            long value = recurring.isActive() ? recurring.getProgress() : source.getProgressCurrent();
            started |= known && value > 0;
            long threshold = targets[Math.min(i, targets.length - 1)];
            boolean complete = known && value >= threshold;
            if (mode == Mode.ACHIEVED) {
                achieved[i] |= complete || metadata.getCompletions().stream().anyMatch(event -> event.getTarget() >= threshold);
                complete = achieved[i];
                known |= complete;
            }
            if (mode == Mode.REPEAT_COMPLETIONS) {
                recurring.getHistory().stream().filter(period -> period.getCompletedAt() > 0).sorted(Comparator.comparingLong(RecurringMilestone.Period::getStart)).forEach(period -> observePeriod(period.getSchedule(), period.getStart()));
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
            text.append(known ? complete ? "[Done] " : "[Pending] " : "[Waiting] ").append(source.getTitle(null)).append(known ? " · observed " + value : " · unavailable").append(" · target ").append(threshold).append('\n');
        }
        if (mode != Mode.MILESTONES && mode != Mode.REPEAT_COMPLETIONS) {
            current = mode == Mode.DEPENDENCY ? prerequisites && met == sources.size() ? 1 : 0 : met;
        }
        if (mode == Mode.ANY_OF && met >= required) {
            available = true;
        }
        if (mode == Mode.DEPENDENCY && !prerequisites) {
            text.insert(0, "Prerequisites pending. This only gates the personal milestone, not gameplay.\n");
        }
        details = text.toString().trim();
    }

    private void observePeriod(RepeatSchedule schedule, long start) {
        if (schedule != RepeatSchedule.NONE && start > completedThrough.getOrDefault(schedule, 0L)) {
            completedThrough.put(schedule, start);
            completedPeriods++;
        }
    }

    public LinkedMilestone revise(String title, String description, Mode mode, List<String> sources, long[] targets, int required) {
        LinkedMilestone result = new LinkedMilestone(title, description, mode, sources, targets, required);
        boolean same = this.mode == mode && this.required == required && this.sources.size() == sources.size();
        if (mode == Mode.MILESTONES || mode == Mode.REPEAT_COMPLETIONS) {
            same &= this.sources.equals(sources) && Arrays.equals(this.targets, targets);
        } else {
            if (mode == Mode.DEPENDENCY) {
                same &= this.sources.get(0).equals(sources.get(0));
            }
            for (int i = 0; i < sources.size(); i++) {
                int original = this.sources.indexOf(sources.get(i));
                same &= original >= 0 && original < this.targets.length && this.targets[original] == targets[i];
            }
        }
        if (same) {
            result.id = id;
            for (int i = 0; i < sources.size(); i++) {
                result.achieved[i] = achieved[this.sources.indexOf(sources.get(i))];
            }
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



    public List<String> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public long[] getTargets() {
        return targets.clone();
    }


    public String getNotes() {
        return description;
    }


    @Override
    public String getTitle(Client client) {
        return title;
    }

    @Override
    public String getDescription() {
        return (description.isEmpty() ? "" : description + "\n") + mode + "\n" + (details == null ? "Waiting for source milestones." : details);
    }

    @Override
    public MilestoneCategory getCategory() {
        return mode == Mode.COLLECTION_SET ? MilestoneCategory.COLLECTION_LOG : MilestoneCategory.OTHER;
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
    public MilestoneStatus getStatus() {
        return available && current >= getProgressMaximum() ? MilestoneStatus.COMPLETE : current > 0 || started && mode != Mode.DEPENDENCY ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    public enum Mode {
        CHECKLIST("All-of checklist"),
        ANY_OF("Any-of milestone"),
        MILESTONES("Milestone ladder"),
        COLLECTION_SET("Selected collection items"),
        ACHIEVED("Spending-safe achievements"),
        REPEAT_COMPLETIONS("Completed recurring periods"),
        DEPENDENCY("Milestone with prerequisites");

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
