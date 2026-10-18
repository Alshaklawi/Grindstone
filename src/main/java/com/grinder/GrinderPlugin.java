package com.grinder;

import com.google.inject.Provides;
import com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal;
import com.grinder.content.utils.Icon;
import com.grinder.repository.GrinderProfile;
import com.grinder.repository.GrinderRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 * Grinder plugin entry point.
 *
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@Slf4j
@PluginDescriptor(name = "Grinder")
public class GrinderPlugin extends Plugin {
    @Inject
    private GrinderPanel panel;

    @Inject
    private Client client;

    @Getter
    @Inject
    private GrinderConfig config;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    private NavigationButton navigation;
    private GrinderProfile profile;

    @Override
    protected void startUp() throws Exception {
        log.debug("Grinder started!");

        GrinderRepository.load();

        navigation = NavigationButton.builder()
                .tooltip("Grinder")
                .icon(Icon.MAIN.getImage())
                .priority(8)
                .panel(panel)
                .build();

        setNavigationVisible(true);
    }

    @Override
    protected void shutDown() throws Exception {
        log.debug("Grinder stopped!");
        setNavigationVisible(false);
        navigation = null;
        panel.unload();
        profile = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();
        if (state == GameState.LOGGED_IN) {
            clientThread.invokeLater(() -> {
                if (client.getGameState() != GameState.LOGGED_IN) {
                    return;
                }
                load();
            });
        } else if (state == GameState.LOGIN_SCREEN || state == GameState.CONNECTION_LOST) {
            onLogout();
        }
    }

    private void load() {
        try {
            profile = GrinderRepository.load(client);
            onLogin();
        } catch (Exception e) {
            log.error("Unable to load Grinder profile.", e);
        }
    }

    @Provides
    GrinderConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GrinderConfig.class);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted) {
    }

    @Subscribe(priority = 100)
    private void onClientShutdown(ClientShutdown event) {
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
    }

    @Subscribe
    private void onRuneScapeProfileChanged(RuneScapeProfileChanged event) {
    }

    private void onLogin() {
        log.debug("Logged in");

        require(profile -> {
            panel.load(profile);
            setNavigationVisible(true);

            for (final var goal : profile.getGoals()) {
                if (goal instanceof CollectionLogEntryGoal) {
                    CollectionLogEntryGoal cg = (CollectionLogEntryGoal) goal;
                    for (final var item : cg.getEntry().getItems(client)) {
                        log.debug("Collection log item: {}", item.getName());
                    }
                }
            }
        });
    }

    private void onLogout() {
        log.debug("Logged out");
        setNavigationVisible(false);
        panel.unload();
        profile = null;
    }

    private void setNavigationVisible(boolean visible) {
        if (navigation == null) {
            log.error("Unable to find navigation...");
            return;
        }

        if (visible) {
            clientToolbar.addNavigation(navigation);
        } else {
            clientToolbar.removeNavigation(navigation);
        }

        log.debug("Grinder navigation: {}", visible);
    }

    public void require(Consumer<GrinderProfile> consumer) {
        if (profile == null) {
            log.error("Unable to find goal profile.");
            return;
        }

        consumer.accept(profile);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        require(profile -> {
            profile.getGoals().forEach(goal -> goal.onItemContainerUpdated(client, event.getItemContainer()));
        });
    }
}
