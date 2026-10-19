package com.grindstone.repository;
import com.grindstone.content.goal.Goal;
import com.grindstone.content.goal.GoalCategory;
import com.grindstone.content.goal.GoalStatus;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class GoalMetadata {
    @Getter
    private final String id;
    @Getter
    @Setter
    private boolean pinned;
    private Boolean collapsed;
    @Getter
    private long deadlineAt;
    @Getter
    private boolean deadlineReminder;
    @Getter
    private long lastObservedAt;
    @Getter
    @Setter
    private GoalCategory displayCategory;
    private RecurringGoal recurring;
    public RecurringGoal recurring() {
        if (recurring == null) {
            recurring = new RecurringGoal();
        }
        return recurring;
    }
    public boolean isCollapsed(boolean defaultValue) {
        return collapsed == null ? defaultValue : collapsed;
    }
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }
    public void setDeadline(long deadlineAt, boolean reminder) {
        if (deadlineAt < 0 || deadlineAt > 253_370_764_800_000L) {
            throw new IllegalArgumentException("Invalid personal deadline.");
        }
        this.deadlineAt = deadlineAt;
        deadlineReminder = deadlineAt != 0 && reminder;
    }
    public String deadlineText(long now) {
        if (deadlineAt == 0) {
            return "";
        }
        String date = Instant.ofEpochMilli(deadlineAt - 1).atZone(ZoneOffset.UTC).toLocalDate().toString();
        return (now >= deadlineAt ? "Past due · " : "Due · ") + date + " UTC";
    }
    private final List<Completion> completions = new ArrayList<>();
    public GoalMetadata() {
        id = UUID.randomUUID().toString();
    }
    private GoalMetadata(GoalMetadata source) {
        id = source.id;
        pinned = source.pinned;
        collapsed = source.collapsed;
        deadlineAt = source.deadlineAt;
        deadlineReminder = source.deadlineReminder;
        lastObservedAt = source.lastObservedAt;
        displayCategory = source.displayCategory;
        recurring = source.recurring == null ? null : source.recurring.copy();
        completions.addAll(source.completions);
    }
    public GoalMetadata copy() {
        return new GoalMetadata(this);
    }
    public List<Completion> getCompletions() {
        return Collections.unmodifiableList(completions);
    }
    public boolean observe(Goal goal, String title, long now) {
        if (now > 0) {
            lastObservedAt = Math.max(lastObservedAt, now);
        }
        long target = goal.getProgressMaximum();
        if (goal.getStatus() != GoalStatus.COMPLETE || target <= 0 || now <= 0 || completions.stream().anyMatch(event -> event.target == target)) {
            return false;
        }
        completions.add(new Completion(title, target, now));
        return true;
    }
    public static GoalMetadata validated(GoalMetadata saved) {
        UUID.fromString(saved.id);
        saved.setDeadline(saved.deadlineAt, saved.deadlineReminder);
        if (saved.lastObservedAt < 0) {
            throw new IllegalArgumentException("Invalid last observation.");
        }
        if (saved.recurring != null) {
            saved.recurring.validate();
        }
        if (saved.completions == null || saved.completions.size() > 2000) {
            throw new IllegalArgumentException("Invalid completion history");
        }
        for (Completion event : saved.completions) {
            if (event == null || event.title == null || event.title.length() > 300 || event.target <= 0 || event.observedAt <= 0) {
                throw new IllegalArgumentException("Invalid completion observation");
            }
        }
        return saved;
    }
    public String historyText() {
        if (completions.isEmpty()) {
            return "No completed targets observed yet.";
        }
        StringBuilder text = new StringBuilder("First observed completed (not the historical achievement date):\n\n");
        for (Completion event : completions) {
            text.append(event.title).append(" — target ").append(String.format(Locale.ROOT, "%,d", event.target)).append(" — ").append(Instant.ofEpochMilli(event.observedAt)).append('\n');
        }
        return text.toString();
    }
    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/18/2026
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Completion {
        @Getter
        private final String title;
        @Getter
        private final long target;
        @Getter
        private final long observedAt;
    }
}
