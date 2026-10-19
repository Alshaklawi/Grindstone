package com.grindstone.ui.editor;

import com.grindstone.repository.RepeatSchedule;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.ParseException;
import javax.swing.*;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class RepeatGoalForm extends JPanel {
    private final JComboBox<RepeatSchedule> schedule = new JComboBox<>(RepeatSchedule.values());
    private final JSpinner target = new JSpinner(new SpinnerNumberModel(1L, 1L, (long) Integer.MAX_VALUE, 1L));
    private Long fixedTarget;
    public RepeatGoalForm() {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 6));
        fields.setOpaque(false);
        fields.add(new JLabel("Repeat"));
        fields.add(schedule);
        fields.add(new JLabel("Target per period"));
        fields.add(target);
        target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
        schedule.setToolTipText("UTC resets: daily at midnight, weekly on Monday, monthly on the first. None restores lifetime progress.");
        target.setToolTipText("XP gained, activity completions, or manual progress required in each period. This does not change your lifetime target.");
        JLabel explanation = new JLabel("<html>UTC resets · XP and activities count gains.<br>Offline resets capture a new baseline on login.<br>Custom progress resets with each period or schedule change.<br>Changing the schedule or target archives the current period.</html>");
        explanation.setFont(FontManager.getRunescapeSmallFont());
        add(fields, BorderLayout.NORTH);
        add(explanation, BorderLayout.CENTER);
        schedule.addActionListener(event -> target.setEnabled(fixedTarget == null && getSchedule() != RepeatSchedule.NONE));
        target.setEnabled(false);
    }
    public void load(RepeatSchedule repeat, long target) {
        schedule.setSelectedItem(repeat);
        this.target.setValue(target);
    }
    public void setFixedTarget(Long fixedTarget) {
        this.fixedTarget = fixedTarget;
        if (fixedTarget != null) {
            target.setValue(fixedTarget);
        }
        target.setEnabled(fixedTarget == null && getSchedule() != RepeatSchedule.NONE);
    }
    public RepeatSchedule getSchedule() {
        return (RepeatSchedule) schedule.getSelectedItem();
    }
    public long getTarget() {
        return ((Number) target.getValue()).longValue();
    }
    public void commit() throws ParseException {
        target.commitEdit();
    }
    public void addScheduleListener(Runnable listener) {
        schedule.addActionListener(event -> listener.run());
    }
}
