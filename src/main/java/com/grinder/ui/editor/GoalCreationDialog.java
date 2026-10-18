package com.grinder.ui.editor;

import com.grinder.ui.Icon;

import com.grinder.content.goal.Goal;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
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
public final class GoalCreationDialog extends JDialog {
    private final GoalEditorPanel form;
    private final JScrollPane scroll;
    private final JButton add = new JButton("Add goal");
    private final JTextArea error = new JTextArea(2, 28);
    private Goal result;

    private GoalCreationDialog(Component owner, GoalEditorPanel form) {
        super(SwingUtilities.getWindowAncestor(owner), "Add goal", Dialog.ModalityType.APPLICATION_MODAL);
        this.form = form;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(420, 220));
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel heading = new JLabel("Track your next milestone");
        heading.setFont(FontManager.getRunescapeBoldFont());
        heading.setIcon(new ImageIcon(Icon.MAIN.getImage()));
        heading.setIconTextGap(10);
        content.add(heading, BorderLayout.NORTH);
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
        cancel.setToolTipText("Close without adding a goal.");
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
            public void actionPerformed(ActionEvent event) { dispose(); }
        });
        form.addPropertyChangeListener("ready", event -> updateReady());
        form.addPropertyChangeListener("formLayout", event -> {
            error.setVisible(false);
            fitToForm();
        });
        updateReady();
        fitToForm();
        setLocationRelativeTo(owner);
    }

    private void updateReady() {
        add.setEnabled(form.isReady());
        add.setToolTipText(form.isReady() ? "Add this goal to the current player profile."
                : "Search for an item and select a result first.");
    }

    private void fitToForm() {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension desired = form.getPreferredSize();
        scroll.setPreferredSize(new Dimension(desired.width, Math.min(desired.height, Math.max(180, bounds.height - 230))));
        pack();
        setSize(Math.max(420, getWidth()), Math.min(getHeight(), bounds.height - 40));
        if (isShowing()) {
            setLocation(Math.max(bounds.x, Math.min(getX(), bounds.x + bounds.width - getWidth())),
                    Math.max(bounds.y, Math.min(getY(), bounds.y + bounds.height - getHeight())));
        }
    }

    private void submit() {
        try {
            result = form.createGoal();
            dispose();
        }
        catch (java.text.ParseException | IllegalArgumentException exception) {
            error.setText(exception instanceof IllegalArgumentException ? exception.getMessage()
                    : "Enter a number within the target's allowed range.");
            error.setVisible(true);
            fitToForm();
        }
    }

    @Override
    public void dispose() {
        form.stopSearch();
        super.dispose();
    }

    public static Goal showDialog(Component owner, GoalEditorPanel form) {
        return showDialog(owner, form, "Add goal", "Add goal");
    }
    public static Goal showDialog(Component owner, GoalEditorPanel form, String title, String action) {
        GoalCreationDialog dialog = new GoalCreationDialog(owner, form);
        dialog.setTitle(title);
        dialog.add.setText(action);
        dialog.add.setToolTipText("Save the selected goal to the current player profile.");
        dialog.setVisible(true);
        return dialog.result;
    }
}
