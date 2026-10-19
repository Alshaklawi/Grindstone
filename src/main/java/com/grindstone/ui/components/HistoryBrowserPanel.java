package com.grindstone.ui.components;

import com.grindstone.repository.HistoryRecord;
import com.grindstone.repository.RepeatSchedule;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class HistoryBrowserPanel extends JPanel {
    private final List<HistoryRecord> records;
    private final List<HistoryRecord> shown = new ArrayList<>();
    private final JTextField search = new JTextField();
    private final JTextField from = new JTextField();
    private final JTextField to = new JTextField();
    private final JComboBox<String> schedule = new JComboBox<>(new String[]{"All schedules", "Lifetime", "Daily", "Weekly", "Monthly"});
    private final JLabel summary = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Milestone", "Schedule", "Period start", "Period end", "Progress", "Target", "Outcome", "Baseline", "Last observed", "Completed"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
        @Override
        public Class<?> getColumnClass(int column) {
            return column == 4 || column == 5 ? Long.class : String.class;
        }
    };
    private final JTable table = new JTable(model);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    public HistoryBrowserPanel(List<HistoryRecord> records, Consumer<String> reveal) {
        super(new BorderLayout(0, 10));
        this.records = new ArrayList<>(records);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel filters = new JPanel(new GridLayout(2, 4, 8, 4));
        for (String label : new String[]{"Search milestones / outcomes", "Schedule", "From (yyyy-mm-dd UTC)", "Through (yyyy-mm-dd UTC)"}) {
            filters.add(new JLabel(label));
        }
        filters.add(search);
        filters.add(schedule);
        filters.add(from);
        filters.add(to);
        search.setToolTipText("Filter milestone titles and period outcomes.");
        from.setToolTipText("Optional inclusive UTC date; periods overlapping this range are included.");
        to.setToolTipText("Optional inclusive UTC date. Leave blank for no upper date limit.");
        table.setFont(FontManager.getRunescapeFont());
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (int column = 0; column < model.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(column == 0 ? 230 : column == 4 || column == 5 ? 90 : 175);
        }
        table.setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(value == null ? "—" : String.format(Locale.ROOT, "%,d", value));
            }
        });
        JButton open = new JButton("Show selected");
        open.setEnabled(false);
        open.setToolTipText("Close history and reveal the selected milestone in the sidebar.");
        table.getSelectionModel().addListSelectionListener(event -> open.setEnabled(table.getSelectedRow() >= 0 && !shown.get(table.convertRowIndexToModel(table.getSelectedRow())).goalId.isEmpty()));
        open.addActionListener(event -> {
            if (table.getSelectedRow() >= 0) {
                reveal.accept(shown.get(table.convertRowIndexToModel(table.getSelectedRow())).goalId);
            }
        });
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(summary, BorderLayout.CENTER);
        bottom.add(open, BorderLayout.EAST);
        JLabel note = new JLabel("UTC · incomplete = last observed, not a confirmed miss · unobserved gaps may span several periods · dates are first observations");
        note.setFont(FontManager.getRunescapeSmallFont());
        bottom.add(note, BorderLayout.SOUTH);
        add(filters, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        DocumentListener listener = new DocumentListener() {
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
        };
        search.getDocument().addDocumentListener(listener);
        from.getDocument().addDocumentListener(listener);
        to.getDocument().addDocumentListener(listener);
        schedule.addActionListener(event -> filter());
        setPreferredSize(new Dimension(900, 460));
        filter();
    }
    private static String date(long value) {
        return value <= 0 ? "—" : DATE.format(Instant.ofEpochMilli(value));
    }
    private void filter() {
        long first;
        long last;
        try {
            first = from.getText().trim().isEmpty() ? 0 : LocalDate.parse(from.getText().trim()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            last = to.getText().trim().isEmpty() ? Long.MAX_VALUE : LocalDate.parse(to.getText().trim()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            if (last <= first) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            summary.setText("Enter valid UTC dates with the end on or after the start.");
            return;
        }
        RepeatSchedule repeat = schedule.getSelectedIndex() == 0 ? null : RepeatSchedule.values()[schedule.getSelectedIndex() - 1];
        table.clearSelection();
        shown.clear();
        model.setRowCount(0);
        long complete = 0;
        long incomplete = 0;
        long unobserved = 0;
        long milestones = 0;
        for (HistoryRecord record : records) {
            if (!record.matches(search.getText(), repeat, first, last)) {
                continue;
            }
            shown.add(record);
            model.addRow(new Object[]{record.title, record.lifetime ? "Lifetime" : record.schedule.toString(), date(record.start), date(record.end), record.progress, record.target, record.outcome, date(record.baseline), date(record.observed), date(record.completed)});
            if (record.lifetime) {
                milestones++;
            } else if (record.completed > 0) {
                complete++;
            } else if (record.progress == null) {
                unobserved++;
            } else if (record.outcome.startsWith("Incomplete")) {
                incomplete++;
            }
        }
        summary.setText("<html>" + shown.size() + " records · " + complete + " completed periods · " + incomplete + " incomplete<br>" + unobserved + " unobserved / waiting · " + milestones + " lifetime milestones</html>");
    }
}
