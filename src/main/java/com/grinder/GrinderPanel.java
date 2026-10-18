package com.grinder;

import com.grinder.content.goal.Goal;
import com.grinder.repository.GrinderProfile;
import com.grinder.ui.GoalComponent;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.List;

public final class GrinderPanel extends PluginPanel
{
    private final Client client;
    private final JPanel main = new JPanel();
    private final JScrollPane scroll;
    private GrinderProfile profile;

    @Inject
    GrinderPanel(Client client)
    {
        this.client = client;
        this.scroll = new JScrollPane(main, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        init();
    }

    private void init()
    {
        setLayout(new BorderLayout(0, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder());

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel title = new JLabel("Grind Tracker", SwingConstants.LEFT);
        title.setForeground(ColorScheme.BRAND_ORANGE);
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));

        JButton add = new JButton("+ Goal");
        add.setFont(FontManager.getRunescapeSmallFont());
        add.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        add.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        add.setOpaque(true);
        add.setContentAreaFilled(true);
        add.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        add.setFocusPainted(false);
        add.setFocusable(false);
        add.setMargin(new Insets(0, 0, 0, 0));
        add.setToolTipText("Add a goal");
        // add.addActionListener(e -> plugin.addGoalDialog());

        header.add(title, BorderLayout.CENTER);
        header.add(add, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(ColorScheme.DARK_GRAY_COLOR);
        main.setOpaque(true);
        main.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.setOpaque(true);
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollBar vertical = scroll.getVerticalScrollBar();
        vertical.setUnitIncrement(16);
        vertical.setPreferredSize(new Dimension(8, 0));
        vertical.setBorder(BorderFactory.createEmptyBorder());
        vertical.setBackground(ColorScheme.DARK_GRAY_COLOR);
        vertical.setUI(new BasicScrollBarUI()
        {
            @Override
            protected void configureScrollBarColors()
            {
                thumbColor = ColorScheme.MEDIUM_GRAY_COLOR;
                trackColor = ColorScheme.DARK_GRAY_COLOR;
            }

            @Override
            protected JButton createDecreaseButton(int orientation)
            {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation)
            {
                return createZeroButton();
            }

            private JButton createZeroButton()
            {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                button.setFocusable(false);
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setOpaque(false);
                return button;
            }
        });

        add(scroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(300, 500));
    }

    void render(List<Goal> goals)
    {
        main.removeAll();

        JLabel summary = new JLabel("Goals: " + goals.size());
        summary.setFont(FontManager.getRunescapeSmallFont());
        summary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        summary.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(summary);

        for (Goal goal : goals)
        {
            GoalComponent card = goal.build(client);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Allow the card to grow when expanded instead of freezing its collapsed height.
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            main.add(card);
            main.add(Box.createVerticalStrut(8));
        }

        main.revalidate();
        main.repaint();
        scroll.revalidate();
        scroll.repaint();
    }

    public void load(GrinderProfile profile)
    {
        this.profile = profile;

        if (client.getLocalPlayer() != null)
        {
            String message = client.getLocalPlayer().getName() + " has logged in";
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
        }

        render(profile.getGoals());
    }

    public void unload()
    {
        profile = null;
        main.removeAll();
        main.revalidate();
        main.repaint();
    }
}