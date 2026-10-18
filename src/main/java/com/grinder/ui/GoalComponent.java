package com.grinder.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public final class GoalComponent extends JPanel
{
    private static final int GAP = 8;
    private static final int ICON_SIZE = 24;
    private static final int CORNER_RADIUS = 10;
    private static final int INNER_PADDING = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final Color CARD_COLOR = new Color(45, 45, 45);
    private static final Color BORDER_COLOR = new Color(65, 65, 65);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color MUTED_COLOR = new Color(165, 165, 165);

    private final JPanel header = new JPanel(new BorderLayout(GAP, 0));
    private final JPanel headerText = new JPanel(new BorderLayout());
    private final JPanel content = new JPanel(new BorderLayout());
    private final JPanel footer = new JPanel(new BorderLayout(GAP, 4));
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JLabel progressLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JButton expandButton = new JButton("−");

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean expanded;
    private boolean iconVisible = true;
    private boolean progressBarVisible = true;

    public GoalComponent()
    {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(INNER_PADDING, INNER_PADDING, INNER_PADDING, INNER_PADDING));

        iconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));

        dateLabel.setForeground(MUTED_COLOR);
        dateLabel.setFont(dateLabel.getFont().deriveFont(10f));

        headerText.setOpaque(false);
        headerText.add(titleLabel, BorderLayout.NORTH);
        headerText.add(dateLabel, BorderLayout.SOUTH);

        expandButton.setFocusable(false);
        expandButton.setBorderPainted(false);
        expandButton.setContentAreaFilled(false);
        expandButton.setForeground(MUTED_COLOR);
        expandButton.setFont(expandButton.getFont().deriveFont(Font.PLAIN, 14f));
        expandButton.addActionListener(event -> setExpanded(!expanded));

        header.add(iconLabel, BorderLayout.WEST);
        header.add(headerText, BorderLayout.CENTER);
        header.add(expandButton, BorderLayout.EAST);

        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(0, INNER_PADDING, 0, INNER_PADDING));

        progressLabel.setForeground(TEXT_COLOR);
        progressLabel.setFont(progressLabel.getFont().deriveFont(11f));

        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(100, 6));
        progressBar.setBorderPainted(false);
        progressBar.setOpaque(false);

        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, INNER_PADDING, INNER_PADDING, INNER_PADDING));
        footer.add(progressLabel, BorderLayout.NORTH);
        footer.add(progressBar, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setExpanded(false);
    }

    public JPanel getContent() { return content; }

    public JPanel getFooter() { return footer; }

    public void setTitle(String title)
    {
        titleLabel.setText(title == null ? "" : title);
        refreshLayout();
    }

    public void setIcon(BufferedImage image)
    {
        iconLabel.setIcon(image == null ? null : new javax.swing.ImageIcon(image.getScaledInstance(ICON_SIZE, ICON_SIZE, java.awt.Image.SCALE_SMOOTH)));
        iconLabel.setVisible(iconVisible && image != null);
        refreshLayout();
    }

    public void setIconVisible(boolean visible)
    {
        iconVisible = visible;
        iconLabel.setVisible(visible && iconLabel.getIcon() != null);
        refreshLayout();
    }

    public void setProgressBarVisible(boolean visible)
    {
        progressBarVisible = visible;
        progressBar.setVisible(visible);
        refreshLayout();
    }

    public void setStartDate(LocalDateTime date)
    {
        startDate = date;
        updateDateLabel();
    }

    public void setEndDate(LocalDateTime date)
    {
        endDate = date;
        updateDateLabel();
    }

    private void updateDateLabel()
    {
        if (startDate == null && endDate == null)
        {
            dateLabel.setText("");
        }
        else if (startDate != null && endDate != null)
        {
            dateLabel.setText(DATE_FORMAT.format(startDate) + " – " + DATE_FORMAT.format(endDate));
        }
        else
        {
            dateLabel.setText(DATE_FORMAT.format(startDate != null ? startDate : endDate));
        }

        refreshLayout();
    }

    public void setProgress(long current, long maximum)
    {
        setProgress(current, maximum, current + " / " + maximum);
    }

    public void setProgress(long current, long maximum, String text)
    {
        progressLabel.setText(text == null ? "" : text);
        progressBar.setValue(maximum <= 0 ? 0 : (int) Math.max(0, Math.min(100, current * 100.0 / maximum)));
        progressBar.setVisible(progressBarVisible && maximum > 0);
        progressLabel.setVisible(true);
        refreshLayout();
    }

    public void setProgressStatus(String text)
    {
        progressLabel.setText(text == null ? "" : text);
        progressBar.setVisible(false);
        progressLabel.setVisible(text != null && !text.trim().isEmpty());
        refreshLayout();
    }

    public void setExpanded(boolean expanded)
    {
        this.expanded = expanded;
        content.setVisible(expanded);
        expandButton.setText(expanded ? "−" : "+");
        refreshLayout();
    }

    private void refreshLayout()
    {
        invalidate();

        Dimension preferred = getPreferredSize();
        setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));

        revalidate();
        repaint();

        if (getParent() != null)
        {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CARD_COLOR);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
            g.setColor(BORDER_COLOR);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
        }
        finally
        {
            g.dispose();
        }
    }
}