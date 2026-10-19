package com.grindstone;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.combatachievement.CombatAchievementData;
import com.grindstone.content.goal.BaseSkillGoal;
import com.grindstone.content.goal.ClientCompletionGoal;
import com.grindstone.content.goal.Goal;
import com.grindstone.content.goal.impl.TrackerGoal;
import com.grindstone.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grindstone.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grindstone.content.goal.impl.combatachievement.CombatAchievementTaskGoal;
import com.grindstone.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grindstone.content.tracker.TrackerEntry;
import com.grindstone.content.goal.impl.experience.SessionExperienceGoal;
import com.grindstone.content.goal.impl.experience.BaseExperienceGoal;
import com.grindstone.content.goal.impl.experience.SkillExperienceGoal;
import com.grindstone.content.goal.impl.experience.TotalExperienceGoal;
import com.grindstone.content.goal.impl.item.PossessedItemGoal;
import com.grindstone.content.goal.impl.level.CombatLevelGoal;
import com.grindstone.content.goal.impl.level.BaseLevelGoal;
import com.grindstone.content.goal.impl.level.SkillLevelGoal;
import com.grindstone.content.goal.impl.level.TotalLevelGoal;
import com.grindstone.notification.GoalCompletionNotifications;
import com.grindstone.notification.GoalCompletionOverlay;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.GrindstoneRepository;
import com.grindstone.repository.ProfilePersistence;
import com.grindstone.ui.GrindstonePanel;
import com.grindstone.ui.util.SkillWidgets;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;
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
@PluginDescriptor( internalName = "grindstone", name = "Grindstone", description = "Track collection log progress and personal goals", tags = {"collection", "goals", "tracker"} )
public class GrindstonePlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    @Inject
    private GrindstoneConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ClientThread clientThread;

    @Inject
    private GrindstonePanel panel;
    @Inject
    private GrindstoneRepository repository;
    @Inject
    private ProfilePersistence persistence;
    @Inject
    private GoalCompletionNotifications completionNotifications;
    @Inject
    private GoalCompletionOverlay completionOverlay;
    @Inject
    private OverlayManager overlayManager;

    private NavigationButton navigationButton;
    private volatile GrindstoneProfile profile;
    private final Map<Long, GrindstoneProfile> sessionProfiles = new ConcurrentHashMap<>();
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
        if (!GrindstoneConfig.GROUP.equals(event.getGroup())) {
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
        GrindstoneProfile active = profile;
        SwingUtilities.invokeLater(() -> {
            if (running && config.enabled() && profile == active) {
                panel.showAddGoalDialog(selection);
            }
        });
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        if (!hasActivePlayerProfile()) {
            return;
        }
        MenuEntry[] entries = event.getMenuEntries();
        for (int i = entries.length - 1; i >= 0; i--) {
            MenuEntry entry = entries[i];
            if (entry.getParam1() >>> 16 == InterfaceID.CA_BOSSES && (entry.getType() == MenuAction.CC_OP || entry.getType() == MenuAction.CC_OP_LOW_PRIORITY)) {
                addBossAchievementMenu(entry);
                break;
            }
        }
        if (config.combatLevelGoalMenu() && config.levelGoalsEnabled() && isMouseOverWidget(InterfaceID.CombatInterface.LEVEL)) {
            client.getMenu().createMenuEntry(-1)
                    .setOption("Add combat level goal")
                    .setTarget("Combat level")
                    .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                    .onClick(entry -> {
                        if (hasActivePlayerProfile() && config.combatLevelGoalMenu() && config.levelGoalsEnabled()) {
                            openGoalEditor(new CombatLevelGoal());
                        }
                    });
        }
        if (!config.skillGoalMenu() || !isMouseOverWidget(InterfaceID.Stats.TOTAL)) {
            return;
        }
        addSkillGoalOption("Add total level goal", "All skills", TotalLevelGoal::new, false);
        addSkillGoalOption("Add total XP goal", "All skills", TotalExperienceGoal::new, true);
        addSkillGoalOption("Add base level goal", "All skills", BaseLevelGoal::new, false);
        addSkillGoalOption("Add base XP goal", "All skills", BaseExperienceGoal::new, true);
    }

    private boolean isMouseOverWidget(int componentId) {
        Widget widget = client.getWidget(componentId);
        Point mouse = client.getMouseCanvasPosition();
        return widget != null && !widget.isHidden() && widget.getBounds().contains(mouse.getX(), mouse.getY());
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
        if (!config.bossGoalMenu() || !config.combatAchievementGoalsEnabled()) {
            return;
        }
        if (combatBossIds.isEmpty()) {
            CombatAchievementData.tasksByBoss(client).forEach((boss, tasks) -> {
                if (!tasks.isEmpty()) {
                    combatBossIds.put(bossNameKey(CombatAchievementData.bossName(client, boss)), boss);
                }
            });
        }
        String name = Text.removeTags(original.getTarget().replace("<br>", " ")).trim();
        Integer boss = combatBossIds.get(bossNameKey(name));
        if (boss == null && original.getWidget() != null) {
            Set<String> names = new LinkedHashSet<>();
            findBossCardNames(client.getWidget(InterfaceID.CaBosses.BOSSES_LIST), original.getWidget().getBounds(), names);
            if (names.size() == 1) {
                name = names.iterator().next();
                boss = combatBossIds.get(bossNameKey(name));
            }
        }
        if (boss == null) {
            return;
        }
        final String selectedName = name;
        final int selectedBoss = boss;
        for (MenuEntry entry : client.getMenu().getMenuEntries()) {
            if ("Add goal".equals(entry.getOption()) && bossNameKey(entry.getTarget()).equals(bossNameKey(selectedName))) {
                return;
            }
        }
        client.getMenu().createMenuEntry(-2)
                .setOption("Add goal")
                .setTarget(selectedName)
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (hasActivePlayerProfile() && config.bossGoalMenu() && config.combatAchievementGoalsEnabled()) {
                        openGoalEditor(new CombatAchievementBossGoal(selectedBoss, selectedName));
                    }
                });
    }

    private static String bossNameKey(String text) {
        return text == null ? "" : Text.removeTags(text.replace("<br>", " ")).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private void findBossCardNames(Widget widget, Rectangle card, Set<String> names) {
        if (widget == null || widget.isHidden()) {
            return;
        }
        Rectangle bounds = widget.getBounds();
        String text = widget.getText();
        if (text != null && card.contains(bounds.getCenterX(), bounds.getCenterY()) && combatBossIds.containsKey(bossNameKey(text))) {
            names.add(Text.removeTags(text.replace("<br>", " ")).trim().replaceAll("\\s+", " "));
        }
        Widget[] dynamic = widget.getDynamicChildren();
        if (dynamic != null) {
            for (Widget child : dynamic) {
                findBossCardNames(child, card, names);
            }
        }
        Widget[] fixed = widget.getStaticChildren();
        if (fixed != null) {
            for (Widget child : fixed) {
                findBossCardNames(child, card, names);
            }
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
        GrindstoneProfile cached = sessionProfiles.get(hash);
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

    private void activateProfile(GrindstoneProfile loaded) {
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
        GrindstoneProfile active = profile;
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
        return ImageUtil.loadImageResource(getClass(), "/grindstone.png");
    }

    @Provides
    GrindstoneConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GrindstoneConfig.class);
    }
}
