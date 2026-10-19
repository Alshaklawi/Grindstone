package com.grinder.ui.editor;

import java.awt.BorderLayout;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class DeadlineForm extends JPanel {
    private final JTextField date = new JTextField(20);
    private final JCheckBox reminder = new JCheckBox("Show a header reminder within 24 hours / when overdue");

    public DeadlineForm(long deadlineAt, boolean remind) {
        super(new BorderLayout(0, 8));
        add(new JLabel("Due by the end of this UTC date (YYYY-MM-DD; blank removes it)"), BorderLayout.NORTH);
        date.setText(deadlineAt == 0 ? "" : Instant.ofEpochMilli(deadlineAt - 1).atZone(ZoneOffset.UTC).toLocalDate().toString());
        date.setToolTipText("This is a personal deadline, not a game reset. Missing it never deletes or resets progress.");
        add(date, BorderLayout.CENTER);
        reminder.setSelected(remind);
        reminder.setOpaque(false);
        reminder.setToolTipText("Only a reminder in Grinder's header while the profile is loaded. No external notification or game action.");
        add(reminder, BorderLayout.SOUTH);
    }

    public long getDeadlineAt() {
        return parseDate(date.getText());
    }

    public boolean isReminderEnabled() {
        return reminder.isSelected();
    }

    public static long parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        try {
            LocalDate due = LocalDate.parse(text.trim());
            if (due.getYear() < 1970 || due.getYear() > 9998) {
                throw new IllegalArgumentException("Choose a UTC date from 1970 to 9998.");
            }
            return due.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (java.time.DateTimeException failure) {
            throw new IllegalArgumentException("Enter a valid date as YYYY-MM-DD, or leave it blank.", failure);
        }
    }
}
