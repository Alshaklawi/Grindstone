
package com.grinder;

import com.google.inject.Provides;
import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogItemGoal;
import com.grinder.repository.GrinderProfile;
import com.grinder.repository.GrinderRepository;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
        name = "Grinder",
        description = "Track collection log progress and personal goals",
        tags = {"collection", "goals", "tracker"}
)
public class GrinderPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    @Inject
    private GrinderConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ClientThread clientThread;

    @Inject
    private GrinderPanel panel;

    private NavigationButton navigationButton;
    private GrinderProfile profile;

    @Override
    protected void startUp()
    {
        try
        {
            GrinderRepository.load();
        }
        catch (IOException e)
        {
            log.error("Unable to load Grinder profiles", e);
        }

        navigationButton = NavigationButton.builder()
                .tooltip("Grinder")
                .icon(loadIcon())
                .priority(5)
                .panel(panel)
                .build();

        if (config.enabled())
        {
            clientToolbar.addNavigation(navigationButton);
        }

        if (client.getGameState() == GameState.LOGGED_IN && config.enabled())
        {
            loadProfile();
        }
    }

    @Override
    protected void shutDown()
    {
        if (config.saveOnShutdown())
        {
            saveProfile();
        }
        panel.unload();
        setNavigationVisible(false);
        navigationButton = null;
        profile = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        GameState state = event.getGameState();

        if (state == GameState.LOGGED_IN && config.enabled())
        {
            loadProfile();
        }
        else if (state == GameState.LOGIN_SCREEN || state == GameState.CONNECTION_LOST)
        {
            onLogout();
        }
    }

    @Subscribe
    public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
    {
        if (!"grinder".equals(event.getGroup()))
        {
            return;
        }

        if (!config.enabled())
        {
            saveProfile();
            panel.unload();
            setNavigationVisible(false);
            return;
        }

        setNavigationVisible(true);

        if (client.getGameState() == GameState.LOGGED_IN && profile == null)
        {
            loadProfile();
        }
        else
        {
            panel.render();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (profile == null || !config.enabled())
        {
            return;
        }

        ItemContainer container = event.getItemContainer();

        if (container == null)
        {
            return;
        }

        for (Goal goal : profile.getGoals())
        {
            if (goal == null || !goal.isAffectedBy(container))
            {
                continue;
            }

            goal.onItemContainerUpdated(client, container);
            panel.refreshGoal(goal);
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (profile == null || !config.enabled())
        {
            return;
        }

        for (Goal goal : profile.getGoals())
        {
            if (goal instanceof TrackerGoal && ((TrackerGoal) goal).refresh(client))
            {
                panel.refreshGoal(goal);
            }
        }
    }

    private void loadProfile()
    {
        if (!config.enabled() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        try
        {
            profile = GrinderRepository.load(client, itemManager);
            panel.load(profile);
            setNavigationVisible(true);

            log.debug("Loaded Grinder profile with {} goals", profile.getGoals().size());

            for (Goal goal : profile.getGoals())
            {
                if (goal instanceof CollectionLogEntryGoal)
                {
                    CollectionLogEntryGoal collectionGoal = (CollectionLogEntryGoal) goal;
                    log.debug("Loaded collection log goal: {}", collectionGoal.getTitle(client));
                }
            }
        }
        catch (IOException e)
        {
            log.error("Unable to load Grinder profile", e);
            profile = null;
            panel.unload();
        }
    }

    private void onLogout()
    {
        if (config.saveOnLogout())
        {
            saveProfile();
        }
        profile = null;
        panel.unload();
    }

    private void saveProfile()
    {
        if (profile == null)
        {
            return;
        }

        try
        {
            GrinderRepository.save(profile);
        }
        catch (IOException e)
        {
            log.error("Unable to save Grinder profile", e);
        }
    }

    private void setNavigationVisible(boolean visible)
    {
        if (navigationButton == null)
        {
            return;
        }

        if (visible)
        {
            clientToolbar.addNavigation(navigationButton);
        }
        else
        {
            clientToolbar.removeNavigation(navigationButton);
        }
    }

    private BufferedImage loadIcon()
    {
        return net.runelite.client.util.ImageUtil.loadImageResource(getClass(), "/grinder.png");
    }

    @Provides
    GrinderConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GrinderConfig.class);
    }
}
