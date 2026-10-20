package com.grindstone;

import com.google.gson.JsonObject;
import com.google.inject.Provides;
import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.combatachievement.CombatAchievementData;
import com.grindstone.content.combatachievement.CombatAchievementTier;
import com.grindstone.content.diary.AchievementDiaryArea;
import com.grindstone.content.diary.DiaryTier;
import com.grindstone.content.milestone.BaseSkillMilestone;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.achievementdiary.AchievementDiaryTierAreaMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTabMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogItemMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementBossMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementDifficultyMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTierMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementTaskMilestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.BaseLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import com.grindstone.content.milestone.impl.quest.QuestMilestone;
import com.grindstone.content.tracker.TrackerEntry;
import com.grindstone.notification.MilestoneCompletionNotifications;
import com.grindstone.notification.MilestoneCompletionOverlay;
import com.grindstone.overlay.GrindstoneProgressOverlay;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.GrindstoneRepository;
import com.grindstone.repository.ProfilePersistence;
import com.grindstone.ui.GrindstonePanel;
import com.grindstone.ui.editor.CompletionMilestoneChoices;
import com.grindstone.ui.util.SkillWidgets;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
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
@PluginDescriptor( internalName = "grindstone", name = "Grindstone", description = "Track milestones, collection log unlocks and personal routines", tags = {"collection", "milestones", "routines", "goals", "tracker"} )
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
    private MilestoneCompletionNotifications completionNotifications;
    @Inject
    private MilestoneCompletionOverlay completionOverlay;
    @Inject
    private GrindstoneProgressOverlay progressOverlay;
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
    private static final int COLLECTION_SEARCH_ITEM_SCRIPT = 4100;
    private boolean collectionCaptureDirty;
    private long lastRecurringRefresh;
    private JsonObject lastSavedSnapshot;

    @Override
    protected void startUp() {
        running = true;
        lastSavedSnapshot = null;
        completionNotifications.stop();
        overlayManager.add(completionOverlay);
        overlayManager.add(progressOverlay);
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
        collectionCaptureDirty = false;
        overlayManager.remove(completionOverlay);
        overlayManager.remove(progressOverlay);
        progressOverlay.clear();
        completionNotifications.stop();
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
        if (state != GameState.LOGGED_IN) {
            progressOverlay.clear();
        }

        if (state == GameState.LOGGED_IN && config.enabled()) {
            loadProfile();
            panel.render();
        } else if (state == GameState.LOGIN_SCREEN || state == GameState.CONNECTION_LOST) {
            onLogout();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!GrindstoneConfig.GROUP.equals(event.getGroup())) {
            return;
        }
        clientThread.invokeLater(() -> applyConfigChange(event.getKey()));
    }

    private void applyConfigChange(String key) {
        if (!running) {
            return;
        }
        if (!config.enabled()) {
            completionNotifications.stop();
            saveProfile();
            loadGeneration.incrementAndGet();
            loadingHash = 0;
            profile = null;
            panel.unload();
            setNavigationVisible(false);
            return;
        }

        setNavigationVisible(true);

        if (client.getGameState() == GameState.LOGGED_IN && (profile == null || "enabled".equals(key))) {
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
        if (original.getParam1() >>> 16 == InterfaceID.QUESTLIST || original.getParam1() == InterfaceID.SideJournal.QUEST_LIST) {
            addQuestGoalMenu(original);
            return;
        }
        if (original.getParam1() >>> 16 == InterfaceID.AREA_TASK || original.getParam1() == InterfaceID.SideJournal.TASK_LIST) {
            addDiaryGoalMenu(original);
            return;
        }
        if (original.getParam1() >>> 16 == InterfaceID.STATS) {
            if (config.skillGoalMenu() && original.getIdentifier() == 1) {
                Skill skill = SkillWidgets.getSkillFor(original.getParam1());
                if (skill != null) {
                    addSkillGoalOption("Set level milestone", skill.getName(), () -> new SkillLevelMilestone(skill), false);
                    addSkillGoalOption("Set XP milestone", skill.getName(), () -> new SkillExperienceMilestone(skill), true);
                }
            }
            return;
        }
        if (original.getParam1() >>> 16 == InterfaceID.CA_OVERVIEW || original.getParam1() >>> 16 == InterfaceID.CA_REWARDS) {
            addCombatAchievementTierMenu(original);
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
        String option = selectedItem > 0 ? "Track unlock" : "Track page";
        if (selectedItem <= 0 && selectedName.isEmpty() || hasMilestoneOption(option, original.getTarget())) {
            return;
        }
        if (selectedItem > 0) {
            if (!canOfferGoal(new CollectionLogItemMilestone(CollectionLogPages.getCanonicalItemId(selectedItem), 1))) {
                return;
            }
        } else {
            for (Map.Entry<Integer, String> page : CollectionLogPages.discover(client).entrySet()) {
                if (page.getValue().equalsIgnoreCase(selectedName) && !canOfferGoal(new CollectionLogEntryMilestone(itemManager, page.getKey(), page.getValue()))) {
                    return;
                }
            }
        }
        client.getMenu().createMenuEntry(-2)
                .setOption(option)
                .setTarget(original.getTarget())
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (!hasActivePlayerProfile() || !config.collectionLogEnabled() || !config.collectionLogGoalMenu()) {
                        return;
                    }
                    Milestone goal = null;
                    if (selectedItem > 0) {
                        goal = new CollectionLogItemMilestone(CollectionLogPages.getCanonicalItemId(selectedItem), 1);
                    } else {
                        for (Map.Entry<Integer, String> page : CollectionLogPages.discover(client).entrySet()) {
                            if (page.getValue().equalsIgnoreCase(selectedName)) {
                                goal = new CollectionLogEntryMilestone(itemManager, page.getKey(), page.getValue());
                                break;
                            }
                        }
                    }
                    if (goal != null) {
                        openGoalEditor(goal);
                    }
                });
    }

    private String getGoalMenuName(MenuEntry original) {
        String name = Text.removeTags(original.getTarget()).trim();
        if (name.isEmpty() && original.getWidget() != null) {
            name = Text.removeTags(original.getWidget().getText()).trim();
        }
        return name;
    }

    private boolean hasMilestoneOption(String option, String target) {
        String name = Text.removeTags(target).trim();
        for (MenuEntry entry : client.getMenu().getMenuEntries()) {
            if (option.equals(entry.getOption()) && name.equalsIgnoreCase(Text.removeTags(entry.getTarget()).trim())) {
                return true;
            }
        }
        return false;
    }

    private void addQuestGoalMenu(MenuEntry original) {
        if (!config.questGoalsEnabled() || !config.questGoalMenu()) {
            return;
        }
        String name = getGoalMenuName(original);
        for (Quest quest : Quest.values()) {
            if (!quest.getName().equalsIgnoreCase(name)) {
                continue;
            }
            if (!canOfferGoal(new QuestMilestone(quest)) || hasMilestoneOption("Set milestone", quest.getName())) {
                return;
            }
            client.getMenu().createMenuEntry(-2)
                    .setOption("Set milestone")
                    .setTarget(quest.getName())
                    .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                    .onClick(entry -> {
                        if (hasActivePlayerProfile() && config.questGoalsEnabled() && config.questGoalMenu()) {
                            QuestMilestone goal = new QuestMilestone(quest);
                            if (canOfferGoal(goal)) {
                                openGoalEditor(goal);
                            }
                        }
                    });
            return;
        }
    }

    private String getDiaryAreaName(String text) {
        return Text.removeTags(text).trim().replaceFirst("(?i)^(?:open|wiki) +", "").replaceFirst("(?i) +(?:diary|journal)$", "").trim();
    }

    private void addDiaryGoalMenu(MenuEntry original) {
        if (!config.diaryGoalsEnabled() || !config.diaryGoalMenu()) {
            return;
        }
        String name = getDiaryAreaName(getGoalMenuName(original));
        String optionName = getDiaryAreaName(original.getOption());
        for (AchievementDiaryArea area : AchievementDiaryArea.values()) {
            if (!area.toString().equalsIgnoreCase(name) && !area.toString().equalsIgnoreCase(optionName)) {
                continue;
            }
            for (DiaryTier tier : DiaryTier.values()) {
                String option = "Set " + tier + " milestone";
                if (!canOfferGoal(new AchievementDiaryTierAreaMilestone(area, tier)) || hasMilestoneOption(option, area.toString())) {
                    continue;
                }
                client.getMenu().createMenuEntry(-2)
                        .setOption(option)
                        .setTarget(area.toString())
                        .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                        .onClick(entry -> {
                            if (hasActivePlayerProfile() && config.diaryGoalsEnabled() && config.diaryGoalMenu()) {
                                AchievementDiaryTierAreaMilestone goal = new AchievementDiaryTierAreaMilestone(area, tier);
                                if (canOfferGoal(goal)) {
                                    openGoalEditor(goal);
                                }
                            }
                        });
            }
            return;
        }
    }

    private void addCombatAchievementTierMenu(MenuEntry original) {
        if (!config.combatAchievementGoalsEnabled() || !config.combatAchievementGoalMenu()) {
            return;
        }
        boolean reward = original.getParam1() >>> 16 == InterfaceID.CA_REWARDS;
        int component = original.getParam1();
        if (reward && component != InterfaceID.CaRewards.TIERS_CLICK && component != InterfaceID.CaRewards.TIERS_TABS && component != InterfaceID.CaRewards.TIERS_CONTENT || !reward && component != InterfaceID.CaOverview.CA_BUTTONS_CLICK && component != InterfaceID.CaOverview.CA_BUTTONS) {
            return;
        }
        String name = (getGoalMenuName(original) + " " + Text.removeTags(original.getOption())).replaceAll("(?i)tasks?|rewards?|tiers?", " ").trim().toLowerCase(Locale.ROOT);
        for (CombatAchievementTier tier : CombatAchievementTier.values()) {
            if (!name.matches("(?s).*\\b" + tier.toString().toLowerCase(Locale.ROOT) + "\\b.*")) {
                continue;
            }
            String target = tier + (reward ? " reward tier" : " tasks");
            Supplier<Milestone> factory = reward ? () -> new CombatAchievementTierMilestone(tier) : () -> new CombatAchievementDifficultyMilestone(tier);
            if (!canOfferGoal(factory.get()) || hasMilestoneOption("Set milestone", target)) {
                return;
            }
            client.getMenu().createMenuEntry(-2)
                    .setOption("Set milestone")
                    .setTarget(target)
                    .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                    .onClick(entry -> {
                        if (hasActivePlayerProfile() && config.combatAchievementGoalsEnabled() && config.combatAchievementGoalMenu()) {
                            Milestone goal = factory.get();
                            if (canOfferGoal(goal)) {
                                openGoalEditor(goal);
                            }
                        }
                    });
            return;
        }
    }

    private void addCombatAchievementGoalMenu(MenuEntry original) {
        if (!config.combatAchievementGoalsEnabled() || !config.combatAchievementGoalMenu() || !"Expand".equals(original.getOption()) && !"Collapse".equals(original.getOption())) {
            return;
        }
        String selectedName = Text.removeTags(original.getTarget()).trim();
        if (selectedName.isEmpty() || hasMilestoneOption("Track task", original.getTarget())) {
            return;
        }
        boolean available = CombatAchievementData.getTasks(client).values().stream()
                .filter(task -> Text.removeTags(task.getName()).trim().equalsIgnoreCase(selectedName))
                .anyMatch(task -> canOfferGoal(new CombatAchievementTaskMilestone(task.getId(), task.getName(), task.getDescription())));
        if (!available) {
            return;
        }
        client.getMenu().createMenuEntry(-2)
                .setOption("Track task")
                .setTarget(original.getTarget())
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (!hasActivePlayerProfile() || !config.combatAchievementGoalsEnabled() || !config.combatAchievementGoalMenu()) {
                        return;
                    }
                    for (CombatAchievementData.Task task : CombatAchievementData.getTasks(client).values()) {
                        if (Text.removeTags(task.getName()).trim().equalsIgnoreCase(selectedName)) {
                            openGoalEditor(new CombatAchievementTaskMilestone(task.getId(), task.getName(), task.getDescription()));
                            return;
                        }
                    }
                });
    }

    private boolean canOfferGoal(Milestone goal) {
        if (goal.getCategory() == MilestoneCategory.COLLECTION_LOG && !profile.getCollectionLogHistory().hasSnapshot()) {
            return false;
        }
        goal.refresh(client);
        if (goal instanceof CollectionLogTabMilestone) {
            ((CollectionLogTabMilestone) goal).setHistory(profile.getCollectionLogHistory());
        }
        if (goal instanceof CollectionLogItemMilestone) {
            ((CollectionLogItemMilestone) goal).setHistory(profile.getCollectionLogHistory());
            goal.prepare(client, null);
        } else if (goal instanceof CollectionLogEntryMilestone) {
            ((CollectionLogEntryMilestone) goal).setHistory(profile.getCollectionLogHistory());
            goal.prepare(client, null);
        }
        return CompletionMilestoneChoices.hasRemainingTarget(goal);
    }

    private void openGoalEditor(Milestone selection) {
        if (!canOfferGoal(selection)) {
            return;
        }
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
        if (config.combatLevelGoalMenu() && config.levelGoalsEnabled() && canOfferGoal(new CombatLevelMilestone()) && isMouseOverWidget(InterfaceID.CombatInterface.LEVEL) && !hasMilestoneOption("Set combat milestone", "Combat level")) {
            client.getMenu().createMenuEntry(-1)
                    .setOption("Set combat milestone")
                    .setTarget("Combat level")
                    .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                    .onClick(entry -> {
                        if (hasActivePlayerProfile() && config.combatLevelGoalMenu() && config.levelGoalsEnabled()) {
                            openGoalEditor(new CombatLevelMilestone());
                        }
                    });
        }
        if (!config.skillGoalMenu() || !isMouseOverWidget(InterfaceID.Stats.TOTAL)) {
            return;
        }
        addSkillGoalOption("Set total level milestone", "All skills", TotalLevelMilestone::new, false);
        addSkillGoalOption("Set total XP milestone", "All skills", TotalExperienceMilestone::new, true);
        addSkillGoalOption("Set base level milestone", "All skills", BaseLevelMilestone::new, false);
        addSkillGoalOption("Set base XP milestone", "All skills", BaseExperienceMilestone::new, true);
    }

    private boolean isMouseOverWidget(int componentId) {
        Widget widget = client.getWidget(componentId);
        Point mouse = client.getMouseCanvasPosition();
        return widget != null && !widget.isHidden() && widget.getBounds().contains(mouse.getX(), mouse.getY());
    }

    private void addSkillGoalOption(String label, String target, Supplier<Milestone> factory, boolean experience) {
        if ((experience ? !config.experienceGoalsEnabled() : !config.levelGoalsEnabled()) || hasMilestoneOption(label, target)) {
            return;
        }
        if (!canOfferGoal(factory.get())) {
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
        if (tracker == null || hasMilestoneOption("Track kills", original.getTarget())) {
            return;
        }
        client.getMenu().createMenuEntry(-2)
                .setOption("Track kills")
                .setTarget(original.getTarget())
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (hasActivePlayerProfile() && config.bossGoalMenu() && config.trackerGoalsEnabled()) {
                        openGoalEditor(new TrackerMilestone(tracker, tracker.getDefaultTarget()));
                    }
                });
    }

    private void addBossAchievementMenu(MenuEntry original) {
        if (!config.bossGoalMenu() || !config.combatAchievementGoalsEnabled()) {
            return;
        }
        if (combatBossIds.isEmpty()) {
            CombatAchievementData.getTasksByBoss(client).forEach((boss, tasks) -> {
                if (!tasks.isEmpty()) {
                    combatBossIds.put(getBossNameKey(CombatAchievementData.getBossName(client, boss)), boss);
                }
            });
        }
        String name = Text.removeTags(original.getTarget().replace("<br>", " ")).trim();
        Integer boss = combatBossIds.get(getBossNameKey(name));
        if (boss == null && original.getWidget() != null) {
            Set<String> names = new LinkedHashSet<>();
            findBossCardNames(client.getWidget(InterfaceID.CaBosses.BOSSES_LIST), original.getWidget().getBounds(), names);
            if (names.size() == 1) {
                name = names.iterator().next();
                boss = combatBossIds.get(getBossNameKey(name));
            }
        }
        if (boss == null) {
            return;
        }
        final String selectedName = name;
        final int selectedBoss = boss;
        if (!canOfferGoal(new CombatAchievementBossMilestone(selectedBoss, selectedName))) {
            return;
        }
        for (MenuEntry entry : client.getMenu().getMenuEntries()) {
            if ("Track boss achievements".equals(entry.getOption()) && getBossNameKey(entry.getTarget()).equals(getBossNameKey(selectedName))) {
                return;
            }
        }
        client.getMenu().createMenuEntry(-2)
                .setOption("Track boss achievements")
                .setTarget(selectedName)
                .setType(MenuAction.RUNELITE_LOW_PRIORITY)
                .onClick(entry -> {
                    if (hasActivePlayerProfile() && config.bossGoalMenu() && config.combatAchievementGoalsEnabled()) {
                        openGoalEditor(new CombatAchievementBossMilestone(selectedBoss, selectedName));
                    }
                });
    }

    private static String getBossNameKey(String text) {
        return text == null ? "" : Text.removeTags(text.replace("<br>", " ")).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private void findBossCardNames(Widget widget, Rectangle card, Set<String> names) {
        if (widget == null || widget.isHidden()) {
            return;
        }
        Rectangle bounds = widget.getBounds();
        String text = widget.getText();
        if (text != null && card.contains(bounds.getCenterX(), bounds.getCenterY()) && combatBossIds.containsKey(getBossNameKey(text))) {
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
        if (container.getId() == InventoryID.COLLECTION_TRANSMIT) {
            collectionCaptureDirty = true;
            captureLoadedCollectionSnapshot();
        }
        storageProgressDirty |= profile.getPlayerStorage().observe(client, container);

        for (Milestone goal : profile.getGoals()) {
            if (goal == null || goal instanceof PossessedItemMilestone || !goal.isAffectedBy(container)) {
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
        for (Milestone goal : profile.getGoals()) {
            if (goal instanceof TrackerMilestone && goal.refresh(client)) {
                panel.refreshGoal(goal);
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (hasActivePlayerProfile() && event.getParam1() == InterfaceID.Collection.SEARCH_TOGGLE) {
            CollectionLogPages.refreshReplacements(client);
            collectionCaptureDirty = true;
        }
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        if (!hasActivePlayerProfile() || event.getScriptId() != COLLECTION_SEARCH_ITEM_SCRIPT) {
            return;
        }
        Object[] arguments = event.getScriptEvent().getArguments();
        if (arguments == null || arguments.length < 3 || !(arguments[1] instanceof Integer) || !(arguments[2] instanceof Integer)) {
            return;
        }
        if (!collectionCaptureDirty) {
            CollectionLogPages.refreshReplacements(client);
        }
        int item = CollectionLogPages.getCanonicalItemId((Integer) arguments[1]);
        if (item > 0 && (Integer) arguments[2] > 0) {
            profile.getCollectionLogHistory().record(item, System.currentTimeMillis());
        }
        collectionCaptureDirty = true;
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (hasActivePlayerProfile() && event.getGroupId() == InterfaceID.Collection.UNIVERSE >>> 16) {
            collectionCaptureDirty = true;
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (hasActivePlayerProfile() && event.getScriptId() == 7797) {
            clientThread.invokeLater(() -> {
                if (hasActivePlayerProfile()) {
                    CollectionLogPages.refreshReplacements(client);
                    observeLoadedCollectionSnapshot();
                    collectionCaptureDirty = true;
                }
            });
        }
    }

    private void observeLoadedCollectionSnapshot() {
        CollectionLogPages.refreshReplacements(client);
        Set<Integer> validItems = new LinkedHashSet<>();
        for (int tab : CollectionLogPages.getTabs(client).keySet()) {
            for (int item : CollectionLogPages.getTabItemIds(client, tab)) {
                validItems.add(item);
            }
        }
        int maximum = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
        boolean completeCatalog = maximum > 0 && validItems.size() == maximum;
        if (profile.getCollectionLogHistory().reconcileItems(completeCatalog ? validItems : null)) {
            profile.touch();
            saveProfile();
        }
        if (!completeCatalog) {
            log.debug("Collection-log catalog size {} differs from game maximum {}; retaining unknown observations", validItems.size(), maximum);
        }
        ItemContainer container = client.getItemContainer(InventoryID.COLLECTION_TRANSMIT);
        if (container != null) {
            profile.getCollectionLogHistory().observe(container);
        }
        if (completeCatalog) {
            profile.getCollectionLogHistory().reconcileItems(validItems);
        }
        captureLoadedCollectionSnapshot();
    }

    private void captureLoadedCollectionSnapshot() {
        if (profile == null) {
            return;
        }
        Widget collection = client.getWidget(InterfaceID.Collection.UNIVERSE);
        if (client.getItemContainer(InventoryID.COLLECTION_TRANSMIT) == null && (collection == null || collection.isHidden())) {
            return;
        }
        int total = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
        int maximum = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
        if (profile.getCollectionLogHistory().confirmLoadedSnapshot(total, maximum, System.currentTimeMillis())) {
            profile.touch();
            panel.refreshAll();
            saveProfile();
        }
    }

    private void finishCollectionSnapshot() {
        if (!collectionCaptureDirty) {
            return;
        }
        collectionCaptureDirty = false;
        observeLoadedCollectionSnapshot();
        profile.touch();
        for (Milestone milestone : profile.getGoals()) {
            if (milestone.getCategory() == MilestoneCategory.COLLECTION_LOG) {
                milestone.refresh(client);
                panel.refreshGoal(milestone);
            }
        }
        panel.refreshAll();
        saveProfile();
    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        if (collectionCaptureDirty && hasActivePlayerProfile()) {
            finishCollectionSnapshot();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (hasActivePlayerProfile() && System.currentTimeMillis() - lastRecurringRefresh >= 30_000) {
            lastRecurringRefresh = System.currentTimeMillis();
            if (profile.getGoals().stream().anyMatch(goal -> profile.getMetadata(goal).getRecurring().isActive() || profile.getMetadata(goal).getDeadlineAt() > 0)) {
                panel.refreshAll();
            }
        }
        if ((!completionProgressDirty && !storageProgressDirty) || !hasActivePlayerProfile()) {
            return;
        }
        boolean variablesChanged = completionProgressDirty;
        if (variablesChanged) {
            captureLoadedCollectionSnapshot();
        }
        boolean storageChanged = storageProgressDirty;
        completionProgressDirty = false;
        storageProgressDirty = false;
        if (variablesChanged || storageChanged) {
            profile.getPlayerStorage().refreshVariables(client);
        }
        boolean changed = storageChanged;
        for (Milestone goal : profile.getGoals()) {
            if ((variablesChanged && goal instanceof ClientCompletionMilestone) || goal instanceof PossessedItemMilestone) {
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

        for (Milestone goal : profile.getGoals()) {
            if ((goal instanceof SkillLevelMilestone && ((SkillLevelMilestone) goal).getSkill() == event.getSkill()) || (goal instanceof SkillExperienceMilestone && ((SkillExperienceMilestone) goal).getSkill() == event.getSkill()) || goal instanceof TotalLevelMilestone || goal instanceof TotalExperienceMilestone || goal instanceof CombatLevelMilestone || goal instanceof BaseSkillMilestone || goal instanceof SessionExperienceMilestone) {
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
        collectionCaptureDirty = false;
        completionNotifications.reset();
        profile = loaded;
        sessionProfiles.put(loaded.getHash(), loaded);
        loaded.setItemManager(itemManager);
        loaded.getPlayerStorage().reset();
        loaded.getPlayerStorage().captureLoaded(client);
        loaded.getCollectionLogHistory().observe(client.getItemContainer(InventoryID.COLLECTION_TRANSMIT));
        for (Milestone goal : loaded.getGoals()) {
            loaded.getMetadata(goal).beginCatchUp();
            if (goal instanceof SessionExperienceMilestone) {
                ((SessionExperienceMilestone) goal).restart();
            }
            goal.refresh(client);
        }
        panel.setStorageStatus(null);
        panel.load(loaded);
        observeLoadedCollectionSnapshot();
        log.debug("Loaded Grindstone profile with {} milestones", loaded.getGoals().size());
    }

    private void onLogout() {
        collectionCaptureDirty = false;
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

    private CompletableFuture<Void> saveProfile() {
        GrindstoneProfile active = profile;
        if (active == null) {
            return persistence.flush();
        }
        CompletableFuture<Void> finished = new CompletableFuture<>();
        clientThread.invoke(() -> {
            try {
                JsonObject snapshot = repository.toJson(active);
                if (snapshot.equals(lastSavedSnapshot)) {
                    persistence.flush().whenComplete((ignored, failure) -> completeSave(finished, failure));
                    return;
                }
                lastSavedSnapshot = snapshot;
                if (running && profile == active) {
                    panel.setStorageStatus("Saving profile…");
                }
                persistence.save(snapshot).whenComplete((ignored, failure) -> {
                    completeSave(finished, failure);
                    if (failure != null) {
                        log.warn("Unable to save Grindstone profile; the previous save was retained", failure);
                    }
                    clientThread.invokeLater(() -> {
                        if (failure != null && lastSavedSnapshot == snapshot) {
                            lastSavedSnapshot = null;
                        }
                        if (running && profile == active) {
                            panel.setStorageStatus(failure == null ? null : "Save failed · previous file retained");
                        }
                    });
                });
            } catch (RuntimeException failure) {
                lastSavedSnapshot = null;
                finished.completeExceptionally(failure);
                log.warn("Unable to capture Grindstone profile; saved files were not changed", failure);
                if (running && profile == active) {
                    panel.setStorageStatus("Could not capture profile · saved files kept");
                }
            }
        });
        return finished;
    }

    private static void completeSave(CompletableFuture<Void> finished, Throwable failure) {
        if (failure == null) {
            finished.complete(null);
        } else {
            finished.completeExceptionally(failure);
        }
    }

    @Subscribe
    public void onClientShutdown(ClientShutdown event) {
        if (config.saveOnShutdown()) {
            event.waitFor(saveProfile());
        }
    }

    @Schedule(period = 5, unit = ChronoUnit.SECONDS)
    public void autosaveProfile() {
        clientThread.invokeLater(() -> {
            if (hasActivePlayerProfile()) {
                saveProfile();
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
        return ImageUtil.loadImageResource(GrindstonePlugin.class, "grindstone.png");
    }

    @Provides
    GrindstoneConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GrindstoneConfig.class);
    }
}
