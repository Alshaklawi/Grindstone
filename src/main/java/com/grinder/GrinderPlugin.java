package com.grinder;

import com.grinder.content.goal.ClientCompletionGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;

import com.grinder.ui.GrinderPanel;

import com.google.inject.Provides;
import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
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
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@Slf4j
@PluginDescriptor( internalName = "grinder", name = "Grinder", description = "Track collection log progress and personal goals", tags = {"collection", "goals", "tracker"} )
public class GrinderPlugin extends Plugin {
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
    private boolean completionProgressDirty;
    private boolean storageProgressDirty;
    private long lastRecurringRefresh;

    @Override
    protected void startUp() {
        com.grinder.content.combatachievement.CombatAchievementData.clearCache();
        try {
            GrinderRepository.load();
        } catch (IOException e) {
            log.error("Unable to load Grinder profiles", e);
        }

        navigationButton = NavigationButton.builder()
                .tooltip("Grinder")
                .icon(loadIcon())
                .priority(5)
                .panel(panel)
                .build();

        if (config.enabled()) {
            clientToolbar.addNavigation(navigationButton);
        }

        if (client.getGameState() == GameState.LOGGED_IN && config.enabled()) {
            loadProfile();
        }
    }

    @Override
    protected void shutDown() {
        if (config.saveOnShutdown()) {
            saveProfile();
        }
        panel.unload();
        setNavigationVisible(false);
        navigationButton = null;
        profile = null;
        completionProgressDirty = false;
        storageProgressDirty = false;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();

        if (state == GameState.LOGGED_IN && config.enabled()) {
            loadProfile();
        } else if (state == GameState.LOGIN_SCREEN || state == GameState.CONNECTION_LOST) {
            onLogout();
        }
    }

    @Subscribe
    public void onConfigChanged(net.runelite.client.events.ConfigChanged event) {
        if (!"grinder".equals(event.getGroup())) {
            return;
        }

        if (!config.enabled()) {
            saveProfile();
            panel.unload();
            setNavigationVisible(false);
            return;
        }

        setNavigationVisible(true);

        if (client.getGameState() == GameState.LOGGED_IN && (profile == null || "enabled".equals(event.getKey()))) {
            loadProfile();
        } else {
            panel.render();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (!hasActivePlayerProfile()) {
            return;
        }

        ItemContainer container = event.getItemContainer();

        if (container == null) {
            return;
        }

        profile.getCollectionLogHistory().observe(container);
        storageProgressDirty |= profile.getPlayerStorage().observe(client, container);

        for (Goal goal : profile.getGoals()) {
            if (goal == null || goal instanceof PossessedItemGoal || !goal.isAffectedBy(container)) {
                continue;
            }

            goal.onItemContainerUpdated(client, container);
            panel.refreshGoal(goal);
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (!hasActivePlayerProfile()) {
            return;
        }

        completionProgressDirty = true;
        for (Goal goal : profile.getGoals()) {
            if (goal instanceof TrackerGoal && goal.refresh(client)) {
                panel.refreshGoal(goal);
            }
        }
    }

    @Subscribe
    public void onGameTick(net.runelite.api.events.GameTick event) {
        if (hasActivePlayerProfile() && System.currentTimeMillis() - lastRecurringRefresh >= 30_000) {
            lastRecurringRefresh = System.currentTimeMillis();
            if (profile.getGoals().stream().anyMatch(goal -> profile.metadata(goal).recurring().isActive())) { panel.refreshAll(); }
        }
        if ((!completionProgressDirty && !storageProgressDirty) || !hasActivePlayerProfile()) { return; }
        boolean variablesChanged = completionProgressDirty;
        boolean storageChanged = storageProgressDirty;
        completionProgressDirty = false;
        storageProgressDirty = false;
        if (variablesChanged || storageChanged) { profile.getPlayerStorage().refreshVariables(client); }
        boolean changed = storageChanged;
        for (Goal goal : profile.getGoals()) {
            if ((variablesChanged && goal instanceof ClientCompletionGoal) || goal instanceof PossessedItemGoal) {
                changed |= goal.refresh(client);
            }
        }
        if (changed) { panel.refreshAll(); }
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (!hasActivePlayerProfile()) {
            return;
        }

        boolean changed = false;

        for (Goal goal : profile.getGoals()) {
            if ((goal instanceof SkillLevelGoal && ((SkillLevelGoal) goal).getSkill() == event.getSkill()) || (goal instanceof SkillExperienceGoal && ((SkillExperienceGoal) goal).getSkill() == event.getSkill()) || goal instanceof TotalLevelGoal || goal instanceof TotalExperienceGoal || goal instanceof CombatLevelGoal) {
                changed |= goal.refresh(client);
            }
        }

        if (changed) {
            panel.refreshAll();
        }
    }

    private boolean hasActivePlayerProfile() {
        return config.enabled() && profile != null && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && profile.getHash() == client.getAccountHash();
    }

    private void loadProfile() {
        clientThread.invokeLater(() -> {
            if (!config.enabled() || client.getGameState() != GameState.LOGGED_IN) {
                return true;
            }
            if (client.getLocalPlayer() == null || client.getAccountHash() == 0) {
                return false;
            }
            loadReadyProfile();
            return true;
        });
    }

    private void loadReadyProfile() {
        if (!config.enabled() || client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        try {
            if (profile != null && profile.getHash() != client.getAccountHash()) {
                saveProfile();
                profile = null;
                panel.unload();
            }
            profile = GrinderRepository.load(client, itemManager);
            profile.getPlayerStorage().reset();
            profile.getPlayerStorage().captureLoaded(client);
            profile.getCollectionLogHistory().observe(client.getItemContainer(net.runelite.api.gameval.InventoryID.COLLECTION_TRANSMIT));
            for (Goal goal : profile.getGoals()) {
                goal.refresh(client);
            }
            panel.load(profile);
            setNavigationVisible(true);

            log.debug("Loaded Grinder profile with {} goals", profile.getGoals().size());

            for (Goal goal : profile.getGoals()) {
                if (goal instanceof CollectionLogEntryGoal) {
                    CollectionLogEntryGoal collectionGoal = (CollectionLogEntryGoal) goal;
                    log.debug("Loaded collection log goal: {}", collectionGoal.getTitle(client));
                }
            }
        } catch (IOException e) {
            log.error("Unable to load Grinder profile", e);
            profile = null;
            panel.unload();
        }
    }

    private void onLogout() {
        completionProgressDirty = false;
        storageProgressDirty = false;
        if (profile != null) { profile.getPlayerStorage().reset(); }
        if (config.saveOnLogout()) {
            saveProfile();
        }
        profile = null;
        panel.unload();
    }

    private void saveProfile() {
        if (profile == null) {
            return;
        }

        try {
            GrinderRepository.save(profile);
        } catch (IOException e) {
            log.error("Unable to save Grinder profile", e);
        }
    }

    private void setNavigationVisible(boolean visible) {
        if (navigationButton == null) {
            return;
        }

        if (visible) {
            clientToolbar.addNavigation(navigationButton);
        } else {
            clientToolbar.removeNavigation(navigationButton);
        }
    }

    private BufferedImage loadIcon() {
        return net.runelite.client.util.ImageUtil.loadImageResource(getClass(), "/grinder.png");
    }

    @Provides
    GrinderConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GrinderConfig.class);
    }
}
