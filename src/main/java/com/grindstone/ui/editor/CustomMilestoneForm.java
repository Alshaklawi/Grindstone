package com.grindstone.ui.editor;

import com.grindstone.ui.util.UiTooltips;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.CustomMilestone;
import java.awt.Component;
import java.awt.Dimension;
import java.text.ParseException;
import javax.swing.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CustomMilestoneForm extends JPanel {
    private final JTextField title = new JTextField(24);
    private final JTextArea description = new JTextArea(4, 24);
    private final JComboBox<MilestoneCategory> category = new JComboBox<>(MilestoneCategory.values());
    private final JSpinner current = new JSpinner(new SpinnerNumberModel(0L, 0L, (long) Integer.MAX_VALUE, 1L));
    private final JSpinner target = new JSpinner(new SpinnerNumberModel(1L, 1L, (long) Integer.MAX_VALUE, 1L));
    private JLabel targetLabel;

    public void setRecurring(boolean recurring) {
        target.setVisible(!recurring);
        targetLabel.setVisible(!recurring);
        revalidate();
    }

    public CustomMilestoneForm() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JScrollPane notes = new JScrollPane(description);
        notes.setPreferredSize(new Dimension(290, 100));
        category.setSelectedItem(MilestoneCategory.OTHER);
        category.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof MilestoneCategory) {
                    label.setText(((MilestoneCategory) value).getDisplayName());
                }
                return label;
            }
        });
        current.setEditor(new JSpinner.NumberEditor(current, "#,##0"));
        target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
        UiTooltips.set(title, "A short name for this personal milestone (1 to 80 characters).");
        UiTooltips.set(description, "Optional notes, milestones or a plan (up to 2,000 characters). Descriptions are searchable.");
        UiTooltips.set(category, "Choose where the milestone appears in the sidebar. This does not change how progress is tracked.");
        UiTooltips.set(current, "Enter your manual progress, from zero to the target. Equal to the target marks this milestone complete.");
        UiTooltips.set(target, "Set a positive target. Use 1 for a checklist milestone, then change progress to 1 when finished.");
        addField("Title (required, up to 80 characters)", title);
        addField("Description (optional, up to 2,000 characters)", notes);
        addField("Category", category);
        addField("Current progress", current);
        targetLabel = addField("Target (use 1 for a simple checklist milestone)", target);
    }

    private JLabel addField(String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        label.setToolTipText(field.getToolTipText());
        label.setAlignmentX(LEFT_ALIGNMENT);
        field.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
        add(Box.createVerticalStrut(4));
        add(field);
        add(Box.createVerticalStrut(8));
        return label;
    }

    public void load(CustomMilestone goal) {
        title.setText(goal.getTitle(null));
        description.setText(goal.getDescription());
        category.setSelectedItem(goal.getCategory());
        current.setValue(goal.getProgressCurrent());
        target.setValue(goal.getProgressMaximum());
    }

    public CustomMilestone read() throws ParseException {
        return read(0);
    }

    public CustomMilestone read(long periodTarget) throws ParseException {
        current.commitEdit();
        if (periodTarget == 0) {
            target.commitEdit();
        }
        return new CustomMilestone(title.getText(), description.getText(), (MilestoneCategory) category.getSelectedItem(), ((Number) current.getValue()).longValue(), periodTarget > 0 ? periodTarget : ((Number) target.getValue()).longValue());
    }
}
