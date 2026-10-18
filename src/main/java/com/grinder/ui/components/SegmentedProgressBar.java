package com.grinder.ui.components;

import com.grinder.content.goal.GoalStatus;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JProgressBar;
import net.runelite.client.ui.ColorScheme;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class SegmentedProgressBar extends JProgressBar {
    public SegmentedProgressBar() {
        super(0, 10_000);
        setOpaque(false);
        setBorderPainted(false);
        setStringPainted(false);
        setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        setStatus(GoalStatus.NOT_STARTED);
    }

    public void setStatus(GoalStatus status) {
        setForeground(colorFor(status));
    }

    public static Color colorFor(GoalStatus status) {
        return status == GoalStatus.COMPLETE ? ColorScheme.PROGRESS_COMPLETE_COLOR
                : status == GoalStatus.IN_PROGRESS ? ColorScheme.PROGRESS_INPROGRESS_COLOR
                : ColorScheme.LIGHT_GRAY_COLOR;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        if (width <= 0 || height <= 0) { return; }
        int count = Math.max(1, Math.min(12, width / 8));
        int gap = width >= 24 ? 2 : 1;
        double ratio = Math.max(0, Math.min(1, getPercentComplete()));
        double filledSegments = ratio * count;
        for (int index = 0; index < count; index++) {
            int start = index * width / count;
            int end = (index + 1) * width / count - (index == count - 1 ? 0 : gap);
            int segmentWidth = Math.max(0, end - start);
            graphics.setColor(getBackground());
            graphics.fillRect(insets.left + start, insets.top, segmentWidth, height);
            int fill = (int) Math.round(segmentWidth * Math.max(0, Math.min(1, filledSegments - index)));
            graphics.setColor(getForeground());
            graphics.fillRect(insets.left + start, insets.top, fill, height);
        }
    }
}
