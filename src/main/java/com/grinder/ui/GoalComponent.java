package com.grinder.ui;

import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GoalComponent extends JPanel {
    private static final int GAP = 8;
    private static final int ICON_SIZE = 20;
    private static final float TITLE_FONT_SIZE = 13f;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ImageComponent iconComponent;
    private final JLabel titleLabel;

    @Getter
    private final JPanel content;

    private final JLabel startDateLabel;
    private final JLabel endDateLabel;

    public GoalComponent() {
        super(new BorderLayout(0, GAP));

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        iconComponent = new ImageComponent(ICON_SIZE, ICON_SIZE);

        titleLabel = new JLabel();
        titleLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);

        JPanel rightSpacer = new JPanel();
        rightSpacer.setOpaque(false);
        rightSpacer.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));

        header.add(iconComponent, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        header.add(rightSpacer, BorderLayout.EAST);

        JPanel headerSection = new JPanel(new BorderLayout(0, GAP));
        headerSection.setOpaque(false);
        headerSection.add(header, BorderLayout.CENTER);
        headerSection.add(createSeparator(), BorderLayout.SOUTH);

        add(headerSection, BorderLayout.NORTH);

        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        startDateLabel = createFooterLabel();
        endDateLabel = createFooterLabel();
        endDateLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        footer.add(startDateLabel, BorderLayout.WEST);
        footer.add(endDateLabel, BorderLayout.EAST);

        JPanel footerSection = new JPanel(new BorderLayout(0, GAP));
        footerSection.setOpaque(false);
        footerSection.add(createSeparator(), BorderLayout.NORTH);
        footerSection.add(footer, BorderLayout.CENTER);

        add(footerSection, BorderLayout.SOUTH);
    }

    public void setTitle(String title) {
        titleLabel.setText(title == null ? "" : title);
        revalidate();
        repaint();
    }

    public void setIcon(BufferedImage image) {
        iconComponent.setImage(image);
    }

    public void setStartDate(LocalDateTime startDate) {
        startDateLabel.setText(formatDate(startDate, "Not started"));
        revalidate();
        repaint();
    }

    public void setEndDate(LocalDateTime endDate) {
        endDateLabel.setText(formatDate(endDate, "Ongoing"));
        revalidate();
        repaint();
    }

    private JLabel createFooterLabel() {
        JLabel label = new JLabel();
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(FontManager.getRunescapeSmallFont());
        return label;
    }

    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        separator.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        return separator;
    }

    private String formatDate(LocalDateTime date, String fallback) {
        return date == null ? fallback : DATE_FORMAT.format(date);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }
}