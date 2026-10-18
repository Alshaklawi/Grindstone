
package com.grinder.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import com.grinder.ui.util.UiTypography;
import com.grinder.content.goal.GoalStatus;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalComponent extends JPanel {
    private static final Color CARD_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color TEXT_COLOR = ColorScheme.TEXT_COLOR;
    private static final Color MUTED_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JPanel header = new JPanel(new BorderLayout(8, 0));
    private final JPanel headerText = new JPanel();
    private final JPanel content = new JPanel();
    private final JPanel footer = new JPanel(new BorderLayout(6, 0));
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JLabel progressLabel = new JLabel();
    private final SegmentedProgressBar progressBar = new SegmentedProgressBar();
    private final JLabel statusLabel = new JLabel();
    private final JButton expandButton = new JButton("−");
    private final JButton editButton = new JButton("...");
    private final JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    private String title = "";
    private BufferedImage icon;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private long progressCurrent;
    private long progressMaximum;
    private String progressText = "";
    private String progressStatus = "";
    private boolean expanded = true;
    private boolean iconVisible = true;
    private boolean progressBarVisible = true;

    public GoalComponent() {
        super(new BorderLayout(0, 6));
        setOpaque(true);
        setBackground(CARD_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(9, 8, 9, 8)));

        header.setOpaque(false);
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.putClientProperty("html.disable", Boolean.TRUE);
        titleLabel.setFont(UiTypography.TITLE);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        dateLabel.setForeground(MUTED_COLOR);
        dateLabel.setFont(UiTypography.META);

        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(24, 24));

        expandButton.putClientProperty("JButton.buttonType", "toolBarButton");
        expandButton.setToolTipText("Collapse goal details");
        expandButton.setRolloverEnabled(true);
        expandButton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        expandButton.addActionListener(event -> setExpanded(!expanded));

        editButton.putClientProperty("JButton.buttonType", "toolBarButton");
        editButton.setToolTipText("Goal actions");
        editButton.setRolloverEnabled(true);
        editButton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        editButton.setVisible(false);

        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(2));
        JPanel headerDetails = new JPanel(new BorderLayout(4, 0));
        headerDetails.setOpaque(false);
        headerDetails.setAlignmentX(LEFT_ALIGNMENT);
        headerDetails.add(dateLabel, BorderLayout.CENTER);
        headerDetails.add(headerActions, BorderLayout.EAST);
        headerText.add(headerDetails);

        header.add(iconLabel, BorderLayout.WEST);
        header.add(headerText, BorderLayout.CENTER);
        headerActions.setOpaque(false);
        headerActions.add(editButton);
        headerActions.add(expandButton);

        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        progressBar.setPreferredSize(new Dimension(100, 9));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9));

        progressLabel.setForeground(MUTED_COLOR);
        progressLabel.setFont(UiTypography.BODY);

        footer.setOpaque(false);
        footer.setLayout(new BorderLayout(0, 4));
        footer.add(progressBar, BorderLayout.CENTER);
        footer.add(progressLabel, BorderLayout.SOUTH);
        statusLabel.setFont(UiTypography.BODY);
        footer.add(statusLabel, BorderLayout.NORTH);

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        refreshLayout();
    }

    public JPanel getContent() {
        return content;
    }

    public void setGoalStatus(GoalStatus status) {
        GoalStatus state = status == null ? GoalStatus.NOT_STARTED : status;
        statusLabel.setText(state == GoalStatus.COMPLETE ? "Complete"
                : state == GoalStatus.IN_PROGRESS ? "In progress" : "Not started");
        statusLabel.setForeground(SegmentedProgressBar.colorFor(state));
        statusLabel.setToolTipText(state == GoalStatus.COMPLETE ? "This goal has reached its target."
                : state == GoalStatus.IN_PROGRESS ? "Progress has started and the target has not been reached."
                : "No progress has been recorded toward this goal yet.");
        progressBar.setStatus(state);
    }

    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) { return; }
        javax.swing.JTextArea text = new javax.swing.JTextArea(description, 0, 20);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setToolTipText(description.startsWith("First seen:")
                ? "When Grinder first observed this unlock on this character, not the historical drop date."
                : "Additional goal details. Custom descriptions can be edited from the goal actions menu.");
        text.setOpaque(false);
        text.setForeground(MUTED_COLOR);
        text.setFont(UiTypography.BODY);
        text.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        text.setAlignmentX(LEFT_ALIGNMENT);
        content.add(text);
        expandButton.setVisible(true);
    }

    public void setEditAction(ActionListener action) {
        for (ActionListener listener : editButton.getActionListeners()) {
            editButton.removeActionListener(listener);
        }

        if (action != null) {
            editButton.addActionListener(action);
            editButton.setVisible(true);
        }
        else {
            editButton.setVisible(false);
        }

        revalidate();
        repaint();
    }

    public void setGoalActions(ActionListener edit, ActionListener delete) {
        JPopupMenu menu = new JPopupMenu();
        if (edit != null) {
            JMenuItem editTarget = new JMenuItem("Edit goal...");
            editTarget.setToolTipText("Edit the goal target, or a custom goal's details and manual progress.");
            editTarget.addActionListener(edit);
            menu.add(editTarget);
        }
        JMenuItem remove = new JMenuItem("Delete goal...");
        remove.setToolTipText("Remove this goal after confirmation. Game progress is unaffected; Undo restores the last deletion.");
        remove.addActionListener(delete);
        menu.add(remove);
        setEditAction(event -> menu.show(editButton, 0, editButton.getHeight()));
        setComponentPopupMenu(menu);
        titleLabel.setComponentPopupMenu(menu);
    }

    public void update(String title, BufferedImage icon, LocalDateTime startDate, LocalDateTime endDate,
                       long current, long maximum, String progressText, String status) {
        this.title = title == null ? "" : title;
        this.icon = icon;
        this.startDate = startDate;
        this.endDate = endDate;
        this.progressCurrent = Math.max(0, current);
        this.progressMaximum = Math.max(0, maximum);
        this.progressText = progressText == null ? "" : progressText;
        this.progressStatus = status == null ? "" : status;

        applyValues();
        refreshLayout();
    }

    public void setTitle(String title) {
        String next = title == null ? "" : title;

        if (Objects.equals(this.title, next)) {
            return;
        }

        this.title = next;
        titleLabel.setText(next);
        titleLabel.setToolTipText(next);
        iconLabel.setToolTipText(next);
        revalidate();
        repaint();
    }

    public void setIcon(BufferedImage icon) {
        if (this.icon == icon) {
            return;
        }

        this.icon = icon;
        iconLabel.setIcon(icon == null ? null : new ImageIcon(icon));
        iconLabel.setPreferredSize(new Dimension(icon == null ? 24 : Math.max(24, icon.getWidth()),
                icon == null ? 24 : Math.max(24, icon.getHeight())));
        if (icon instanceof net.runelite.client.util.AsyncBufferedImage) {
            ((net.runelite.client.util.AsyncBufferedImage) icon).addTo(iconLabel);
        }
        iconLabel.setVisible(iconVisible && icon != null);
        revalidate();
        repaint();
    }

    public void setStartDate(LocalDateTime startDate) {
        if (Objects.equals(this.startDate, startDate)) {
            return;
        }

        this.startDate = startDate;
        updateDates();
    }

    public void setEndDate(LocalDateTime endDate) {
        if (Objects.equals(this.endDate, endDate)) {
            return;
        }

        this.endDate = endDate;
        updateDates();
    }

    public void setProgress(long current, long maximum, String text) {
        long nextCurrent = Math.max(0, current);
        long nextMaximum = Math.max(0, maximum);
        String nextText = text == null ? "" : text;

        if (progressCurrent == nextCurrent && progressMaximum == nextMaximum && Objects.equals(progressText, nextText)) {
            return;
        }

        progressCurrent = nextCurrent;
        progressMaximum = nextMaximum;
        progressText = nextText;
        progressStatus = "";
        updateProgress();
    }

    public void setProgressStatus(String status) {
        String next = status == null ? "" : status;

        if (Objects.equals(progressStatus, next)) {
            return;
        }

        progressStatus = next;
        progressText = next;
        updateProgress();
    }

    public void setIconVisible(boolean visible) {
        if (iconVisible == visible) {
            return;
        }

        iconVisible = visible;
        iconLabel.setVisible(visible && icon != null);
        revalidate();
        repaint();
    }

    public void setProgressBarVisible(boolean visible) {
        if (progressBarVisible == visible) {
            return;
        }

        progressBarVisible = visible;
        progressBar.setVisible(visible);
        revalidate();
        repaint();
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) {
            return;
        }

        this.expanded = expanded;
        content.setVisible(expanded);
        footer.setVisible(expanded);
        expandButton.setText(expanded ? "−" : "+");
        expandButton.setToolTipText(expanded ? "Collapse goal details" : "Expand goal details");
        revalidate();
        repaint();
    }

    private void applyValues() {
        titleLabel.setText(title);
        titleLabel.setToolTipText(title);
        iconLabel.setToolTipText(title);
        iconLabel.setIcon(icon == null ? null : new ImageIcon(icon));
        iconLabel.setVisible(iconVisible && icon != null);
        updateDates();
        updateProgress();
        content.setVisible(expanded);
        footer.setVisible(expanded);
        expandButton.setText(expanded ? "−" : "+");
        progressBar.setVisible(progressBarVisible);
    }

    private void updateDates() {
        if (startDate == null && endDate == null) {
            dateLabel.setText("");
            dateLabel.setVisible(false);
        }
        else {
            String start = startDate == null ? "—" : DATE_FORMAT.format(startDate);
            String end = endDate == null ? "—" : DATE_FORMAT.format(endDate);
            dateLabel.setText(start + "  →  " + end);
            dateLabel.setToolTipText("Start date → completion date. A dash means the date is unknown.");
            dateLabel.setVisible(true);
        }
        revalidate();
        repaint();
    }

    private void updateProgress() {
        expandButton.setVisible(content.getComponentCount() > 0);
        int value = progressMaximum <= 0 ? 0 : (int) Math.min(10_000, progressCurrent * 10_000.0 / progressMaximum);
        progressBar.setValue(value);
        setGoalStatus(progressMaximum > 0 && progressCurrent >= progressMaximum ? GoalStatus.COMPLETE
                : progressCurrent > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED);
        progressLabel.setText(progressText);
        progressLabel.setToolTipText(progressText);
        progressBar.setToolTipText(progressMaximum > 0
                ? String.format(java.util.Locale.ROOT, "%,d of %,d (%.1f%%). Yellow: in progress; green: complete.",
                        progressCurrent, progressMaximum, Math.min(100, progressCurrent * 100.0 / progressMaximum))
                : "No numeric target is available for this goal.");
        progressBar.setVisible(progressBarVisible);
        revalidate();
        repaint();
    }

    private void refreshLayout() {
        applyValues();
        revalidate();
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

}
