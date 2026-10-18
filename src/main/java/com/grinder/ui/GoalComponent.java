
package com.grinder.ui;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class GoalComponent extends JPanel
{
    private static final int GAP = 8;
    private static final int ICON_SIZE = 24;
    private static final int CORNER_RADIUS = 10;
    private static final int OUTER_MARGIN = 2;
    private static final int INNER_PADDING = 10;
    private static final int PROGRESS_BAR_HEIGHT = 6;

    private static final Color CARD_BACKGROUND = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color CARD_BORDER = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color PROGRESS_BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    private static final Color PROGRESS_FOREGROUND = ColorScheme.PROGRESS_COMPLETE_COLOR;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final JPanel card = new JPanel()
    {
        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CARD_BACKGROUND);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
            g.setColor(CARD_BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
            g.dispose();
        }
    };

    private final JPanel header = new JPanel(new BorderLayout(GAP, 0));
    private final JPanel headerText = new JPanel();
    private final JPanel progressPanel = new JPanel();
    private final JPanel progressTextPanel = new JPanel(new BorderLayout(GAP, 0));
    private final JPanel progressBarArea = new JPanel(new BorderLayout());
    private final JPanel content = new JPanel();
    private final JPanel footer = new JPanel(new BorderLayout());

    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JLabel progressLabel = new JLabel();
    private final JButton expandButton = new JButton("\u25B8");
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean expanded;

    public GoalComponent()
    {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(OUTER_MARGIN, OUTER_MARGIN, OUTER_MARGIN, OUTER_MARGIN));

        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(INNER_PADDING, INNER_PADDING, INNER_PADDING, INNER_PADDING));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, GAP, 0));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.setPreferredSize(new Dimension(1, 40));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));


        iconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setMinimumSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setMaximumSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.setAlignmentY(Component.CENTER_ALIGNMENT);

        titleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        dateLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        dateLabel.setFont(FontManager.getRunescapeSmallFont());
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(3));
        headerText.add(dateLabel);

        expandButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        expandButton.setFont(FontManager.getRunescapeBoldFont());
        expandButton.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        expandButton.setFocusPainted(false);
        expandButton.setContentAreaFilled(false);
        expandButton.setOpaque(false);
        expandButton.setFocusable(false);
        expandButton.setToolTipText("Expand goal");
        expandButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        expandButton.setPreferredSize(new Dimension(26, 28));
        expandButton.addActionListener(e -> toggleExpanded());
        expandButton.setForeground(new Color(195, 205, 220));

        header.add(iconLabel, BorderLayout.WEST);
        header.add(headerText, BorderLayout.CENTER);
        header.add(expandButton, BorderLayout.EAST);

        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        progressPanel.setOpaque(false);
        progressPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, GAP, 0));
        progressPanel.setVisible(false);

        progressTextPanel.setOpaque(false);
        progressTextPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressTextPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        progressLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        progressLabel.setFont(FontManager.getRunescapeSmallFont());

        progressTextPanel.add(progressLabel, BorderLayout.WEST);

        progressBarArea.setOpaque(false);
        progressBarArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        progressBarArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBarArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, PROGRESS_BAR_HEIGHT + 5));

        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);
        progressBar.setForeground(PROGRESS_FOREGROUND);
        progressBar.setBackground(PROGRESS_BACKGROUND);
        progressBar.setOpaque(true);
        progressBar.setPreferredSize(new Dimension(100, PROGRESS_BAR_HEIGHT));
        progressBar.setMinimumSize(new Dimension(1, PROGRESS_BAR_HEIGHT));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, PROGRESS_BAR_HEIGHT));

        progressBarArea.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(progressTextPanel);
        progressPanel.add(progressBarArea);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.setBorder(BorderFactory.createEmptyBorder(GAP, 0, 0, 0));

        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(header);
        card.add(progressPanel);
        card.add(content);
        card.add(footer);

        add(card, BorderLayout.CENTER);

        MouseAdapter headerClick = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if (event.getButton() == MouseEvent.BUTTON1)
                {
                    toggleExpanded();
                }
            }
        };

        installHeaderClick(header, headerClick);
        setExpanded(false);
    }

    private void installHeaderClick(Component component, MouseAdapter listener)
    {
        if (component == expandButton)
        {
            return;
        }

        component.addMouseListener(listener);

        if (component instanceof java.awt.Container)
        {
            for (Component child : ((java.awt.Container) component).getComponents())
            {
                installHeaderClick(child, listener);
            }
        }
    }

    private void toggleExpanded()
    {
        setExpanded(!expanded);
    }

    public JPanel getContent()
    {
        return content;
    }

    public JPanel getFooter()
    {
        return footer;
    }

    public void setTitle(String title)
    {
        titleLabel.setText(title == null ? "" : title);
        revalidate();
        repaint();
    }

    public void setIcon(BufferedImage image)
    {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0)
        {
            iconLabel.setIcon(null);
        }
        else
        {
            double scale = Math.min((double) ICON_SIZE / image.getWidth(), (double) ICON_SIZE / image.getHeight());
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaledImage));
        }

        revalidate();
        repaint();
    }

    public void setStartDate(LocalDateTime startDate)
    {
        this.startDate = startDate;
        updateDateLabel();
    }

    public void setEndDate(LocalDateTime endDate)
    {
        this.endDate = endDate;
        updateDateLabel();
    }

    private void updateDateLabel()
    {
        if (startDate == null && endDate == null)
        {
            dateLabel.setText("");
        }
        else if (startDate == null)
        {
            dateLabel.setText("Ends: " + DATE_FORMAT.format(endDate));
        }
        else if (endDate == null)
        {
            dateLabel.setText("Started: " + DATE_FORMAT.format(startDate));
        }
        else
        {
            dateLabel.setText(DATE_FORMAT.format(startDate) + " \u2013 " + DATE_FORMAT.format(endDate));
        }

        dateLabel.setVisible(!dateLabel.getText().isEmpty());
        revalidate();
        repaint();
    }

    public void setProgress(long current, long maximum)
    {
        String text = maximum > 0 ? String.format("%,d / %,d", current, maximum) : "";
        setProgress(current, maximum, text);
    }

    public void setProgress(long current, long maximum, String text)
    {
        if (maximum <= 0)
        {
            setProgressStatus(text);
            return;
        }

        double ratio = Math.max(0.0, Math.min(1.0, (double) current / maximum));
        int percentage = (int) Math.round(ratio * 100.0);

        progressLabel.setText(text == null ? "" : text);
        progressLabel.setToolTipText(text);
        progressBar.setValue(percentage);
        progressBar.setToolTipText(percentage + "% complete");

        progressBarArea.setVisible(true);
        progressPanel.setVisible(true);

        revalidate();
        repaint();
    }

    public void setProgressStatus(String text)
    {
        progressLabel.setText(text == null ? "" : text);
        progressLabel.setToolTipText(text);
        progressBarArea.setVisible(false);
        progressPanel.setVisible(text != null && !text.trim().isEmpty());

        revalidate();
        repaint();
    }

    public void setExpanded(boolean expanded)
    {
        this.expanded = expanded;
        content.setVisible(expanded);
        footer.setVisible(expanded);
        expandButton.setText(expanded ? "\u25BE" : "\u25B8");
        expandButton.setToolTipText(expanded ? "Collapse goal" : "Expand goal");

        card.revalidate();
        card.repaint();
        revalidate();
        repaint();
    }
}
