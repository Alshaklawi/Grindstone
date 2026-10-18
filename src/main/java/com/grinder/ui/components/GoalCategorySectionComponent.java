
package com.grinder.ui.components;


import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class GoalCategorySectionComponent extends JPanel {
    private static final int HEADER_HEIGHT = 38;

    private final JPanel header = new JPanel(new BorderLayout(8, 0));
    private final JPanel content = new JPanel();
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel countLabel = new JLabel();
    private final JLabel toggleLabel = new JLabel();
    private boolean expanded = true;

    public GoalCategorySectionComponent(String title, Icon icon) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        iconLabel.setIcon(icon);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        titleLabel.setText(title);
        titleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        titleLabel.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());

        countLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        toggleLabel.setText("\u25BE");
        toggleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        toggleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        toggleLabel.setPreferredSize(new Dimension(20, HEADER_HEIGHT));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        header.setOpaque(true);
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.add(titlePanel, BorderLayout.CENTER);

        JPanel trailingPanel = new JPanel(new BorderLayout(8, 0));
        trailingPanel.setOpaque(false);
        trailingPanel.add(countLabel, BorderLayout.CENTER);
        trailingPanel.add(toggleLabel, BorderLayout.EAST);
        header.add(trailingPanel, BorderLayout.EAST);

        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        MouseAdapter toggleListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                setExpanded(!expanded);
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                header.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        };

        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.addMouseListener(toggleListener);
        addMouseListener(toggleListener);
        setExpanded(true);
    }

    public JPanel getContent() {
        return content;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setIcon(Icon icon) {
        iconLabel.setIcon(icon);
    }

    public void setCount(int count) {
        countLabel.setText(Integer.toString(Math.max(0, count)));
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        content.setVisible(expanded);
        toggleLabel.setText(expanded ? "\u25BE" : "\u25B8");
        revalidate();
        repaint();
    }

    public void setContent(JPanel panel) {
        content.removeAll();

        if (panel != null) {
            content.add(panel);
        }

        revalidate();
        repaint();
    }
}
