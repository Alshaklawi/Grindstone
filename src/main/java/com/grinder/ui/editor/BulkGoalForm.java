package com.grinder.ui.editor;

import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.repository.BulkGoalAction;
import com.grinder.repository.RepeatSchedule;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
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
public final class BulkGoalForm extends JPanel {
    private final List<Row> rows;
    private final DefaultTableModel model;
    private final JTable table;
    private final JComboBox<BulkGoalAction> action = new JComboBox<>(BulkGoalAction.values());
    private final JComboBox<GoalCategory> category = new JComboBox<>(GoalCategory.values());
    private final JComboBox<RepeatSchedule> schedule = new JComboBox<>(RepeatSchedule.values());
    private final JSpinner periodTarget = new JSpinner(new SpinnerNumberModel(1L, 1L, (long) Integer.MAX_VALUE, 1L));
    private final DeadlineForm deadline = new DeadlineForm(0, false);
    private final JLabel count = new JLabel();
    private boolean changingSelection;

    public BulkGoalForm(List<Row> rows) {
        super(new BorderLayout(0, 10));
        this.rows = new ArrayList<>(rows);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        model = new DefaultTableModel(new Object[]{"Use", "Goal", "Category", "Repeat", "Example"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 || column == 4 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        for (Row row : rows) {
            model.addRow(new Object[]{false, row.title, row.category.getDisplayName(), row.schedule.toString(), row.sample});
        }
        table = new JTable(model);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setMaxWidth(44);
        table.getColumnModel().getColumn(4).setMaxWidth(66);
        table.setToolTipText("Tick goals to include them in one action. Hidden selections remain selected; check the selected count before applying.");
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        JTextField search = new JTextField();
        search.setToolTipText("Search names, categories and repeat schedules. This only filters the manager, not your side panel.");
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                String query = search.getText().trim().toLowerCase(Locale.ROOT);
                sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        return (entry.getStringValue(1) + " " + entry.getStringValue(2) + " " + entry.getStringValue(3)).toLowerCase(Locale.ROOT).contains(query);
                    }
                });
                updateCount();
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                changed();
            }
        });
        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(search, BorderLayout.NORTH);
        JPanel selection = new JPanel(new GridLayout(1, 3, 6, 0));
        JButton select = new JButton("Select visible");
        JButton clear = new JButton("Clear all selections");
        select.setToolTipText("Select only rows matching the manager's current search.");
        clear.setToolTipText("Clear every selected goal, including those hidden by the search.");
        select.addActionListener(event -> {
            changingSelection = true;
            for (int i = 0; i < table.getRowCount(); i++) {
                model.setValueAt(true, table.convertRowIndexToModel(i), 0);
            }
            changingSelection = false;
            updateCount();
        });
        clear.addActionListener(event -> {
            changingSelection = true;
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(false, i, 0);
            }
            changingSelection = false;
            updateCount();
        });
        selection.add(select);
        selection.add(clear);
        selection.add(count);
        top.add(selection, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(620, 300));
        add(scroll, BorderLayout.CENTER);
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(action);
        options.add(category);
        JPanel repeat = new JPanel(new GridLayout(1, 3, 6, 0));
        repeat.add(schedule);
        repeat.add(new JLabel("Period target"));
        repeat.add(periodTarget);
        periodTarget.setToolTipText("Shared period target for XP, activity and custom goals. Checklists always require all their steps. Unsupported or duplicate schedules reject the entire action.");
        options.add(repeat);
        options.add(deadline);
        Runnable visibility = () -> {
            category.setVisible(getAction() == BulkGoalAction.CATEGORY);
            repeat.setVisible(getAction() == BulkGoalAction.SCHEDULE);
            deadline.setVisible(getAction() == BulkGoalAction.DEADLINE);
            revalidate();
        };
        action.addActionListener(event -> visibility.run());
        visibility.run();
        add(options, BorderLayout.SOUTH);
        model.addTableModelListener(event -> {
            if (!changingSelection) {
                updateCount();
            }
        });
        updateCount();
    }

    private void updateCount() {
        count.setText(selectedGoals().size() + " selected / " + table.getRowCount() + " visible");
    }

    public List<Goal> selectedGoals() {
        List<Goal> result = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (Boolean.TRUE.equals(model.getValueAt(i, 0))) {
                result.add(rows.get(i).goal);
            }
        }
        return result;
    }

    public BulkGoalAction getAction() {
        return (BulkGoalAction) action.getSelectedItem();
    }

    public GoalCategory getCategory() {
        return (GoalCategory) category.getSelectedItem();
    }

    public RepeatSchedule getSchedule() {
        return (RepeatSchedule) schedule.getSelectedItem();
    }

    public long getPeriodTarget() throws java.text.ParseException {
        periodTarget.commitEdit();
        return ((Number) periodTarget.getValue()).longValue();
    }

    public long getDeadlineAt() {
        return getAction() == BulkGoalAction.DEADLINE ? deadline.getDeadlineAt() : 0;
    }

    public boolean isReminderEnabled() {
        return deadline.isReminderEnabled();
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    public static final class Row {
        private final Goal goal;
        private final String title;
        private final GoalCategory category;
        private final RepeatSchedule schedule;
        private final boolean sample;

        public Row(Goal goal, String title, GoalCategory category, RepeatSchedule schedule, boolean sample) {
            this.goal = goal;
            this.title = title;
            this.category = category;
            this.schedule = schedule;
            this.sample = sample;
        }
    }
}
