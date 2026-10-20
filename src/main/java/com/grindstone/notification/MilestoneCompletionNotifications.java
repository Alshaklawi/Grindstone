package com.grindstone.notification;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.TrackingState;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.RecurringMilestone;
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

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
@Singleton
public final class MilestoneCompletionNotifications {
    private final CompletionTransitions transitions = new CompletionTransitions();
    @Inject
    private GrindstoneConfig config;
    @Inject
    private Client client;
    @Inject
    private MilestoneCompletionOverlay overlay;
    @Inject
    private ChatMessageManager chat;
    @Inject
    private Notifier notifier;
    @Inject
    private RoutineReminders reminders;
    private GrindstoneProfile profile;

    public synchronized void reset() {
        profile = null;
        transitions.clear();
        overlay.clear();
    }

    public synchronized void stop() {
        reset();
        reminders.clear();
    }

    public synchronized void observe(GrindstoneProfile active) {
        if (profile != active) {
            reset();
            profile = active;
        }
        reminders.observe(active);
        Set<String> ids = new HashSet<>();
        List<String> completed = new ArrayList<>();
        List<String> details = new ArrayList<>();
        List<String> headings = new ArrayList<>();
        int routineCompletions = 0;
        for (Milestone goal : active.getGoals()) {
            MilestoneMetadata metadata = active.metadata(goal);
            RecurringMilestone recurring = metadata.recurring();
            String id = metadata.getId();
            ids.add(id);
            if (TrackingState.of(goal, recurring, true, active.getPlayerStorage().hasBankSnapshot()) == TrackingState.WAITING) {
                continue;
            }
            boolean repeating = recurring.isActive() && RecurringMilestone.supports(goal);
            long target = repeating ? recurring.getTarget() : goal.getProgressMaximum();
            String cycle = target + ":" + (repeating ? recurring.getSchedule() + ":" + recurring.getStart() : "once");
            boolean complete = (repeating ? recurring.getStatus() : goal.getStatus()) == MilestoneStatus.COMPLETE;
            if (transitions.observe(id, cycle, complete)) {
                String title = NotificationMessages.title(goal.getTitle(client));
                String label = (repeating ? recurring.getSchedule() + ": " : "") + title;
                completed.add(label);
                headings.add(repeating ? "Routine complete" : "Milestone complete");
                details.add(label + (config.completionNumbers() ? NotificationMessages.progress(goal, repeating ? recurring.getProgress() : goal.getProgressCurrent(), target) : ""));
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
        String heading = completed.size() == 1 ? (routinesOnly ? "Routine complete" : "Milestone complete") : completed.size() + (routinesOnly ? " routines complete" : " targets reached");
        String detail = String.join("; ", details.subList(0, Math.min(3, details.size()))) + (details.size() > 3 ? "; and " + (details.size() - 3) + " more" : "");
        String message = heading + ": " + detail;
        if (config.completionChat()) {
            for (int index = 0; index < Math.min(5, details.size()); index++) {
                chat.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).value(NotificationMessages.chat(headings.get(index), details.get(index), config.notificationColours(), true)).build());
            }
            if (details.size() > 5) {
                chat.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).value(NotificationMessages.chat("More targets reached", (details.size() - 5) + " more complete. Open History in Grindstone to view them.", config.notificationColours(), true)).build());
            }
        }
        if (config.completionNotification().isEnabled()) {
            notifier.notify(config.completionNotification(), message);
        }
    }
}
