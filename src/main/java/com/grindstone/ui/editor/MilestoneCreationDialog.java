package com.grindstone.ui.editor;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.Icon;
import com.grindstone.ui.util.DialogPlacement;
import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class MilestoneCreationDialog extends JDialog {
    private final MilestoneEditorPanel form;
    private final JScrollPane scroll;
    private final JButton add = new JButton("Add milestone");
    private final JTextArea error = new JTextArea(2, 28);
    private final JLabel heading = new JLabel("Track your next milestone");
    private final JLabel introduction = new JLabel("Choose a focus, set a target and make it yours.");
    private Milestone result;
    private boolean creating = true;

    private MilestoneCreationDialog(Component owner, MilestoneEditorPanel form) {
        super(SwingUtilities.getWindowAncestor(owner), "Add milestone", Dialog.ModalityType.APPLICATION_MODAL);
        this.form = form;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(480, 220));
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        heading.setFont(FontManager.getRunescapeBoldFont());
        heading.setIcon(new ImageIcon(Icon.MAIN.getImage()));
        heading.setIconTextGap(10);
        JPanel welcome = new JPanel(new BorderLayout(0, 6));
        welcome.setOpaque(false);
        welcome.add(heading, BorderLayout.NORTH);
        introduction.setFont(UiTypography.BODY);
        introduction.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        welcome.add(introduction, BorderLayout.CENTER);
        welcome.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        content.add(welcome, BorderLayout.NORTH);
        scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        error.setLineWrap(true);
        error.setWrapStyleWord(true);
        error.setEditable(false);
        error.setOpaque(false);
        error.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        error.setVisible(false);
        bottom.add(error, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancel = new JButton("Cancel");
        cancel.setToolTipText("Close without saving changes.");
        cancel.addActionListener(event -> dispose());
        add.addActionListener(event -> submit());
        buttons.add(cancel);
        buttons.add(add);
        bottom.add(buttons, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(add);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        form.addPropertyChangeListener("ready", event -> updateReady());
        form.addPropertyChangeListener("formLayout", event -> {
            error.setVisible(false);
            updateReady();
            fitToForm();
        });
        updateReady();
        fitToForm();
    }

    private void updateReady() {
        if (creating) {
            boolean routine = form.getRepeatSchedule() != RepeatSchedule.NONE;
            setTitle(routine ? "Add routine" : "Add milestone");
            add.setText(routine ? "Add routine" : "Add milestone");
            heading.setText(routine ? "Build your next routine" : "Track your next milestone");
            introduction.setText(routine ? "Choose a schedule and make steady progress." : "Choose a focus, set a target and make it yours.");
        }
        add.setEnabled(form.isReady());
        add.setToolTipText(form.isReady() ? "Save this milestone to the current player profile." : "Choose an available entry or select an item search result to continue.");
    }

    private void fitToForm() {
        Rectangle bounds = DialogPlacement.usableBounds(getOwner());
        Dimension desired = form.getPreferredSize();
        scroll.setPreferredSize(new Dimension(desired.width, Math.min(desired.height, Math.max(180, bounds.height - 230))));
        pack();
        setSize(Math.min(Math.max(480, getWidth()), bounds.width - 40), Math.min(getHeight(), bounds.height - 40));
        DialogPlacement.center(this, getOwner());
    }

    private void submit() {
        try {
            Milestone candidate = form.createGoal();
            if (form.isReviewEnabled()) {
                JTextArea preview = new JTextArea(form.reviewText(candidate), 12, 38);
                preview.setFont(UiTypography.BODY);
                preview.setEditable(false);
                preview.setLineWrap(true);
                preview.setWrapStyleWord(true);
                if (JOptionPane.showConfirmDialog(this, new JScrollPane(preview), form.getRepeatSchedule() == RepeatSchedule.NONE ? "Review milestone" : "Review routine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            result = candidate;
            dispose();
        } catch (ParseException | IllegalArgumentException exception) {
            error.setText(exception instanceof IllegalArgumentException ? exception.getMessage() : "Enter a number within the target's allowed range.");
            error.setVisible(true);
            fitToForm();
        }
    }

    @Override
    public void dispose() {
        form.stopSearch();
        super.dispose();
    }

    public static Milestone showDialog(Component owner, MilestoneEditorPanel form) {
        return showDialog(owner, form, "Add milestone", "Add milestone");
    }
    public static Milestone showDialog(Component owner, MilestoneEditorPanel form, String title, String action) {
        MilestoneCreationDialog dialog = new MilestoneCreationDialog(owner, form);
        dialog.creating = "Add milestone".equals(action);
        dialog.setTitle(title);
        dialog.add.setText(action);
        if (!"Add milestone".equals(action)) {
            dialog.heading.setText("Update your tracker");
            dialog.introduction.setText("Adjust your selection, target or repeat schedule.");
        }
        dialog.updateReady();
        dialog.setVisible(true);
        return dialog.result;
    }
}
