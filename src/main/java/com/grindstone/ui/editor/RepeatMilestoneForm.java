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
public final class RepeatMilestoneForm extends JPanel {
    private final JComboBox<RepeatSchedule> schedule = new JComboBox<>(RepeatSchedule.values());
    private final JSpinner target = new JSpinner(new SpinnerNumberModel(1L, 1L, (long) Integer.MAX_VALUE, 1L));
    private Long fixedTarget;
    public RepeatMilestoneForm() {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 6));
        fields.setOpaque(false);
        fields.add(new JLabel("Schedule"));
        fields.add(schedule);
        fields.add(new JLabel("Target per period"));
        fields.add(target);
        target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
        schedule.setToolTipText("Choose one-time tracking or a routine with UTC resets.");
        target.setToolTipText("New XP, completions or manual progress required each period.");
        JLabel explanation = new JLabel("<html>UTC resets. Manual progress resets each period.<br>After an offline reset, tracking starts on login.<br>Changing the schedule or target starts a new period.</html>");
        explanation.setFont(FontManager.getRunescapeSmallFont());
        explanation.setVisible(false);
        add(fields, BorderLayout.NORTH);
        add(explanation, BorderLayout.CENTER);
        schedule.addActionListener(event -> {
            target.setEnabled(fixedTarget == null && getSchedule() != RepeatSchedule.NONE);
            explanation.setVisible(getSchedule() != RepeatSchedule.NONE);
        });
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
