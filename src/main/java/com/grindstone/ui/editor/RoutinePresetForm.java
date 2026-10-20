package com.grindstone.ui.editor;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.tracker.TrackerEntry;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.repository.RepeatSchedule;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.*;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class RoutinePresetForm extends JPanel {
    private final JComboBox<RepeatSchedule> schedule = new JComboBox<>(new RepeatSchedule[]{RepeatSchedule.DAILY, RepeatSchedule.WEEKLY, RepeatSchedule.MONTHLY});
    private final List<Row> rows = new ArrayList<>();
    public RoutinePresetForm(boolean weekly) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        schedule.setSelectedItem(weekly ? RepeatSchedule.WEEKLY : RepeatSchedule.DAILY);
        schedule.setToolTipText("Choose the schedule for this routine. Native activity reset rules are not changed.");
        add(new JLabel("Preview your routine · choose milestones and edit targets"));
        add(Box.createVerticalStrut(8));
        add(schedule);
        add(Box.createVerticalStrut(8));
        if (weekly) {
            addRow(new TotalExperienceMilestone(), 1_000_000);
            addRow(new TrackerMilestone(TrackerEntry.WINTERTODT, 1), 10);
            addRow(new TrackerMilestone(TrackerEntry.CORPOREAL_BEAST, 1), 5);
        } else {
            addRow(new SkillExperienceMilestone(Skill.WOODCUTTING), 100_000);
            addRow(new SkillExperienceMilestone(Skill.FISHING), 50_000);
            addRow(new TrackerMilestone(TrackerEntry.WINTERTODT, 1), 3);
        }
        String id = UUID.nameUUIDFromBytes(("grindstone-routine-" + weekly).getBytes(StandardCharsets.UTF_8)).toString();
        addRow(new CustomMilestone(id, "Personal challenge", "Edit this routine to describe your own activity.", MilestoneCategory.OTHER, 0, 1), 1);
        JLabel note = new JLabel("<html>Existing tracker/schedule matches are skipped, not overwritten.<br>Targets count gains per UTC period. You can edit each milestone afterward.</html>");
        note.setFont(FontManager.getRunescapeSmallFont());
        add(Box.createVerticalStrut(8));
        add(note);
    }
    private void addRow(Milestone goal, long target) {
        Row row = new Row(goal, target);
        rows.add(row);
        add(row.panel);
        add(Box.createVerticalStrut(6));
    }
    public List<Milestone> selectedGoals() throws ParseException {
        List<Milestone> selected = new ArrayList<>();
        for (Row row : rows) {
            if (row.enabled.isSelected()) {
                row.target.commitEdit();
                if (row.goal instanceof CustomMilestone) {
                    row.goal.setTarget(((Number) row.target.getValue()).longValue());
                }
                selected.add(row.goal);
            }
        }
        return selected;
    }
    public MilestoneMetadata getMetadata(Milestone goal, long now) {
        Row row = rows.stream().filter(value -> value.goal == goal).findFirst().orElseThrow(IllegalArgumentException::new);
        MilestoneMetadata metadata = new MilestoneMetadata();
        metadata.getRecurring().configure((RepeatSchedule) schedule.getSelectedItem(), ((Number) row.target.getValue()).longValue(), now);
        return metadata;
    }
    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    private static final class Row {
        private final Milestone goal;
        private final JPanel panel = new JPanel(new BorderLayout(8, 0));
        private final JCheckBox enabled;
        private final JSpinner target;
        private Row(Milestone goal, long target) {
            this.goal = goal;
            enabled = new JCheckBox(goal.getTitle(null), true);
            enabled.setFont(FontManager.getRunescapeFont());
            enabled.setToolTipText("Include this milestone in the routine, or uncheck to skip it.");
            this.target = new JSpinner(new SpinnerNumberModel(target, 1L, (long) Integer.MAX_VALUE, 1L));
            this.target.setEditor(new JSpinner.NumberEditor(this.target, "#,##0"));
            this.target.setPreferredSize(new Dimension(120, 28));
            this.target.setToolTipText("Required progress per period. Edit this before adding the routine.");
            enabled.addActionListener(event -> this.target.setEnabled(enabled.isSelected()));
            panel.setOpaque(false);
            panel.add(enabled, BorderLayout.CENTER);
            panel.add(this.target, BorderLayout.EAST);
        }
    }
}
