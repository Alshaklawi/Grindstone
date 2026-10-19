package com.grindstone.ui.editor;

import com.grindstone.content.goal.impl.ChecklistGoal;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class ChecklistGoalForm extends JPanel {
    private final JTextField title = new JTextField(28);
    private final JTextArea description = new JTextArea(2, 28);
    private final JTextArea steps = new JTextArea(6, 28);
    private ChecklistGoal original;

    public ChecklistGoalForm() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        steps.setToolTipText("One distinct step per line, up to 100. Each step is checked manually and performs no game action.");
        add(field("Goal name", title));
        add(field("Description (optional)", new JScrollPane(description)));
        add(field("Checklist steps (one per line)", new JScrollPane(steps)));
    }

    private JPanel field(String title, JComponent control) {
        JPanel field = new JPanel(new BorderLayout(0, 4));
        field.setOpaque(false);
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JLabel label = new JLabel(title);
        label.setLabelFor(control);
        field.add(label, BorderLayout.NORTH);
        field.add(control, BorderLayout.CENTER);
        return field;
    }

    public void load(ChecklistGoal goal) {
        original = goal;
        title.setText(goal.getTitle(null));
        description.setText(goal.getDescription());
        steps.setText(String.join("\n", goal.getSteps()));
    }

    public ChecklistGoal read() {
        List<String> lines = Arrays.stream(steps.getText().split("\\R")).map(String::trim).filter(line -> !line.isEmpty()).collect(Collectors.toList());
        if (original != null && original.getSteps().equals(lines)) {
            return new ChecklistGoal(original.getId(), title.getText(), description.getText(), lines, original.getChecks());
        }
        return new ChecklistGoal(title.getText(), description.getText(), lines);
    }

    public long getStepCount() {
        return Math.max(1, Arrays.stream(steps.getText().split("\\R")).filter(line -> !line.trim().isEmpty()).count());
    }

    public void addStepsListener(Runnable listener) {
        steps.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                listener.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                listener.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                listener.run();
            }
        });
    }
}
