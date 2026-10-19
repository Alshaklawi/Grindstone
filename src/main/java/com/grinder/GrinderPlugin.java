package com.grinder;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import com.grinder.config.GrinderConfig;
import com.grinder.content.collectionlog.CollectionLogPages;
import com.grinder.content.combatachievement.CombatAchievementData;
import com.grinder.content.goal.BaseSkillGoal;
import com.grinder.content.goal.ClientCompletionGoal;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTaskGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.tracker.TrackerEntry;
import com.grinder.content.goal.impl.experience.SessionExperienceGoal;
import com.grinder.content.goal.impl.experience.BaseExperienceGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.BaseLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.notification.GoalCompletionNotifications;
import com.grinder.notification.GoalCompletionOverlay;
import com.grinder.repository.GrinderProfile;
import com.grinder.repository.GrinderRepository;
import com.grinder.repository.ProfilePersistence;
import com.grinder.ui.GrinderPanel;
import com.grinder.ui.util.SkillWidgets;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@Slf4j
@PluginDescriptor( internalName = "grinder", legacyDataDirectory = "grinder", name = "Grindstone", description = "Track collection log progress and personal goals", tags = {"collection", "goals", "tracker"} )
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
    @Inject
    private GrinderRepository repository;
    @Inject
    private ProfilePersistence persistence;
    @Inject
    private GoalCompletionNotifications completionNotifications;
    @Inject
    private GoalCompletionOverlay completionOverlay;
    @Inject
    private OverlayManager overlayManager;

    private NavigationButton navigationButton;
    private volatile GrinderProfile profile;
    private final Map<Long, GrinderProfile> sessionProfiles = new ConcurrentHashMap<>();
    private final AtomicInteger loadGeneration = new AtomicInteger();
    private volatile boolean running;
    private volatile long loadingHash;
    private boolean completionProgressDirty;
    private boolean storageProgressDirty;
    private final Map<String, Integer> combatBossIds = new HashMap<>();
    private long lastRecurringRefresh;

    @Override
    protected void startUp() {
        running = true;
        completionNotifications.reset();
        overlayManager.add(completionOverlay);
        loadGeneration.incrementAndGet();
        sessionProfiles.clear();
        persistence.configure(this::getPluginDirectory);
        panel.setMigrationHandler(this::installMigratedProfile);
        CombatAchievementData.clearCache();
        combatBossIds.clear();

        navigationButton = NavigationButton.builder()
                .tooltip("Grindstone")
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
        running = false;
        overlayManager.remove(completionOverlay);
        completionNotifications.reset();
        loadGeneration.incrementAndGet();
        loadingHash = 0;
        if (config.saveOnShutdown()) {
            saveProfile();
        }
        panel.unload();
        setNavigationVisible(false);
        navigationButton = null;
        profile = null;
        completionProgressDirty = false;
        storageProgressDirty = false;
        combatBossIds.clear();
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
    public void onConfigChanged(ConfigChanged event) {
        if (!"grinder".equals(event.getGroup())) {
            return;
        }

        if (!config.enabled()) {
            completionNotifications.reset();
            saveProfile();
            loadGeneration.incrementAndGet();
            loadingHash = 0;
            profile = null;
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
    public void onMenuEntryAdded(MenuEntryAdded event) {
        MenuEntry original = event.getMenuEntry();
        if (!hasActivePlayerProfile()) {
            return;
        }
        if (original.getType() == MenuAction.EXAMINE_NPC) {
            addBossTrackerMenu(original);
            return;
        }
        if (original.getType() != MenuAction.CC_OP && original.getType() != MenuAction.CC_OP_LOW_PRIORITY) {
            return;
        }
        if (original.getParam1() >>> 16 == InterfaceID.STATS) {
            if (config.skillGoalMenu() && original.getIdentifier() == 1) {
                Skill skill = SkillWidgets.skillFor(original.getParam1());
                if (skill != null) {
                    addSkillGoalOption("Add level goal", skill.getName(), () -> new SkillLevelGoal(skill), false);
                    addSkillGoalOption("Add XP goal", skill.getName(), () -> new SkillExperienceGoal(skill), true);
                }
            }
            return;
        }
        if (original.getParam1() >>> 16 == InterfaceID.CA_BOSSES) {
            addBossAchievementMenu(original);
            return;
        }
        if (original.getParam1() >>> 16 == InterfaceID.CA_TASKS) {
            addCombatAchievementGoalMenu(original);
            return;
        }
        if (!config.collectionLogEnabled() || !config.collectionLogGoalMenu() || original.getParam1() >>> 16 != InterfaceID.COLLECTION || !"Check".equals(original.getOption())) {
            return;
        }
        int itemId = original.getItemId();
        if (itemId <= 0 && original.getWidget() != null) {
            itemId = original.getWidget().getItemId();
        }
        final int selectedItem = itemId;
        final String selectedName = Text.removeTags(original.getTarget()).trim();
        client.getMenu().createMenuEntry(-2)
                .setOption("Add goal")
                .setTarget(original.getTarget())
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (!hasActivePlayerProfile() || !config.collectionLogEnabled() || !config.collectionLogGoalMenu()) {
                        return;
                    }
                    Goal goal = null;
                    if (selectedItem > 0) {
                        goal = new CollectionLogItemGoal(CollectionLogPages.canonicalItemId(selectedItem), 1);
                    } else {
                        for (Map.Entry<Integer, String> page : CollectionLogPages.discover(client).entrySet()) {
                            if (page.getValue().equalsIgnoreCase(selectedName)) {
                                goal = new CollectionLogEntryGoal(itemManager, page.getKey(), page.getValue());
                                break;
                            }
                        }
                    }
                    if (goal != null) {
                        openGoalEditor(goal);
                    }
                });
    }

    private void addCombatAchievementGoalMenu(MenuEntry original) {
        if (!config.combatAchievementGoalsEnabled() || !config.combatAchievementGoalMenu() || !"Expand".equals(original.getOption()) && !"Collapse".equals(original.getOption())) {
            return;
        }
        String selectedName = Text.removeTags(original.getTarget()).trim();
        if (selectedName.isEmpty()) {
            return;
        }
        client.getMenu().createMenuEntry(-2)
                .setOption("Add goal")
                .setTarget(original.getTarget())
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (!hasActivePlayerProfile() || !config.combatAchievementGoalsEnabled() || !config.combatAchievementGoalMenu()) {
                        return;
                    }
                    for (CombatAchievementData.Task task : CombatAchievementData.tasks(client).values()) {
                        if (Text.removeTags(task.getName()).trim().equalsIgnoreCase(selectedName)) {
                            openGoalEditor(new CombatAchievementTaskGoal(task.getId(), task.getName(), task.getDescription()));
                            return;
                        }
                    }
                });
    }

    private void openGoalEditor(Goal selection) {
        GrinderProfile active = profile;
        SwingUtilities.invokeLater(() -> {
            if (running && config.enabled() && profile == active) {
                panel.showAddGoalDialog(selection);
            }
        });
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        if (!hasActivePlayerProfile() || !config.skillGoalMenu()) {
            return;
        }
        Widget total = client.getWidget(InterfaceID.Stats.TOTAL);
        Point mouse = client.getMouseCanvasPosition();
        if (total == null || total.isHidden() || !total.getBounds().contains(mouse.getX(), mouse.getY())) {
            return;
        }
        addSkillGoalOption("Add total level goal", "All skills", TotalLevelGoal::new, false);
        addSkillGoalOption("Add total XP goal", "All skills", TotalExperienceGoal::new, true);
        addSkillGoalOption("Add base level goal", "All skills", BaseLevelGoal::new, false);
        addSkillGoalOption("Add base XP goal", "All skills", BaseExperienceGoal::new, true);
    }

    private void addSkillGoalOption(String label, String target, Supplier<Goal> factory, boolean experience) {
        if (experience ? !config.experienceGoalsEnabled() : !config.levelGoalsEnabled()) {
            return;
        }
        client.getMenu().createMenuEntry(-1)
                .setOption(label)
                .setTarget(target)
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (hasActivePlayerProfile() && config.skillGoalMenu() && (experience ? config.experienceGoalsEnabled() : config.levelGoalsEnabled())) {
                        openGoalEditor(factory.get());
                    }
                });
    }

    private void addBossTrackerMenu(MenuEntry original) {
        if (!config.bossGoalMenu() || !config.trackerGoalsEnabled()) {
            return;
        }
        String name = original.getNpc() == null ? Text.removeTags(original.getTarget()).replaceFirst("\\s*\\(level-\\d+\\)\\s*$", "") : original.getNpc().getName();
        TrackerEntry tracker = TrackerEntry.forBossName(name);
        if (tracker == null) {
            return;
        }
        client.getMenu().createMenuEntry(-2)
                .setOption("Add goal")
                .setTarget(original.getTarget())
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (hasActivePlayerProfile() && config.bossGoalMenu() && config.trackerGoalsEnabled()) {
                        openGoalEditor(new TrackerGoal(tracker, tracker.getDefaultTarget()));
                    }
                });
    }

    private void addBossAchievementMenu(MenuEntry original) {
        if (!config.bossGoalMenu() || !config.combatAchievementGoalsEnabled() || original.getIdentifier() != 1) {
            return;
        }
        String name = Text.removeTags(original.getTarget()).trim();
        if (name.isEmpty()) {
            return;
        }
        if (combatBossIds.isEmpty()) {
            CombatAchievementData.tasksByBoss(client).forEach((boss, tasks) -> {
                if (!tasks.isEmpty()) {
                    combatBossIds.put(Text.removeTags(CombatAchievementData.bossName(client, boss)).trim().toLowerCase(Locale.ROOT), boss);
                }
            });
        }
        Integer boss = combatBossIds.get(name.toLowerCase(Locale.ROOT));
        if (boss == null) {
            return;
        }
        client.getMenu().createMenuEntry(-2)
                .setOption("Add goal")
                .setTarget(original.getTarget())
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (hasActivePlayerProfile() && config.bossGoalMenu() && config.combatAchievementGoalsEnabled()) {
                        openGoalEditor(new CombatAchievementBossGoal(boss, name));
                    }
                });
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
    public void onGameTick(GameTick event) {
        if (hasActivePlayerProfile() && System.currentTimeMillis() - lastRecurringRefresh >= 30_000) {
            lastRecurringRefresh = System.currentTimeMillis();
            if (profile.getGoals().stream().anyMatch(goal -> profile.metadata(goal).recurring().isActive() || profile.metadata(goal).getDeadlineAt() > 0)) {
                panel.refreshAll();
            }
        }
        if ((!completionProgressDirty && !storageProgressDirty) || !hasActivePlayerProfile()) {
            return;
        }
        boolean variablesChanged = completionProgressDirty;
        boolean storageChanged = storageProgressDirty;
        completionProgressDirty = false;
        storageProgressDirty = false;
        if (variablesChanged || storageChanged) {
            profile.getPlayerStorage().refreshVariables(client);
        }
        boolean changed = storageChanged;
        for (Goal goal : profile.getGoals()) {
            if ((variablesChanged && goal instanceof ClientCompletionGoal) || goal instanceof PossessedItemGoal) {
                changed |= goal.refresh(client);
            }
        }
        if (changed) {
            panel.refreshAll();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (!hasActivePlayerProfile()) {
            return;
        }

        boolean changed = false;

        for (Goal goal : profile.getGoals()) {
            if ((goal instanceof SkillLevelGoal && ((SkillLevelGoal) goal).getSkill() == event.getSkill()) || (goal instanceof SkillExperienceGoal && ((SkillExperienceGoal) goal).getSkill() == event.getSkill()) || goal instanceof TotalLevelGoal || goal instanceof TotalExperienceGoal || goal instanceof CombatLevelGoal || goal instanceof BaseSkillGoal || goal instanceof SessionExperienceGoal) {
                changed |= goal.refresh(client);
            }
        }

        if (changed) {
            panel.refreshAll();
        }
    }

    private boolean hasActivePlayerProfile() {
        return running && config.enabled() && profile != null && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && profile.getHash() == client.getAccountHash();
    }

    private void loadProfile() {
        clientThread.invokeLater(() -> {
            if (!running || !config.enabled() || client.getGameState() != GameState.LOGGED_IN) {
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
        if (!running || !config.enabled() || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) {
            return;
        }
        long hash = client.getAccountHash();
        if (hash == 0 || profile != null && profile.getHash() == hash || loadingHash == hash) {
            return;
        }
        if (profile != null) {
            saveProfile();
            profile = null;
            panel.unload();
        }
        int token = loadGeneration.incrementAndGet();
        loadingHash = 0;
        GrinderProfile cached = sessionProfiles.get(hash);
        if (cached != null) {
            activateProfile(cached);
            return;
        }
        loadingHash = hash;
        panel.setStorageStatus("Loading saved profile…");
        persistence.load(hash).whenComplete((loaded, failure) -> clientThread.invokeLater(() -> {
            if (!running || token != loadGeneration.get() || !config.enabled() || client.getGameState() != GameState.LOGGED_IN || client.getAccountHash() != hash || client.getLocalPlayer() == null) {
                return;
            }
            loadingHash = 0;
            if (failure != null) {
                log.warn("Unable to load Grindstone profile; existing saves were not replaced", failure);
                panel.setStorageStatus("Profile could not be loaded · saved files kept");
                return;
            }
            activateProfile(loaded);
        }));
    }

    private void activateProfile(GrinderProfile loaded) {
        completionNotifications.reset();
        profile = loaded;
        sessionProfiles.put(loaded.getHash(), loaded);
        loaded.setItemManager(itemManager);
        loaded.getPlayerStorage().reset();
        loaded.getPlayerStorage().captureLoaded(client);
        loaded.getCollectionLogHistory().observe(client.getItemContainer(InventoryID.COLLECTION_TRANSMIT));
        for (Goal goal : loaded.getGoals()) {
            if (goal instanceof SessionExperienceGoal) {
                ((SessionExperienceGoal) goal).restart();
            }
            goal.refresh(client);
        }
        panel.setStorageStatus(null);
        panel.load(loaded);
        log.debug("Loaded Grindstone profile with {} goals", loaded.getGoals().size());
    }

    private void installMigratedProfile(GrinderProfile restored) {
        if (!hasActivePlayerProfile() || restored.getHash() != profile.getHash()) {
            return;
        }
        saveProfile();
        loadGeneration.incrementAndGet();
        activateProfile(restored);
        saveProfile();
    }

    private void onLogout() {
        completionNotifications.reset();
        loadGeneration.incrementAndGet();
        loadingHash = 0;
        completionProgressDirty = false;
        storageProgressDirty = false;
        if (profile != null) {
            profile.getPlayerStorage().reset();
        }
        if (config.saveOnLogout()) {
            saveProfile();
        }
        profile = null;
        panel.unload();
    }

    private void saveProfile() {
        GrinderProfile active = profile;
        if (active == null) {
            return;
        }
        clientThread.invoke(() -> {
            try {
                JsonObject snapshot = repository.toJson(active);
                if (running && profile == active) {
                    panel.setStorageStatus("Saving profile…");
                }
                persistence.save(snapshot).whenComplete((ignored, failure) -> {
                    if (failure != null) {
                        log.warn("Unable to save Grindstone profile; the previous save was retained", failure);
                    }
                    clientThread.invokeLater(() -> {
                        if (running && profile == active) {
                            panel.setStorageStatus(failure == null ? null : "Save failed · previous file retained");
                        }
                    });
                });
            } catch (RuntimeException failure) {
                log.warn("Unable to capture Grindstone profile; saved files were not changed", failure);
                if (running && profile == active) {
                    panel.setStorageStatus("Could not capture profile · saved files kept");
                }
            }
        });
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
        return ImageUtil.loadImageResource(getClass(), "/grinder.png");
    }

    @Provides
    GrinderConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GrinderConfig.class);
    }
}
