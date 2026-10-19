package com.grindstone.notification;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.goal.Goal;
import com.grindstone.content.goal.GoalStatus;
import com.grindstone.content.goal.TrackingState;
import com.grindstone.repository.GoalMetadata;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.RecurringGoal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.util.Text;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
@Singleton
public final class GoalCompletionNotifications {
    private final CompletionTransitions transitions = new CompletionTransitions();
    @Inject
    private GrindstoneConfig config;
    @Inject
    private Client client;
    @Inject
    private GoalCompletionOverlay overlay;
    @Inject
    private ChatMessageManager chat;
    @Inject
    private Notifier notifier;
    private GrindstoneProfile profile;

    public synchronized void reset() {
        profile = null;
        transitions.clear();
        overlay.clear();
    }

    public synchronized void observe(GrindstoneProfile active) {
        if (profile != active) {
            reset();
            profile = active;
        }
        Set<String> ids = new HashSet<>();
        List<String> completed = new ArrayList<>();
        int routineCompletions = 0;
        for (Goal goal : active.getGoals()) {
            GoalMetadata metadata = active.metadata(goal);
            RecurringGoal recurring = metadata.recurring();
            if (TrackingState.of(goal, recurring, true, active.getPlayerStorage().hasBankSnapshot()) == TrackingState.WAITING) {
                continue;
            }
            String id = metadata.getId();
            ids.add(id);
            boolean repeating = recurring.isActive() && RecurringGoal.supports(goal);
            long target = repeating ? recurring.getTarget() : goal.getProgressMaximum();
            String cycle = target + ":" + (repeating ? recurring.getSchedule() + ":" + recurring.getStart() : "once");
            boolean complete = (repeating ? recurring.getStatus() : goal.getStatus()) == GoalStatus.COMPLETE;
            if (transitions.observe(id, cycle, complete)) {
                String title = Text.removeTags(goal.getTitle(client)).replaceAll("[\\r\\n\\t]", " ");
                if (title.trim().isEmpty()) {
                    title = "Personal milestone";
                }
                if (title.length() > 100) {
                    title = title.substring(0, 97) + "...";
                }
                completed.add((repeating ? recurring.getSchedule() + ": " : "") + title);
                if (repeating) {
                    routineCompletions++;
                }
            }
        }
        transitions.retain(ids);
        if (completed.isEmpty()) {
            return;
        }
        if (config.completionPopup()) {
            overlay.show(completed, routineCompletions == completed.size());
        }
        boolean routinesOnly = routineCompletions == completed.size();
        String message = completed.size() == 1 ? (routinesOnly ? "Routine complete: " : "Milestone complete: ") + completed.get(0) : completed.size() + (routinesOnly ? " routines complete: " : " targets reached: ") + String.join(", ", completed.subList(0, Math.min(3, completed.size()))) + (completed.size() > 3 ? " and " + (completed.size() - 3) + " more" : "");
        if (config.completionChat()) {
            chat.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).value("[Grindstone] " + message).build());
        }
        if (config.completionNotification().isEnabled()) {
            notifier.notify(config.completionNotification(), message);
        }
    }
}
