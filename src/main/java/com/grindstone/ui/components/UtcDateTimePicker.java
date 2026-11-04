package com.grindstone.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public final class UtcDateTimePicker extends JPanel {
    private final UtcDatePicker date;
    private final JComboBox<Integer> hour = createTimeSelector(23, "Hour (UTC)");
    private final JComboBox<Integer> minute = createTimeSelector(59, "Minute (UTC)");
    private final long originalEpochMillis;

    public UtcDateTimePicker(long epochMillis) {
        super(new BorderLayout(0, 8));
        setOpaque(false);
        originalEpochMillis = epochMillis;
        LocalDateTime initial = epochMillis == 0 ? null : Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC).toLocalDateTime();
        date = new UtcDatePicker(initial == null ? null : initial.toLocalDate());
        LocalTime time = initial == null ? LocalTime.of(23, 59) : initial.toLocalTime();
        hour.setSelectedItem(time.getHour());
        minute.setSelectedItem(time.getMinute());
        add(date, BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Time (24-hour)"));
        controls.add(hour);
        controls.add(new JLabel(":"));
        controls.add(minute);
        add(controls, BorderLayout.CENTER);
        date.addPropertyChangeListener("date", event -> updateEnabled());
        updateEnabled();
    }

    public boolean isDateSelected() {
        return date.getDate() != null;
    }

    public long getEpochMillis() {
        if (!isDateSelected()) {
            return 0;
        }
        LocalDateTime selected = date.getDate().atTime((Integer) hour.getSelectedItem(), (Integer) minute.getSelectedItem());
        if (originalEpochMillis != 0) {
            LocalDateTime original = Instant.ofEpochMilli(originalEpochMillis).atOffset(ZoneOffset.UTC).toLocalDateTime();
            if (selected.equals(original.truncatedTo(ChronoUnit.MINUTES))) {
                return originalEpochMillis;
            }
        }
        return selected.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private void updateEnabled() {
        hour.setEnabled(isDateSelected());
        minute.setEnabled(isDateSelected());
    }

    private static JComboBox<Integer> createTimeSelector(int maximum, String label) {
        JComboBox<Integer> selector = new JComboBox<>();
        for (int value = 0; value <= maximum; value++) {
            selector.addItem(value);
        }
        selector.setMaximumRowCount(12);
        selector.getAccessibleContext().setAccessibleName(label);
        selector.setToolTipText(label);
        selector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel rendered = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Integer) {
                    rendered.setText(String.format(Locale.ROOT, "%02d", value));
                }
                return rendered;
            }
        });
        return selector;
    }
}
