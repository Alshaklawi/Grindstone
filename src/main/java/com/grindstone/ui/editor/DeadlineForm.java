package com.grindstone.ui.editor;

import com.grindstone.ui.components.UtcDatePicker;
import java.awt.BorderLayout;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class DeadlineForm extends JPanel {
    private final UtcDatePicker date;
    private final JCheckBox reminder = new JCheckBox("Show a header reminder within 24 hours / when overdue");

    public DeadlineForm(long deadlineAt, boolean remind) {
        super(new BorderLayout(0, 8));
        add(new JLabel("Due by the end of this UTC date (Clear removes it)"), BorderLayout.NORTH);
        date = new UtcDatePicker(deadlineAt == 0 ? null : Instant.ofEpochMilli(deadlineAt - 1).atZone(ZoneOffset.UTC).toLocalDate());
        date.setToolTipText("This is a personal deadline, not a game reset. Missing it never deletes or resets progress.");
        add(date, BorderLayout.CENTER);
        reminder.setSelected(remind);
        reminder.setOpaque(false);
        reminder.setToolTipText("Only a reminder in Grindstone's header while the profile is loaded. No external notification or game action.");
        add(reminder, BorderLayout.SOUTH);
    }

    public long getDeadlineAt() {
        return date.getDate() == null ? 0 : date.getDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public boolean isReminderEnabled() {
        return date.getDate() != null && reminder.isSelected();
    }

}
