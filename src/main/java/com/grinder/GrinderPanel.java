package com.grinder;

import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogItemGoal;
import com.grinder.repository.GrinderProfile;
import com.grinder.ui.GoalComponent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class GrinderPanel extends PluginPanel {
    private final Client client;
    private final GrinderConfig config;
    private final JPanel main = new JPanel();
    private final JScrollPane scroll;
    private GrinderProfile profile;

    @Inject
    public GrinderPanel(Client client, GrinderConfig config) {
        this.client = client;
        this.config = config;

        setLayout(new BorderLayout());

        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        scroll = new JScrollPane(main, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(300, 500));
    }

    public void load(GrinderProfile profile) {
        this.profile = profile;
        render();
    }

    public void unload() {
        profile = null;
        main.removeAll();
        main.revalidate();
        main.repaint();
    }

    public void render() {
        main.removeAll();

        if (profile == null || !config.enabled()) {
            main.revalidate();
            main.repaint();
            return;
        }

        List<Goal> visibleGoals = new ArrayList<>();

        for (Goal goal : profile.getGoals()) {
            if (!config.collectionLogEnabled() && isCollectionLogGoal(goal)) {
                continue;
            }

            visibleGoals.add(goal);
        }

        JLabel summary = new JLabel("Goals: " + visibleGoals.size());
        summary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        summary.setAlignmentX(LEFT_ALIGNMENT);
        main.add(summary);
        main.add(Box.createVerticalStrut(8));

        for (Goal goal : visibleGoals) {
            GoalComponent component = goal.build(client, config);
            component.setAlignmentX(LEFT_ALIGNMENT);
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            main.add(component);
            main.add(Box.createVerticalStrut(8));
        }

        main.revalidate();
        main.repaint();
    }

    private boolean isCollectionLogGoal(Goal goal) {
        return goal instanceof CollectionLogEntryGoal || goal instanceof CollectionLogItemGoal;
    }
}