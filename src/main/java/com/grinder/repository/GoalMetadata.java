package com.grinder.repository;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class GoalMetadata {
    private final String id;
    private boolean pinned;
    private Boolean collapsed;
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
    public boolean isPinned() {
        return pinned;
    }
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
    private final List<Completion> completions = new ArrayList<>();
    public GoalMetadata() {
        id = UUID.randomUUID().toString();
    }
    private GoalMetadata(GoalMetadata source) {
        id = source.id;
        pinned = source.pinned;
        collapsed = source.collapsed;
        recurring = source.recurring == null ? null : source.recurring.copy();
        completions.addAll(source.completions);
    }
    public GoalMetadata copy() {
        return new GoalMetadata(this);
    }
    public String getId() {
        return id;
    }
    public List<Completion> getCompletions() {
        return Collections.unmodifiableList(completions);
    }
    public boolean observe(Goal goal, String title, long now) {
        long target = goal.getProgressMaximum();
        if (goal.getStatus() != GoalStatus.COMPLETE || target <= 0 || now <= 0 || completions.stream().anyMatch(event -> event.target == target)) {
            return false;
        }
        completions.add(new Completion(title, target, now));
        return true;
    }
    public static GoalMetadata validated(GoalMetadata saved) {
        UUID.fromString(saved.id);
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
            text.append(event.title).append(" — target ").append(String.format(java.util.Locale.ROOT, "%,d", event.target)).append(" — ").append(java.time.Instant.ofEpochMilli(event.observedAt)).append('\n');
        }
        return text.toString();
    }
    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/18/2026
     */
    public static final class Completion {
        private final String title;
        private final long target;
        private final long observedAt;
        private Completion(String title, long target, long observedAt) {
            this.title = title;
            this.target = target;
            this.observedAt = observedAt;
        }
        public String getTitle() {
            return title;
        }
        public long getTarget() {
            return target;
        }
        public long getObservedAt() {
            return observedAt;
        }
    }
}
