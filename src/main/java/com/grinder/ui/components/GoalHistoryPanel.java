package com.grinder.ui.components;

import com.grinder.repository.GoalMetadata;
import com.grinder.repository.RecurringGoal;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class GoalHistoryPanel extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    public static java.util.function.Supplier<GoalHistoryPanel> snapshot(GoalMetadata metadata) {
        java.util.List<Object[]> periods = new java.util.ArrayList<>();
        java.util.List<Object[]> milestones = new java.util.ArrayList<>();
        RecurringGoal recurring = metadata.recurring();
        if (recurring.isActive()) {
            periods.add(new Object[]{recurring.getSchedule(), date(recurring.getStart()), recurring.getBaselineAt() == 0 ? "—" : date(recurring.getSchedule().next(recurring.getStart())), recurring.getProgress(), recurring.getTarget(), recurring.getBaselineAt() == 0 ? "Waiting for login" : recurring.getCompletedAt() > 0 ? "Completed · current period" : "Current period", date(recurring.getBaselineAt()), date(recurring.getLastObservedAt()), date(recurring.getCompletedAt())});
        }
        java.util.List<RecurringGoal.Period> history = recurring.getHistory();
        for (int index = history.size() - 1; index >= 0; index--) {
            RecurringGoal.Period period = history.get(index);
            periods.add(new Object[]{period.getSchedule(), date(period.getStart()), date(period.getEnd()), period.getBaselineAt() == 0 ? null : period.getProgress(), period.getTarget(), period.getOutcome(), date(period.getBaselineAt()), date(period.getLastObservedAt()), date(period.getCompletedAt())});
        }
        java.util.List<GoalMetadata.Completion> completions = metadata.getCompletions();
        for (int index = completions.size() - 1; index >= 0; index--) {
            GoalMetadata.Completion event = completions.get(index);
            milestones.add(new Object[]{event.getTitle(), event.getTarget(), date(event.getObservedAt())});
        }
        boolean showPeriods = recurring.isActive() || !history.isEmpty();
        return () -> new GoalHistoryPanel(periods, milestones, showPeriods);
    }
    private GoalHistoryPanel(java.util.List<Object[]> periodRows, java.util.List<Object[]> milestoneRows, boolean showPeriods) {
        super(new BorderLayout(0, 10));
        JTabbedPane tabs = new JTabbedPane();
        DefaultTableModel periods = model("Schedule", "Period start", "Period end", "Progress", "Target", "Outcome", "Baseline captured", "Last observed", "Completed at");
        periodRows.forEach(periods::addRow);
        DefaultTableModel milestones = model("Goal", "Target", "First observed completed");
        milestoneRows.forEach(milestones::addRow);
        tabs.addTab("Periods (" + periodRows.size() + ")", table(periods));
        tabs.addTab("Lifetime targets (" + milestoneRows.size() + ")", table(milestones));
        if (!showPeriods) { tabs.setSelectedIndex(1); }
        JLabel note = new JLabel("<html>Times are UTC. Progress starts from the captured baseline, not an assumed offline reset value.<br>Incomplete values are last observed; unobserved gaps are not counted as misses. Latest 500 period records retained.<br>Completion dates are first observed, not historical achievement dates.</html>");
        note.setFont(FontManager.getRunescapeSmallFont());
        add(note, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(700, 340));
    }
    private static String date(long value) { return value <= 0 ? "—" : DATE.format(Instant.ofEpochMilli(value)); }
    private static DefaultTableModel model(String... columns) { return new DefaultTableModel(columns, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) {
            for (int row = 0; row < getRowCount(); row++) { if (getValueAt(row, column) instanceof Long) { return Long.class; } }
            return Object.class;
        }
    }; }
    private static JScrollPane table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FontManager.getRunescapeFont());
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(Long.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) { setHorizontalAlignment(SwingConstants.RIGHT); setText(value == null ? "—" : String.format(java.util.Locale.ROOT, "%,d", value)); }
        });
        table.getTableHeader().setReorderingAllowed(false);
        for (int column = 0; column < model.getColumnCount(); column++) { table.getColumnModel().getColumn(column).setPreferredWidth(model.getColumnClass(column) == Long.class ? 95 : 175); }
        table.setToolTipText("Scroll horizontally for observation dates. Click a heading to sort; rows cannot be edited.");
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }
}
