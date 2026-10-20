package com.grindstone.ui.components;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class UtcDatePicker extends JPanel {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH);
    private final JButton choose = new JButton();
    @Getter
    private LocalDate date;

    public UtcDatePicker(LocalDate date) {
        super(new BorderLayout());
        setOpaque(false);
        choose.setToolTipText("Choose a UTC date from the calendar. Clear removes the date.");
        choose.getAccessibleContext().setAccessibleName("Choose UTC date");
        choose.addActionListener(event -> openCalendar());
        add(choose, BorderLayout.CENTER);
        setDate(date);
    }

    public void setDate(LocalDate date) {
        if (date != null && (date.getYear() < 1970 || date.getYear() > 9998)) {
            throw new IllegalArgumentException("Choose a UTC date from 1970 to 9998.");
        }
        LocalDate previous = this.date;
        this.date = date;
        choose.setText(date == null ? "Choose date..." : DISPLAY.format(date));
        firePropertyChange("date", previous, date);
    }

    private void openCalendar() {
        JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(createCalendar(popup), BorderLayout.CENTER);
        popup.show(choose, 0, choose.getHeight());
    }

    JPanel createCalendar(JPopupMenu popup) {
        LocalDate initial = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        JPanel calendar = new JPanel(new BorderLayout(6, 8));
        calendar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel heading = new JPanel(new GridLayout(1, 2, 6, 0));
        JComboBox<String> month = new JComboBox<>();
        for (Month value : Month.values()) {
            month.addItem(value.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        month.setSelectedIndex(initial.getMonthValue() - 1);
        month.getAccessibleContext().setAccessibleName("Month");
        JSpinner year = new JSpinner(new SpinnerNumberModel(initial.getYear(), 1970, 9998, 1));
        year.setEditor(new JSpinner.NumberEditor(year, "0"));
        year.getAccessibleContext().setAccessibleName("Year");
        heading.add(month);
        heading.add(year);
        calendar.add(heading, BorderLayout.NORTH);
        JPanel days = new JPanel(new GridLayout(0, 7, 2, 2));
        Runnable refresh = () -> {
            days.removeAll();
            for (String weekday : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) {
                days.add(new JLabel(weekday, SwingConstants.CENTER));
            }
            LocalDate first = LocalDate.of(((Number) year.getValue()).intValue(), month.getSelectedIndex() + 1, 1);
            int offset = first.getDayOfWeek().getValue() - 1;
            for (int cell = 0; cell < 42; cell++) {
                int day = cell - offset + 1;
                if (day < 1 || day > first.lengthOfMonth()) {
                    days.add(new JLabel());
                    continue;
                }
                LocalDate value = first.withDayOfMonth(day);
                JButton button = new JButton(Integer.toString(day));
                button.setMargin(new Insets(5, 5, 5, 5));
                button.setToolTipText(value + " UTC");
                button.getAccessibleContext().setAccessibleName(DISPLAY.format(value));
                if (value.equals(date)) {
                    button.setForeground(ColorScheme.BRAND_ORANGE);
                }
                button.addActionListener(event -> {
                    setDate(value);
                    popup.setVisible(false);
                });
                days.add(button);
            }
            days.revalidate();
            days.repaint();
        };
        month.addActionListener(event -> refresh.run());
        year.addChangeListener(event -> refresh.run());
        refresh.run();
        calendar.add(days, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton today = new JButton("Today (UTC)");
        today.addActionListener(event -> {
            setDate(LocalDate.now(ZoneOffset.UTC));
            popup.setVisible(false);
        });
        JButton clear = new JButton("Clear");
        clear.addActionListener(event -> {
            setDate(null);
            popup.setVisible(false);
        });
        actions.add(today);
        actions.add(clear);
        calendar.add(actions, BorderLayout.SOUTH);
        return calendar;
    }
}
