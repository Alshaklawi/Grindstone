
package com.grinder;

import com.google.inject.Provides;
import com.grinder.config.GrinderConfig;
import com.grinder.repository.GrinderProfile;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal;
import com.grinder.repository.GrinderRepository;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(name = "Grinder")
public class GrinderPlugin extends Plugin
{
    @Inject
    private GrinderPanel panel;

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

    private NavigationButton navigation;
    private GrinderProfile profile;

    @Override
    protected void startUp()
    {
        log.info("Grinder started.");

        try
        {
            GrinderRepository.load();
        }
        catch (IOException ex)
        {
            log.error("Unable to load Grinder profiles.", ex);
        }

        navigation = NavigationButton.builder()
                .tooltip("Grinder")
                .icon(ImageUtil.loadImageResource(getClass(), "/grinder.png"))
                .priority(5)
                .panel(panel)
                .build();

        setNavigationVisible(config.enabled());

        if (client.getGameState() == GameState.LOGGED_IN && config.enabled())
        {
            load();
        }
    }

    @Override
    protected void shutDown()
    {
        if (config.saveOnShutdown())
        {
            saveProfile();
        }

        setNavigationVisible(false);
        panel.unload();
        navigation = null;
        profile = null;
        log.info("Grinder stopped.");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (!config.enabled())
        {
            return;
        }

        if (event.getGameState() == GameState.LOGGED_IN)
        {
            load();
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.CONNECTION_LOST)
        {
            onLogout();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
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
            load();
        }
        else
        {
            panel.render();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!config.enabled() || profile == null)
        {
            return;
        }

        for (Goal goal : profile.getGoals())
        {
            goal.onItemContainerUpdated(client, event.getItemContainer());
        }

        panel.render();
    }

    private void load()
    {
        if (!config.enabled())
        {
            return;
        }

        try
        {
            profile = GrinderRepository.load(client, itemManager);
            onLogin();
        }
        catch (IOException ex)
        {
            log.error("Unable to load Grinder profile.", ex);
        }
    }

    private void onLogin()
    {
        if (profile == null || !config.enabled())
        {
            return;
        }

        panel.load(profile);
        setNavigationVisible(true);

        for (Goal goal : profile.getGoals())
        {
            if (goal instanceof CollectionLogEntryGoal)
            {
                CollectionLogEntryGoal collectionLogGoal = (CollectionLogEntryGoal) goal;
                log.debug("Loaded collection log goal: {}", collectionLogGoal.getEntry());
            }
        }
    }

    private void onLogout()
    {
        if (config.saveOnLogout())
        {
            saveProfile();
        }

        panel.unload();
        profile = null;
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
        catch (IOException ex)
        {
            log.error("Unable to save Grinder profile.", ex);
        }
    }

    private void setNavigationVisible(boolean visible)
    {
        if (navigation == null)
        {
            return;
        }

        if (visible && config.enabled())
        {
            clientToolbar.addNavigation(navigation);
        }
        else
        {
            clientToolbar.removeNavigation(navigation);
        }
    }

    @Provides
    GrinderConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GrinderConfig.class);
    }
}
