package com.grindstone.notification;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.TrackingState;
import com.grindstone.overlay.OverlayStyle;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.RecurringMilestone;
import com.grindstone.repository.RepeatSchedule;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
 * @since 10/20/2026
 */
@Singleton
public final class RoutineReminders {
    @Inject
    private GrindstoneConfig config;
    @Inject
    private Client client;
    @Inject
    private ChatMessageManager chat;
    @Inject
    private Notifier notifier;
    private final Map<RepeatSchedule, Long> lastReminded = new EnumMap<>(RepeatSchedule.class);
    private final Map<RepeatSchedule, Long> periods = new EnumMap<>(RepeatSchedule.class);
    private long account;

    public void clear() {
        lastReminded.clear();
        periods.clear();
        account = 0;
    }

    public void observe(GrindstoneProfile profile) {
        if (!config.routineReminders() || (!config.routineReminderChat() && !config.routineReminderNotification().isEnabled())) {
            clear();
            return;
        }
        if (account != profile.getHash()) {
            clear();
            account = profile.getHash();
        }
        long now = System.currentTimeMillis();
        long interval = Math.max(5, Math.min(240, config.routineReminderMinutes())) * 60_000L;
        for (RepeatSchedule schedule : RepeatSchedule.values()) {
            if (!enabled(schedule)) {
                lastReminded.remove(schedule);
                periods.remove(schedule);
                continue;
            }
            long start = schedule.start(now);
            Long previousPeriod = periods.put(schedule, start);
            if (previousPeriod == null || previousPeriod != start) {
                boolean remindNow = previousPeriod == null ? config.routineReminderOnLogin() : config.routineReminderOnReset();
                lastReminded.put(schedule, remindNow ? 0L : now);
            }
            if (now - lastReminded.getOrDefault(schedule, now) < interval) {
                continue;
            }
            List<String> pending = new ArrayList<>();
            for (Milestone goal : profile.getGoals()) {
                RecurringMilestone routine = profile.metadata(goal).recurring();
                if (routine.getSchedule() != schedule || !RecurringMilestone.supports(goal) || routine.getStatus() == MilestoneStatus.COMPLETE || TrackingState.of(goal, routine, true, profile.getPlayerStorage().hasBankSnapshot()) == TrackingState.WAITING) {
                    continue;
                }
                pending.add(NotificationMessages.title(goal.getTitle(client)) + (config.routineReminderNumbers() ? NotificationMessages.progress(goal, routine.getProgress(), routine.getTarget()) : ""));
            }
            if (pending.isEmpty()) {
                continue;
            }
            lastReminded.put(schedule, now);
            String heading = schedule + " reminder";
            String detail = pending.size() + (pending.size() == 1 ? " routine left: " : " routines left: ") + String.join("; ", pending.subList(0, Math.min(3, pending.size()))) + (pending.size() > 3 ? "; and " + (pending.size() - 3) + " more" : "") + "." + (config.routineReminderCountdown() ? " Resets in " + OverlayStyle.countdown(schedule.next(start), now) + "." : "");
            if (config.routineReminderChat()) {
                chat.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).value(NotificationMessages.chat(heading, detail, config.notificationColours(), false)).build());
            }
            if (config.routineReminderNotification().isEnabled()) {
                notifier.notify(config.routineReminderNotification(), heading + ": " + detail);
            }
        }
    }

    private boolean enabled(RepeatSchedule schedule) {
        switch (schedule) {
            case DAILY:
                return config.dailyReminders();
            case WEEKLY:
                return config.weeklyReminders();
            case MONTHLY:
                return config.monthlyReminders();
            default:
                return false;
        }
    }
}
