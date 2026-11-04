package com.grindstone.ui.editor;

import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.ui.components.UtcDateTimePicker;
import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class DeadlineForm extends JPanel {
    private final UtcDateTimePicker dateTime;
    private final JCheckBox reminder = new JCheckBox("Show a header reminder within 24 hours / when overdue");

    public DeadlineForm(long deadlineAt, boolean remind) {
        super(new BorderLayout(0, 8));
        add(new JLabel("Deadline (UTC)"), BorderLayout.NORTH);
        dateTime = new UtcDateTimePicker(deadlineAt);
        dateTime.setToolTipText("Choose a date, hour and minute. Clear removes the deadline. Missing it never deletes or resets progress.");
        add(dateTime, BorderLayout.CENTER);
        reminder.setSelected(remind);
        reminder.setOpaque(false);
        reminder.setToolTipText("Only a reminder in Grindstone's header while the profile is loaded. No external notification or game action.");
        add(reminder, BorderLayout.SOUTH);
    }

    public long getDeadlineAt() {
        long deadlineAt = dateTime.getEpochMillis();
        if (dateTime.isDateSelected() && (deadlineAt <= 0 || deadlineAt > MilestoneMetadata.MAX_DEADLINE_AT)) {
            throw new IllegalArgumentException("Choose a UTC deadline after 1 Jan 1970 and no later than 1 Jan 9999 at 00:00.");
        }
        return deadlineAt;
    }

    public boolean isReminderEnabled() {
        return dateTime.isDateSelected() && reminder.isSelected();
    }
}
