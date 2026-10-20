package com.grindstone.ui.components;

import com.grindstone.content.milestone.MilestoneStatus;
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
    private int maximumChunks = 20;
    private long target = 100;
    private long current;
    private boolean explicitProgress;
    public void setMaximumChunks(int maximumChunks) {
        this.maximumChunks = Math.max(1, Math.min(20, maximumChunks));
        repaint();
    }
    public long getChunkSize() {
        return target <= 0 ? 1 : 1 + (target - 1) / maximumChunks;
    }
    public int getChunkCount() {
        return target <= 0 ? 1 : (int) (1 + (target - 1) / getChunkSize());
    }
    public void setProgress(long current, long target) {
        this.current = Math.max(0, current);
        this.target = Math.max(0, target);
        explicitProgress = true;
        setValue(target <= 0 ? 0 : (int) Math.min(10_000, this.current * 10_000.0 / target));
        repaint();
    }
    public SegmentedProgressBar() {
        super(0, 10_000);
        setOpaque(false);
        setBorderPainted(false);
        setStringPainted(false);
        setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        setStatus(MilestoneStatus.NOT_STARTED);
    }

    public void setStatus(MilestoneStatus status) {
        setForeground(getColorFor(status));
    }

    public static Color getColorFor(MilestoneStatus status) {
        return status == MilestoneStatus.COMPLETE ? ColorScheme.PROGRESS_COMPLETE_COLOR
                : status == MilestoneStatus.IN_PROGRESS ? ColorScheme.PROGRESS_INPROGRESS_COLOR
                : ColorScheme.LIGHT_GRAY_COLOR;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        if (width <= 0 || height <= 0) {
            return;
        }
        int count = getChunkCount();
        int gap = width >= 24 ? 2 : 1;
        double ratio = Math.max(0, Math.min(1, getPercentComplete()));
        for (int index = 0; index < count; index++) {
            int start = index * width / count;
            int end = (index + 1) * width / count - (index == count - 1 ? 0 : gap);
            int segmentWidth = Math.max(0, end - start);
            graphics.setColor(getBackground());
            graphics.fillRect(insets.left + start, insets.top, segmentWidth, height);
            long chunkStart = index * getChunkSize();
            long chunkUnits = Math.max(1, Math.min(getChunkSize(), target - chunkStart));
            double fraction = explicitProgress ? (current - chunkStart) / (double) chunkUnits : ratio * count - index;
            int fill = (int) Math.round(segmentWidth * Math.max(0, Math.min(1, fraction)));
            graphics.setColor(getForeground());
            graphics.fillRect(insets.left + start, insets.top, fill, height);
        }
    }
}
