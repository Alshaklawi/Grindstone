package com.grindstone.ui.components;

import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import net.runelite.client.ui.ColorScheme;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class SnapshotStatusPanel extends JPanel {
    private static final ImageIcon INFO_ICON = createInfoIcon();
    private final JPanel rows = new JPanel();

    public SnapshotStatusPanel() {
        super(new BorderLayout(0, 4));
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        JToggleButton heading = new JToggleButton("Snapshots", true);
        heading.setFont(UiTypography.BODY);
        heading.setToolTipText("Show or hide captured game data. Hover each information button for refresh instructions.");
        heading.addActionListener(event -> rows.setVisible(heading.isSelected()));
        add(heading, BorderLayout.NORTH);
        add(rows, BorderLayout.CENTER);
    }

    private static ImageIcon createInfoIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(90, 160, 215));
        graphics.fillOval(1, 1, 14, 14);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        graphics.drawString("i", 6, 12);
        graphics.dispose();
        return new ImageIcon(image);
    }

    public void updateSnapshots(Map<String, String> statuses, Map<String, String> tips, Set<String> missing) {
        rows.removeAll();
        statuses.forEach((name, status) -> {
            JPanel row = new JPanel(new BorderLayout(3, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            JLabel label = new JLabel(name);
            label.setFont(UiTypography.META);
            label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            row.add(label, BorderLayout.WEST);
            JLabel value = new JLabel(status, JLabel.RIGHT);
            value.setFont(UiTypography.META);
            value.setForeground("Not loaded".equals(status) ? ColorScheme.LIGHT_GRAY_COLOR : Color.WHITE);
            value.setToolTipText(tips.get(name));
            row.add(value, BorderLayout.CENTER);
            if (missing.contains(name)) {
                JButton info = new JButton(INFO_ICON);
                info.setBorder(BorderFactory.createEmptyBorder());
                info.setPreferredSize(new Dimension(20, 20));
                info.setContentAreaFilled(false);
                info.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                info.setToolTipText(tips.get(name));
                info.getAccessibleContext().setAccessibleName(name + " snapshot information");
                row.add(info, BorderLayout.EAST);
            }
            rows.add(row);
        });
        revalidate();
        repaint();
    }
}
