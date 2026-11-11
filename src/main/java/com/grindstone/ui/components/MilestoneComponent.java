
package com.grindstone.ui.components;

import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.ui.util.UiTypography;
import com.grindstone.util.HtmlUtils;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
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
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class MilestoneComponent extends JPanel {
    private static final Color CARD_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color TEXT_COLOR = ColorScheme.TEXT_COLOR;
    private static final Color MUTED_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
    static final int ICON_SIZE = 16;
    private static final ImageIcon PIN_ICON = createPinIcon(false);
    private static final ImageIcon PINNED_ICON = createPinIcon(true);
    private static final ImageIcon COLLAPSED_ICON = new ImageIcon(ImageUtil.loadImageResource(MilestoneComponent.class, "/util/arrow_right.png"));
    private static final ImageIcon EXPANDED_ICON = new ImageIcon(ImageUtil.rotateImage((BufferedImage) COLLAPSED_ICON.getImage(), Math.PI / 2));

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
        deadlineLabel.setToolTipText("Personal deadline in UTC.");
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
            complete.setToolTipText("Mark this milestone complete or incomplete.");
            complete.addActionListener(event -> toggle.run());
            manualControls.add(complete);
        } else {
            JButton plus = new JButton("+1");
            plus.setFont(UiTypography.BODY);
            plus.setMargin(new Insets(4, 5, 4, 5));
            plus.setEnabled(current < target);
            plus.setToolTipText("Add 1 to progress.");
            plus.addActionListener(event -> increment.run());
            manualControls.add(plus);
        }
        JButton set = new JButton("Set progress");
        set.setFont(UiTypography.BODY);
        set.setMargin(new Insets(4, 5, 4, 5));
        set.setToolTipText("Set progress manually.");
        set.addActionListener(event -> setProgress.run());
        manualControls.add(set);
        manualControls.setVisible(true);
    }
    private final JButton expandButton = new JButton(EXPANDED_ICON);
    private final JButton editButton = new JButton("...");
    private final JToggleButton pinButton = new JToggleButton(PIN_ICON);
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

    public MilestoneComponent() {
        super(new BorderLayout(0, 6));
        setAlignmentX(LEFT_ALIGNMENT);
        setOpaque(true);
        setBackground(CARD_COLOR);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR), BorderFactory.createEmptyBorder(8, 8, 8, 8)));

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
        iconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));

        expandButton.putClientProperty("JButton.buttonType", "toolBarButton");
        expandButton.setToolTipText("Collapse milestone details");
        expandButton.setRolloverEnabled(true);
        expandButton.setMargin(new Insets(0, 4, 0, 4));
        expandButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        expandButton.addActionListener(event -> setExpanded(!expanded));

        editButton.putClientProperty("JButton.buttonType", "toolBarButton");
        editButton.setToolTipText("Actions");
        editButton.setRolloverEnabled(true);
        editButton.setMargin(new Insets(0, 4, 0, 4));
        editButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editButton.setVisible(false);

        pinButton.putClientProperty("JButton.buttonType", "toolBarButton");
        pinButton.setSelectedIcon(PINNED_ICON);
        pinButton.setRolloverEnabled(true);
        pinButton.setMargin(new Insets(0, 3, 0, 3));
        pinButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pinButton.setVisible(false);

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

        header.add(iconLabel, BorderLayout.WEST);
        header.add(headerText, BorderLayout.CENTER);
        headerActions.setOpaque(false);
        headerActions.add(pinButton);
        headerActions.add(editButton);
        headerActions.add(expandButton);

        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        progressBar.setPreferredSize(new Dimension(100, 5));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));

        progressLabel.setForeground(MUTED_COLOR);
        progressLabel.setFont(UiTypography.META);

        footer.setOpaque(false);
        footer.setLayout(new BorderLayout(0, 4));
        footer.add(progressBar, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(progressLabel, BorderLayout.NORTH);
        bottom.add(manualControls, BorderLayout.SOUTH);
        manualControls.setVisible(false);
        footer.add(bottom, BorderLayout.SOUTH);
        statusLabel.setFont(UiTypography.META);
        JPanel statusRow = new JPanel(new BorderLayout(6, 0));
        statusRow.setOpaque(false);
        statusRow.add(statusLabel, BorderLayout.CENTER);
        statusRow.add(headerActions, BorderLayout.EAST);
        footer.add(statusRow, BorderLayout.NORTH);

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
        requiredQuantityLabel.setToolTipText("Quantity needed for this target. " + requiredQuantityLabel.getText());
        requiredQuantityLabel.setVisible(quantity > 0);
        revalidate();
        repaint();
    }
    public void setTrackingNote(String text) {
        trackingNoteLabel.setText(text);
        trackingNoteLabel.setToolTipText("Saved storage contents. Reopen storage to refresh.");
        trackingNoteLabel.setVisible(text != null && !text.isEmpty());
        revalidate();
    }
    public void setAchievementNote(String text) {
        achievementNoteLabel.setText(text);
        achievementNoteLabel.setToolTipText(text == null ? null : text + ". See Milestone history in milestone actions for dates and targets.");
        achievementNoteLabel.setVisible(text != null && !text.isEmpty());
        revalidate();
    }

    public void setGoalStatus(MilestoneStatus status) {
        MilestoneStatus state = status == null ? MilestoneStatus.NOT_STARTED : status;
        statusLabel.setText(state == MilestoneStatus.COMPLETE ? "Complete" : state == MilestoneStatus.IN_PROGRESS ? "In progress" : "Not started");
        statusLabel.setForeground(SegmentedProgressBar.getColorFor(state));
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

    private static ImageIcon createPinIcon(boolean pinned) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setColor(pinned ? ColorScheme.PROGRESS_INPROGRESS_COLOR : MUTED_COLOR);
            graphics.rotate(Math.PI / 4, 8, 8);
            Path2D pin = new Path2D.Double();
            pin.moveTo(5, 2.5);
            pin.lineTo(11, 2.5);
            pin.lineTo(10.5, 4);
            pin.lineTo(10.5, 7);
            pin.lineTo(13, 10);
            pin.lineTo(3, 10);
            pin.lineTo(5.5, 7);
            pin.lineTo(5.5, 4);
            pin.closePath();
            if (pinned) {
                graphics.fill(pin);
            }
            graphics.draw(pin);
            graphics.drawLine(8, 10, 8, 14);
        } finally {
            graphics.dispose();
        }
        return new ImageIcon(image);
    }

    public void setPinAction(boolean pinned, Runnable action) {
        for (ActionListener listener : pinButton.getActionListeners()) {
            pinButton.removeActionListener(listener);
        }
        pinButton.setSelected(pinned);
        updatePinButton();
        if (action != null) {
            pinButton.addActionListener(event -> {
                updatePinButton();
                action.run();
            });
        }
        pinButton.setVisible(action != null);
        revalidate();
        repaint();
    }

    private void updatePinButton() {
        String action = pinButton.isSelected() ? "Unpin milestone" : "Pin milestone";
        pinButton.setToolTipText(action);
        pinButton.getAccessibleContext().setAccessibleName(action);
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
            JMenuItem editTarget = new JMenuItem("Edit...");
            editTarget.setToolTipText("Edit the target or personal milestone details.");
            editTarget.addActionListener(edit);
            menu.add(editTarget);
        }
        JMenuItem remove = new JMenuItem("Delete...");
        remove.setToolTipText("Delete this milestone.");
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
        updateIconImage();
        if (icon instanceof AsyncBufferedImage) {
            ((AsyncBufferedImage) icon).onLoaded(() -> SwingUtilities.invokeLater(() -> {
                if (this.icon == icon) {
                    updateIconImage();
                    iconLabel.repaint();
                }
            }));
        }
        iconLabel.setVisible(iconVisible && icon != null);
        revalidate();
        repaint();
    }

    private void updateIconImage() {
        iconLabel.setIcon(icon == null ? null : createCompactIcon(icon));
    }

    static ImageIcon createCompactIcon(BufferedImage image) {
        double scale = Math.min(1, ICON_SIZE / (double) Math.max(image.getWidth(), image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        return new ImageIcon(ImageUtil.resizeImage(image, width, height));
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
        expandButton.setIcon(expanded ? EXPANDED_ICON : COLLAPSED_ICON);
        expandButton.setToolTipText(expanded ? "Collapse milestone details" : "Expand milestone details");
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
        updateIconImage();
        iconLabel.setVisible(iconVisible && icon != null);
        updateProgress();
        content.setVisible(expanded);
        footer.setVisible(true);
        expandButton.setIcon(expanded ? EXPANDED_ICON : COLLAPSED_ICON);
        progressBar.setVisible(progressBarVisible);
    }

    private void updateProgress() {
        expandButton.setVisible(content.getComponentCount() > 0);
        progressBar.setProgress(progressCurrent, progressMaximum);
        setGoalStatus(progressMaximum > 0 && progressCurrent >= progressMaximum ? MilestoneStatus.COMPLETE : progressCurrent > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED);
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
