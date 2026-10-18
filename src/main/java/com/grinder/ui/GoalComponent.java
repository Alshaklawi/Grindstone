
package com.grinder.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import net.runelite.client.ui.ColorScheme;

public class GoalComponent extends JPanel
{
    private static final Color CARD_COLOR = new Color(45, 45, 45);
    private static final Color BORDER_COLOR = new Color(65, 65, 65);
    private static final Color TEXT_COLOR = new Color(235, 235, 235);
    private static final Color MUTED_COLOR = new Color(175, 175, 175);
    private static final Color PROGRESS_COLOR = new Color(65, 160, 100);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JPanel header = new JPanel(new BorderLayout(8, 0));
    private final JPanel headerText = new JPanel();
    private final JPanel content = new JPanel();
    private final JPanel footer = new JPanel(new BorderLayout(6, 0));
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JLabel progressLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JButton expandButton = new JButton("−");

    private String title = "";
    private BufferedImage icon;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private long progressCurrent;
    private long progressMaximum;
    private String progressText = "";
    private String progressStatus = "";
    private boolean expanded = true;
    private boolean iconVisible = true;
    private boolean progressBarVisible = true;

    public GoalComponent()
    {
        super(new BorderLayout(0, 6));
        setOpaque(true);
        setBackground(CARD_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 9, 8, 9)));

        header.setOpaque(false);
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);

        dateLabel.setForeground(MUTED_COLOR);
        dateLabel.setFont(dateLabel.getFont().deriveFont(10f));

        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(24, 24));

        expandButton.setFocusable(false);
        expandButton.setBorderPainted(false);
        expandButton.setContentAreaFilled(false);
        expandButton.setForeground(MUTED_COLOR);
        expandButton.setPreferredSize(new Dimension(22, 22));
        expandButton.addActionListener(event -> setExpanded(!expanded));

        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(dateLabel);

        header.add(iconLabel, BorderLayout.WEST);
        header.add(headerText, BorderLayout.CENTER);
        header.add(expandButton, BorderLayout.EAST);

        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        progressBar.setForeground(PROGRESS_COLOR);
        progressBar.setPreferredSize(new Dimension(100, 7));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 7));

        progressLabel.setForeground(MUTED_COLOR);
        progressLabel.setFont(progressLabel.getFont().deriveFont(10f));

        footer.setOpaque(false);
        footer.add(progressBar, BorderLayout.CENTER);
        footer.add(progressLabel, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        refreshLayout();
    }

    public JPanel getContent()
    {
        return content;
    }

    public void update(String title, BufferedImage icon, LocalDateTime startDate, LocalDateTime endDate,
                       long current, long maximum, String progressText, String status)
    {
        this.title = title == null ? "" : title;
        this.icon = icon;
        this.startDate = startDate;
        this.endDate = endDate;
        this.progressCurrent = Math.max(0, current);
        this.progressMaximum = Math.max(0, maximum);
        this.progressText = progressText == null ? "" : progressText;
        this.progressStatus = status == null ? "" : status;

        applyValues();
        refreshLayout();
    }

    public void setTitle(String title)
    {
        String next = title == null ? "" : title;

        if (Objects.equals(this.title, next))
        {
            return;
        }

        this.title = next;
        titleLabel.setText(next);
        revalidate();
        repaint();
    }

    public void setIcon(BufferedImage icon)
    {
        if (this.icon == icon)
        {
            return;
        }

        this.icon = icon;
        iconLabel.setIcon(icon == null ? null : new ImageIcon(icon));
        iconLabel.setVisible(iconVisible && icon != null);
        revalidate();
        repaint();
    }

    public void setStartDate(LocalDateTime startDate)
    {
        if (Objects.equals(this.startDate, startDate))
        {
            return;
        }

        this.startDate = startDate;
        updateDates();
    }

    public void setEndDate(LocalDateTime endDate)
    {
        if (Objects.equals(this.endDate, endDate))
        {
            return;
        }

        this.endDate = endDate;
        updateDates();
    }

    public void setProgress(long current, long maximum, String text)
    {
        long nextCurrent = Math.max(0, current);
        long nextMaximum = Math.max(0, maximum);
        String nextText = text == null ? "" : text;

        if (progressCurrent == nextCurrent && progressMaximum == nextMaximum && Objects.equals(progressText, nextText))
        {
            return;
        }

        progressCurrent = nextCurrent;
        progressMaximum = nextMaximum;
        progressText = nextText;
        progressStatus = "";
        updateProgress();
    }

    public void setProgressStatus(String status)
    {
        String next = status == null ? "" : status;

        if (Objects.equals(progressStatus, next))
        {
            return;
        }

        progressStatus = next;
        progressText = next;
        updateProgress();
    }

    public void setIconVisible(boolean visible)
    {
        if (iconVisible == visible)
        {
            return;
        }

        iconVisible = visible;
        iconLabel.setVisible(visible && icon != null);
        revalidate();
        repaint();
    }

    public void setProgressBarVisible(boolean visible)
    {
        if (progressBarVisible == visible)
        {
            return;
        }

        progressBarVisible = visible;
        progressBar.setVisible(visible);
        revalidate();
        repaint();
    }

    public void setExpanded(boolean expanded)
    {
        if (this.expanded == expanded)
        {
            return;
        }

        this.expanded = expanded;
        content.setVisible(expanded);
        footer.setVisible(expanded);
        expandButton.setText(expanded ? "−" : "+");
        revalidate();
        repaint();
    }

    private void applyValues()
    {
        titleLabel.setText(title);
        iconLabel.setIcon(icon == null ? null : new ImageIcon(icon));
        iconLabel.setVisible(iconVisible && icon != null);
        updateDates();
        updateProgress();
        content.setVisible(expanded);
        footer.setVisible(expanded);
        expandButton.setText(expanded ? "−" : "+");
        progressBar.setVisible(progressBarVisible);
    }

    private void updateDates()
    {
        String start = startDate == null ? "—" : DATE_FORMAT.format(startDate);
        String end = endDate == null ? "—" : DATE_FORMAT.format(endDate);
        dateLabel.setText(start + "  →  " + end);
        revalidate();
        repaint();
    }

    private void updateProgress()
    {
        int value = progressMaximum <= 0 ? 0 : (int) Math.min(100, progressCurrent * 100.0 / progressMaximum);
        progressBar.setValue(value);
        progressLabel.setText(progressText);
        progressBar.setVisible(progressBarVisible);
        revalidate();
        repaint();
    }

    private void refreshLayout()
    {
        applyValues();
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D g = (Graphics2D) graphics.create();

        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }
        finally
        {
            g.dispose();
        }
    }
}
