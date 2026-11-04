package com.grindstone.ui;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.RecurringMilestone;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.util.DialogPlacement;
import com.grindstone.ui.util.UiTypography;
import java.awt.Component;
import java.util.function.Supplier;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public final class MilestoneActionSupport {
    @Getter
    private final Component owner;
    @Getter
    private final Client client;
    @Getter
    private final ClientThread clientThread;
    private final Supplier<GrindstoneProfile> profiles;
    private final Runnable refresh;

    public MilestoneActionSupport(Component owner, Client client, ClientThread clientThread, Supplier<GrindstoneProfile> profiles, Runnable refresh) {
        this.owner = owner;
        this.client = client;
        this.clientThread = clientThread;
        this.profiles = profiles;
        this.refresh = refresh;
    }

    public GrindstoneProfile getProfile() {
        return profiles.get();
    }

    public boolean isCurrentProfile(GrindstoneProfile profile) {
        return getProfile() == profile;
    }

    public boolean hasClientProfile(GrindstoneProfile profile) {
        return profile != null && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == profile.getHash();
    }

    public void render() {
        refresh.run();
    }

    public String getTitle(Milestone goal) {
        String title = goal.getTitle(client);
        return title == null ? "" : title;
    }

    public MilestoneCategory getCategory(Milestone goal) {
        GrindstoneProfile profile = getProfile();
        if (profile != null && profile.getMetadata(goal).getDisplayCategory() != null) {
            return profile.getMetadata(goal).getDisplayCategory();
        }
        MilestoneCategory category = goal.getCategory();
        return category == null ? MilestoneCategory.OTHER : category;
    }

    public void configureRepeat(GrindstoneProfile active, Milestone goal, RepeatSchedule schedule, long target) {
        if (!RecurringMilestone.supports(goal)) {
            return;
        }
        RecurringMilestone recurring = active.getMetadata(goal).getRecurring();
        if (goal instanceof ChecklistMilestone) {
            target = goal.getProgressMaximum();
            if (schedule != RepeatSchedule.NONE && (recurring.getSchedule() != schedule || recurring.getTarget() != target)) {
                ((ChecklistMilestone) goal).resetProgress();
            }
        }
        boolean newSchedule = recurring.getSchedule() != schedule;
        recurring.configure(schedule, target, System.currentTimeMillis());
        if (goal instanceof CustomMilestone && schedule != RepeatSchedule.NONE) {
            CustomMilestone custom = (CustomMilestone) goal;
            custom.update(custom.getTitle(null), custom.getDescription(), custom.getCategory(), newSchedule ? 0 : Math.min(custom.getProgressCurrent(), target), target);
        }
    }

    public JScrollPane createPreview(String summary) {
        JTextArea preview = new JTextArea(summary, 14, 44);
        preview.setEditable(false);
        preview.setFont(UiTypography.BODY);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setCaretPosition(0);
        return new JScrollPane(preview);
    }
}
