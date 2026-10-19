package com.grinder.ui.editor;

import com.grinder.content.goal.impl.LinkedGoal;
import com.grinder.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class LinkedGoalForm extends JPanel {
    private final List<Choice> choices;
    private final LinkedGoal original;
    private final JTextField title = new JTextField(30);
    private final JTextArea notes = new JTextArea(2, 30);
    private final JComboBox<LinkedGoal.Mode> mode = new JComboBox<>(LinkedGoal.Mode.values());
    private final JComboBox<Choice> main;
    private final JTextField search = new JTextField();
    private final JTextField milestones = new JTextField("50, 75, 99");
    private final JSpinner required = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JPanel mainField;
    private final JPanel milestonesField;
    private final JPanel requiredField;
    private final JPanel sourcesField;

    public LinkedGoalForm(List<Choice> choices, LinkedGoal original) {
        this.choices = new ArrayList<>(choices);
        this.original = original;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        main = new JComboBox<>(choices.toArray(new Choice[0]));
        javax.swing.DefaultListCellRenderer sourceRenderer = new javax.swing.DefaultListCellRenderer();
        sourceRenderer.putClientProperty("html.disable", Boolean.TRUE);
        main.setRenderer(sourceRenderer);
        model = new DefaultTableModel(new Object[]{"Use", "Source goal", "Required reading"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : column == 2 ? Long.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 1;
            }
        };
        for (Choice choice : choices) {
            model.addRow(new Object[]{false, choice.toString(), Math.max(1, choice.target)});
        }
        table = new JTable(model);
        javax.swing.table.DefaultTableCellRenderer textRenderer = new javax.swing.table.DefaultTableCellRenderer();
        textRenderer.putClientProperty("html.disable", Boolean.TRUE);
        table.setDefaultRenderer(String.class, textRenderer);
        table.setRowHeight(24);
        table.setFont(UiTypography.BODY);
        table.setToolTipText("Select sources and edit their required readings. Targets are captured here; editing the source later does not silently change this plan.");
        table.getColumnModel().getColumn(0).setMaxWidth(44);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(460, 210));
        JPanel sourceControls = new JPanel(new BorderLayout(0, 6));
        sourceControls.add(search, BorderLayout.NORTH);
        sourceControls.add(scroll, BorderLayout.CENTER);
        search.setToolTipText("Search source goals. Selections remain selected when filtered out.");
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        add(field("Goal name", title));
        add(field("Description (optional)", new JScrollPane(notes)));
        add(field("Goal type", mode));
        mainField = field("Main source (the goal gated by prerequisites, or ladder/count source)", main);
        milestonesField = field("Milestones, smallest first (comma separated)", milestones);
        requiredField = field("Required goals / completed periods", required);
        sourcesField = field("Sources / prerequisites", sourceControls);
        add(mainField);
        add(milestonesField);
        add(requiredField);
        add(sourcesField);
        JLabel hint = new JLabel("<html>Uses existing trackers only. Add missing source trackers first.<br>Linked goals cannot reference other linked goals, preventing cycles.<br>Collection sets accept individual collection-item goals only.<br>Repeat counts retain observed completed periods, not unobserved history.</html>");
        hint.setFont(UiTypography.META);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        add(hint);
        mode.addActionListener(event -> refreshFields());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filter();
            }
        });
        if (original != null) {
            title.setText(original.getTitle(null));
            notes.setText(original.getNotes());
            mode.setSelectedItem(original.getMode());
            required.setValue(original.getRequired());
            milestones.setText(Arrays.stream(original.getTargets()).mapToObj(Long::toString).collect(Collectors.joining(", ")));
            for (int i = 0; i < choices.size(); i++) {
                int index = original.getSources().indexOf(choices.get(i).id);
                if (index >= 0) {
                    model.setValueAt(true, i, 0);
                    model.setValueAt(original.getTargets()[Math.min(index, original.getTargets().length - 1)], i, 2);
                    if (index == 0) {
                        main.setSelectedIndex(i);
                    }
                }
            }
        }
        refreshFields();
    }

    private JPanel field(String text, javax.swing.JComponent control) {
        JPanel field = new JPanel(new BorderLayout(0, 4));
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(0, 0, 9, 0));
        JLabel label = new JLabel(text);
        label.setLabelFor(control);
        field.add(label, BorderLayout.NORTH);
        field.add(control, BorderLayout.CENTER);
        field.setAlignmentX(LEFT_ALIGNMENT);
        return field;
    }

    private void refreshFields() {
        LinkedGoal.Mode selected = (LinkedGoal.Mode) mode.getSelectedItem();
        boolean single = selected == LinkedGoal.Mode.MILESTONES || selected == LinkedGoal.Mode.REPEAT_COMPLETIONS;
        mainField.setVisible(single || selected == LinkedGoal.Mode.DEPENDENCY);
        milestonesField.setVisible(selected == LinkedGoal.Mode.MILESTONES);
        requiredField.setVisible(selected == LinkedGoal.Mode.ANY_OF || selected == LinkedGoal.Mode.REPEAT_COMPLETIONS);
        sourcesField.setVisible(!single);
        filter();
        revalidate();
        repaint();
    }

    private void filter() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Choice choice = choices.get(entry.getIdentifier());
                return choice.title.toLowerCase(Locale.ROOT).contains(query) && (mode.getSelectedItem() != LinkedGoal.Mode.COLLECTION_SET || choice.collectionItem);
            }
        });
    }

    public LinkedGoal read() throws java.text.ParseException {
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
            throw new IllegalArgumentException("Finish editing the required reading.");
        }
        required.commitEdit();
        LinkedGoal.Mode selected = (LinkedGoal.Mode) mode.getSelectedItem();
        List<String> ids = new ArrayList<>();
        List<Long> targets = new ArrayList<>();
        if (selected == LinkedGoal.Mode.MILESTONES || selected == LinkedGoal.Mode.REPEAT_COMPLETIONS || selected == LinkedGoal.Mode.DEPENDENCY) {
            Choice choice = (Choice) main.getSelectedItem();
            if (choice == null || selected == LinkedGoal.Mode.REPEAT_COMPLETIONS && !choice.recurring) {
                throw new IllegalArgumentException("Choose a source. Completion counts require an existing recurring goal.");
            }
            requireLoadedTarget(choice);
            ids.add(choice.id);
            targets.add(((Number) model.getValueAt(main.getSelectedIndex(), 2)).longValue());
        }
        if (selected != LinkedGoal.Mode.MILESTONES && selected != LinkedGoal.Mode.REPEAT_COMPLETIONS) {
            for (int i = 0; i < choices.size(); i++) {
                if (Boolean.TRUE.equals(model.getValueAt(i, 0)) && !ids.contains(choices.get(i).id)) {
                    requireLoadedTarget(choices.get(i));
                    if (selected == LinkedGoal.Mode.COLLECTION_SET && !choices.get(i).collectionItem) {
                        throw new IllegalArgumentException("Collection sets can reference only individual collection-item goals.");
                    }
                    ids.add(choices.get(i).id);
                    targets.add(((Number) model.getValueAt(i, 2)).longValue());
                }
            }
        }
        long[] values = selected == LinkedGoal.Mode.MILESTONES ? Arrays.stream(milestones.getText().split(",")).map(String::trim).mapToLong(Long::parseLong).toArray() : targets.stream().mapToLong(Long::longValue).toArray();
        int count = selected == LinkedGoal.Mode.ANY_OF || selected == LinkedGoal.Mode.REPEAT_COMPLETIONS ? ((Number) required.getValue()).intValue() : 1;
        return original == null ? new LinkedGoal(title.getText(), notes.getText(), selected, ids, values, count) : original.revise(title.getText(), notes.getText(), selected, ids, values, count);
    }

    private void requireLoadedTarget(Choice choice) {
        if (choice.target <= 0 && (original == null || !original.getSources().contains(choice.id))) {
            throw new IllegalArgumentException("The native target for " + choice.title + " is not loaded. Open its native interface and reopen this editor before adding it as a source.");
        }
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    public static final class Choice {
        private final String id;
        private final String title;
        private final long target;
        private final boolean collectionItem;
        private final boolean recurring;

        public Choice(String id, String title, long target, boolean collectionItem, boolean recurring) {
            this.id = id;
            this.title = title;
            this.target = target;
            this.collectionItem = collectionItem;
            this.recurring = recurring;
        }

        @Override
        public String toString() {
            return target > 0 ? title : title + " · target not loaded";
        }
    }
}
