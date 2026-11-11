package com.grindstone.ui;

import com.grindstone.ui.util.UiTypography;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import net.runelite.client.ui.ColorScheme;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/05/2026
 */
final class SidebarFilterRenderer extends JPanel implements ListCellRenderer<String> {
    private final JLabel context = new JLabel();
    private final JLabel valueLabel = new JLabel();
    private final JLabel count = new JLabel();
    private final String field;
    private final Map<String, Integer> counts;

    SidebarFilterRenderer(String field, Map<String, Integer> counts) {
        super(new BorderLayout(6, 0));
        this.field = field;
        this.counts = counts;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        setPreferredSize(new Dimension(160, 28));
        context.setFont(UiTypography.META);
        valueLabel.setFont(UiTypography.BODY);
        valueLabel.putClientProperty("html.disable", Boolean.TRUE);
        count.setFont(UiTypography.META);
        add(context, BorderLayout.WEST);
        add(valueLabel, BorderLayout.CENTER);
        add(count, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean selected, boolean focus) {
        String text = value == null ? "" : value;
        boolean popupSelection = index >= 0 && selected;
        Color foreground = popupSelection ? list.getSelectionForeground() : ColorScheme.TEXT_COLOR;
        Color secondary = popupSelection ? foreground : ColorScheme.LIGHT_GRAY_COLOR;
        setBackground(popupSelection ? list.getSelectionBackground() : ColorScheme.DARKER_GRAY_COLOR);
        context.setText(field + ":");
        context.setVisible(index < 0 && !field.isEmpty());
        context.setForeground(secondary);
        valueLabel.setText(MilestoneFilters.ALL_STATUSES.equals(text) ? "All" : MilestoneFilters.LINKED.equals(text) ? "Groups" : text);
        valueLabel.setForeground(list.isEnabled() ? foreground : secondary);
        Integer total = counts.get(text);
        count.setText(total == null ? "" : total.toString());
        count.setVisible(total != null);
        count.setForeground(secondary);
        setToolTipText(null);
        return this;
    }

    @Override
    public void paint(Graphics graphics) {
        doLayout();
        super.paint(graphics);
    }
}
