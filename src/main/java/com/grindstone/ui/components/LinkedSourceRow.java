package com.grindstone.ui.components;

import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.ui.util.UiTypography;
import com.grindstone.util.HtmlUtils;
import com.grindstone.util.NumberFormats;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/11/2026
 */
public final class LinkedSourceRow extends ContentHeightPanel {
    public LinkedSourceRow(String title, BufferedImage image, long current, long target, boolean complete, String unavailableReason, boolean showBar, int chunks) {
        setLayout(new BorderLayout(6, 0));
        setAlignmentX(LEFT_ALIGNMENT);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(5, 0, 3, 0));

        if (image != null) {
            JLabel icon = new JLabel();
            int size = MilestoneComponent.ICON_SIZE;
            icon.setPreferredSize(new Dimension(size, size));
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setVerticalAlignment(SwingConstants.TOP);
            icon.setIcon(MilestoneComponent.createCompactIcon(image));
            if (image instanceof AsyncBufferedImage) {
                ((AsyncBufferedImage) image).onLoaded(() -> SwingUtilities.invokeLater(() -> icon.setIcon(MilestoneComponent.createCompactIcon(image))));
            }
            add(icon, BorderLayout.WEST);
        }

        JPanel content = new JPanel(new BorderLayout(0, 2));
        content.setOpaque(false);
        JLabel name = new JLabel(title);
        name.putClientProperty("html.disable", Boolean.TRUE);
        name.setFont(UiTypography.BODY);
        name.setForeground(ColorScheme.TEXT_COLOR);
        name.setToolTipText("<html>" + HtmlUtils.escape(title) + "</html>");
        content.add(name, BorderLayout.NORTH);

        JPanel progress = new JPanel(new BorderLayout(6, 0));
        progress.setOpaque(false);
        String caption = unavailableReason;
        if (caption == null) {
            caption = complete && current < target ? "Reached earlier" : NumberFormats.formatTarget(current) + " / " + NumberFormats.formatTarget(target);
        }
        JLabel amount = new JLabel(caption);
        amount.setFont(UiTypography.META);
        amount.setForeground(complete ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
        if (unavailableReason == null) {
            amount.setToolTipText(String.format(Locale.ROOT, "Current: %,d · Target: %,d", current, target));
        }
        if (showBar && unavailableReason == null) {
            SegmentedProgressBar bar = new SegmentedProgressBar();
            bar.setMaximumChunks(chunks);
            bar.setProgress(complete ? Math.max(current, target) : current, target);
            bar.setStatus(complete ? MilestoneStatus.COMPLETE : current > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED);
            bar.setPreferredSize(new Dimension(30, 4));
            bar.getAccessibleContext().setAccessibleName(title + " progress");
            JPanel track = new JPanel(new GridBagLayout());
            track.setOpaque(false);
            GridBagConstraints fill = new GridBagConstraints();
            fill.weightx = 1;
            fill.fill = GridBagConstraints.HORIZONTAL;
            track.add(bar, fill);
            progress.add(track, BorderLayout.CENTER);
            progress.add(amount, BorderLayout.EAST);
        } else {
            progress.add(amount, BorderLayout.CENTER);
        }
        content.add(progress, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
    }

}
