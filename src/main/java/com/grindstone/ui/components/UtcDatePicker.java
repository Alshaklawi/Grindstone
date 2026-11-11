package com.grindstone.ui.components;

import com.grindstone.ui.util.DialogPlacement;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.IntConsumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import com.grindstone.ui.util.DialogPlacement;
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
        choose.setToolTipText("Choose a UTC date, or clear to remove it.");
        choose.getAccessibleContext().setAccessibleName("Choose UTC date");
        choose.addActionListener(event -> openCalendar());
        add(choose, BorderLayout.CENTER);
        setDate(date);
    }

    public void setDate(LocalDate date) {
        if (date != null && (date.getYear() < 1970 || date.getYear() > 9999)) {
            throw new IllegalArgumentException("Choose a UTC date from 1970 to 9999.");
        }
        LocalDate previous = this.date;
        this.date = date;
        choose.setText((date == null ? "Choose date..." : DISPLAY.format(date)) + "  \u25be");
        choose.getAccessibleContext().setAccessibleDescription(date == null ? "No date selected" : DISPLAY.format(date) + " UTC");
        firePropertyChange("date", previous, date);
    }

    private void openCalendar() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Choose date (UTC)", Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(createCalendar(dialog::dispose));
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        dialog.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
            }
        });
        dialog.pack();
        dialog.setResizable(false);
        DialogPlacement.center(dialog, this);
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
    }

    JPanel createCalendar(Runnable close) {
        LocalDate initial = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        JPanel calendar = new JPanel(new BorderLayout(6, 8));
        calendar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel heading = new JPanel(new BorderLayout(6, 0));
        JPanel selectors = new JPanel(new GridLayout(1, 2, 6, 0));
        JComboBox<Month> month = new JComboBox<>();
        for (Month value : Month.values()) {
            month.addItem(value);
        }
        month.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Month) {
                    label.setText(((Month) value).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                }
                return label;
            }
        });
        month.setSelectedItem(initial.getMonth());
        month.getAccessibleContext().setAccessibleName("Month");
        JSpinner year = new JSpinner(new SpinnerNumberModel(initial.getYear(), 1970, 9999, 1));
        year.setEditor(new JSpinner.NumberEditor(year, "0"));
        ((JSpinner.DefaultEditor) year.getEditor()).getTextField().setColumns(4);
        ((JSpinner.DefaultEditor) year.getEditor()).getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        year.getAccessibleContext().setAccessibleName("Year");
        selectors.add(month);
        selectors.add(year);
        JButton previous = new JButton("<");
        previous.setMargin(new Insets(4, 8, 4, 8));
        previous.setToolTipText("Previous month");
        previous.getAccessibleContext().setAccessibleName("Previous month");
        JButton next = new JButton(">");
        next.setMargin(new Insets(4, 8, 4, 8));
        next.setToolTipText("Next month");
        next.getAccessibleContext().setAccessibleName("Next month");
        heading.add(previous, BorderLayout.WEST);
        heading.add(selectors, BorderLayout.CENTER);
        heading.add(next, BorderLayout.EAST);
        calendar.add(heading, BorderLayout.NORTH);
        JPanel days = new JPanel(new GridLayout(0, 7, 2, 2));
        Runnable refresh = () -> {
            days.removeAll();
            for (String weekday : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) {
                days.add(new JLabel(weekday, SwingConstants.CENTER));
            }
            LocalDate first = LocalDate.of(((Number) year.getValue()).intValue(), ((Month) month.getSelectedItem()).getValue(), 1);
            previous.setEnabled(first.isAfter(LocalDate.of(1970, 1, 1)));
            next.setEnabled(first.isBefore(LocalDate.of(9999, 12, 1)));
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
                    button.setBackground(ColorScheme.BRAND_ORANGE);
                    button.setForeground(Color.BLACK);
                }
                button.addActionListener(event -> {
                    setDate(value);
                    close.run();
                });
                days.add(button);
            }
            days.revalidate();
            days.repaint();
        };
        month.addActionListener(event -> refresh.run());
        year.addChangeListener(event -> refresh.run());
        IntConsumer navigate = amount -> {
            try {
                year.commitEdit();
            } catch (ParseException invalid) {
                ((JSpinner.DefaultEditor) year.getEditor()).getTextField().setValue(year.getValue());
            }
            YearMonth target = YearMonth.of(((Number) year.getValue()).intValue(), ((Month) month.getSelectedItem()).getValue()).plusMonths(amount);
            if (target.getYear() >= 1970 && target.getYear() <= 9999) {
                year.setValue(target.getYear());
                month.setSelectedItem(target.getMonth());
            }
        };
        previous.addActionListener(event -> navigate.accept(-1));
        next.addActionListener(event -> navigate.accept(1));
        refresh.run();
        calendar.add(days, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 3, 6, 0));
        JButton today = new JButton("Today (UTC)");
        today.addActionListener(event -> {
            setDate(LocalDate.now(ZoneOffset.UTC));
            close.run();
        });
        JButton clear = new JButton("Clear");
        clear.addActionListener(event -> {
            setDate(null);
            close.run();
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> close.run());
        actions.add(today);
        actions.add(clear);
        actions.add(cancel);
        calendar.add(actions, BorderLayout.SOUTH);
        calendar.setPreferredSize(new Dimension(340, 320));
        return calendar;
    }
}
