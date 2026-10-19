
package com.grinder.ui.components;

import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.util.UiTypography;
import com.grinder.util.HtmlUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalComponent extends JPanel {
    private static final Color CARD_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color TEXT_COLOR = ColorScheme.TEXT_COLOR;
    private static final Color MUTED_COLOR = ColorScheme.LIGHT_GRAY_COLOR;

    private final JPanel header = new JPanel(new BorderLayout(8, 0));
    private final JPanel headerText = new JPanel();
    @Getter
    private final JPanel content = new JPanel();
    private final JPanel footer = new JPanel(new BorderLayout(6, 0));
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel requiredQuantityLabel = new JLabel();
    private final JLabel trackingNoteLabel = new JLabel();
    private final JLabel achievementNoteLabel = new JLabel();
    private final JLabel reliabilityLabel = new JLabel();
    private final JLabel deadlineLabel = new JLabel();
    public void setDeadlineNote(String text) {
        deadlineLabel.setText(text);
        deadlineLabel.setVisible(text != null && !text.isEmpty());
        deadlineLabel.setToolTipText("Personal UTC deadline. Passing it never deletes progress or changes game state; it is not a game reset timer.");
    }
    public void setReliability(String label, String detail) {
        reliabilityLabel.setText(label);
        reliabilityLabel.setToolTipText(detail);
        reliabilityLabel.setVisible(label != null && !label.isEmpty());
        String tooltip = detail == null || detail.isEmpty() ? title : "<html><div style='width: 260px'>" + HtmlUtils.escape(title) + "<br>" + HtmlUtils.escape(detail) + "</div></html>";
        titleLabel.setToolTipText(tooltip);
        iconLabel.setToolTipText(tooltip);
    }
    private final JLabel progressLabel = new JLabel();
    private final SegmentedProgressBar progressBar = new SegmentedProgressBar();
    private final JLabel statusLabel = new JLabel();
    private final JPanel manualControls = new JPanel(new GridLayout(1, 2, 6, 0));
    public void setManualActions(long current, long target, Runnable increment, Runnable toggle, Runnable setProgress) {
        manualControls.removeAll();
        manualControls.setOpaque(false);
        if (target == 1) {
            JCheckBox complete = new JCheckBox("Completed", current >= target);
            complete.setOpaque(false);
            complete.setFont(UiTypography.BODY);
            complete.setToolTipText("Check or uncheck this personal goal. This does not perform any game action.");
            complete.addActionListener(event -> toggle.run());
            manualControls.add(complete);
        } else {
            JButton plus = new JButton("+1");
            plus.setFont(UiTypography.BODY);
            plus.setMargin(new Insets(4, 5, 4, 5));
            plus.setEnabled(current < target);
            plus.setToolTipText("Add one to this personal goal's progress. Undo is available in the header.");
            plus.addActionListener(event -> increment.run());
            manualControls.add(plus);
        }
        JButton set = new JButton("Set progress");
        set.setFont(UiTypography.BODY);
        set.setMargin(new Insets(4, 5, 4, 5));
        set.setToolTipText("Enter manual progress without opening the full goal editor.");
        set.addActionListener(event -> setProgress.run());
        manualControls.add(set);
        manualControls.setVisible(true);
    }
    private final JButton expandButton = new JButton("−");
    private final JButton editButton = new JButton("...");
    private final JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    private String title = "";
    private BufferedImage icon;
    private long progressCurrent;
    private long progressMaximum;
    private String progressText = "";
    private String progressStatus = "";
    private boolean expanded = true;
    private Consumer<Boolean> expansionListener;
    public void setExpansionListener(Consumer<Boolean> listener) {
        expansionListener = listener;
    }
    public void setMaximumProgressChunks(int count) {
        progressBar.setMaximumChunks(count);
    }
    private boolean iconVisible = true;
    private boolean progressBarVisible = true;

    public GoalComponent() {
        super(new BorderLayout(0, 6));
        setAlignmentX(LEFT_ALIGNMENT);
        setOpaque(true);
        setBackground(CARD_COLOR);
        setBorder(BorderFactory.createCompoundBorder( BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR), BorderFactory.createEmptyBorder(9, 8, 9, 8)));

        header.setOpaque(false);
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.putClientProperty("html.disable", Boolean.TRUE);
        titleLabel.setFont(UiTypography.TITLE);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        requiredQuantityLabel.setFont(UiTypography.BODY);
        requiredQuantityLabel.setForeground(TEXT_COLOR);
        requiredQuantityLabel.setAlignmentX(LEFT_ALIGNMENT);
        requiredQuantityLabel.setVisible(false);
        trackingNoteLabel.setFont(UiTypography.META);
        trackingNoteLabel.setForeground(MUTED_COLOR);
        trackingNoteLabel.setAlignmentX(LEFT_ALIGNMENT);
        trackingNoteLabel.setVisible(false);
        achievementNoteLabel.setFont(UiTypography.META);
        achievementNoteLabel.setForeground(MUTED_COLOR);
        achievementNoteLabel.setVisible(false);
        achievementNoteLabel.setAlignmentX(LEFT_ALIGNMENT);

        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(24, 24));

        expandButton.putClientProperty("JButton.buttonType", "toolBarButton");
        expandButton.setToolTipText("Collapse goal details");
        expandButton.setRolloverEnabled(true);
        expandButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        expandButton.addActionListener(event -> setExpanded(!expanded));

        editButton.putClientProperty("JButton.buttonType", "toolBarButton");
        editButton.setToolTipText("Goal actions");
        editButton.setRolloverEnabled(true);
        editButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editButton.setVisible(false);

        headerText.add(titleLabel);
        headerText.add(requiredQuantityLabel);
        headerText.add(trackingNoteLabel);
        headerText.add(achievementNoteLabel);
        reliabilityLabel.setFont(UiTypography.META);
        reliabilityLabel.setForeground(MUTED_COLOR);
        reliabilityLabel.setAlignmentX(LEFT_ALIGNMENT);
        reliabilityLabel.setVisible(false);
        headerText.add(reliabilityLabel);
        deadlineLabel.setFont(UiTypography.META);
        deadlineLabel.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
        deadlineLabel.setAlignmentX(LEFT_ALIGNMENT);
        deadlineLabel.setVisible(false);
        headerText.add(deadlineLabel);
        headerText.add(Box.createVerticalStrut(2));
        JPanel headerDetails = new JPanel(new BorderLayout(4, 0));
        headerDetails.setOpaque(false);
        headerDetails.setAlignmentX(LEFT_ALIGNMENT);
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
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(progressLabel, BorderLayout.NORTH);
        bottom.add(manualControls, BorderLayout.SOUTH);
        manualControls.setVisible(false);
        footer.add(bottom, BorderLayout.SOUTH);
        statusLabel.setFont(UiTypography.BODY);
        footer.add(statusLabel, BorderLayout.NORTH);

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        refreshLayout();
    }


    public void addDetail(JComponent detail) {
        content.add(detail);
        updateProgress();
    }

    public void setRequiredQuantity(long quantity) {
        requiredQuantityLabel.setText(String.format(Locale.ROOT, "Required: %,d", Math.max(0, quantity)));
        requiredQuantityLabel.setToolTipText("Target item quantity, not the amount currently owned. " + requiredQuantityLabel.getText());
        requiredQuantityLabel.setVisible(quantity > 0);
        revalidate();
        repaint();
    }
    public void setTrackingNote(String text) {
        trackingNoteLabel.setText(text);
        trackingNoteLabel.setToolTipText("A snapshot is the last known client data, not a guarantee that every storage location is loaded or current.");
        trackingNoteLabel.setVisible(text != null && !text.isEmpty());
        revalidate();
    }
    public void setAchievementNote(String text) {
        achievementNoteLabel.setText(text);
        achievementNoteLabel.setToolTipText(text == null ? null : text + ". See Goal history in goal actions for dates and targets.");
        achievementNoteLabel.setVisible(text != null && !text.isEmpty());
        revalidate();
    }

    public void setGoalStatus(GoalStatus status) {
        GoalStatus state = status == null ? GoalStatus.NOT_STARTED : status;
        statusLabel.setText(state == GoalStatus.COMPLETE ? "Complete" : state == GoalStatus.IN_PROGRESS ? "In progress" : "Not started");
        statusLabel.setForeground(SegmentedProgressBar.colorFor(state));
        statusLabel.setToolTipText(null);
        progressBar.setStatus(state);
    }

    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return;
        }
        JTextArea text = new JTextArea(description, 0, 20);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
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
        } else {
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
    public void addGoalAction(String title, String tooltip, ActionListener listener) {
        JPopupMenu menu = getComponentPopupMenu();
        if (menu == null) {
            return;
        }
        JMenuItem item = new JMenuItem(title);
        item.setToolTipText(tooltip);
        item.addActionListener(listener);
        menu.add(item);
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
        iconLabel.setPreferredSize(new Dimension(icon == null ? 24 : Math.max(24, icon.getWidth()), icon == null ? 24 : Math.max(24, icon.getHeight())));
        if (icon instanceof AsyncBufferedImage) {
            ((AsyncBufferedImage) icon).addTo(iconLabel);
        }
        iconLabel.setVisible(iconVisible && icon != null);
        revalidate();
        repaint();
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
        footer.setVisible(true);
        expandButton.setText(expanded ? "−" : "+");
        expandButton.setToolTipText(expanded ? "Collapse goal details" : "Expand goal details");
        if (expansionListener != null) {
            expansionListener.accept(expanded);
        }
        revalidate();
        repaint();
    }

    private void applyValues() {
        titleLabel.setText(title);
        titleLabel.setToolTipText(title);
        iconLabel.setToolTipText(title);
        iconLabel.setIcon(icon == null ? null : new ImageIcon(icon));
        iconLabel.setVisible(iconVisible && icon != null);
        updateProgress();
        content.setVisible(expanded);
        footer.setVisible(true);
        expandButton.setText(expanded ? "−" : "+");
        progressBar.setVisible(progressBarVisible);
    }

    private void updateProgress() {
        expandButton.setVisible(content.getComponentCount() > 0);
        progressBar.setProgress(progressCurrent, progressMaximum);
        setGoalStatus(progressMaximum > 0 && progressCurrent >= progressMaximum ? GoalStatus.COMPLETE : progressCurrent > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED);
        progressLabel.setText(progressMaximum > 0 && progressStatus.isEmpty() ? String.format(Locale.ROOT, "%s (%.1f%%)", progressText, Math.min(100.0, progressCurrent * 100.0 / progressMaximum)) : progressText);
        progressLabel.setToolTipText(null);
        progressBar.setToolTipText(null);
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
