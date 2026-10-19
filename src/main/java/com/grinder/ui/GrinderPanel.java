package com.grinder.ui;

import com.grinder.util.HtmlUtils;

import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.repository.GrinderProfile;
import com.grinder.ui.components.GoalComponent;
import com.grinder.ui.components.ContentHeightPanel;
import com.grinder.ui.components.GoalListPanel;
import com.grinder.ui.editor.GoalEditorPanel;
import com.grinder.ui.editor.GoalCreationDialog;
import com.grinder.ui.item.ItemSearchService;
import com.grinder.ui.editor.CustomGoalForm;
import com.grinder.ui.util.UiTooltips;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.ui.components.SegmentedProgressBar;
import net.runelite.client.ui.components.IconTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import com.grinder.ui.util.UiTypography;
import net.runelite.client.ui.PluginPanel;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GrinderPanel extends PluginPanel {
    @Inject private ProfileBackupActions backupActions;
    private static final String ALL_CATEGORIES = "All categories";
    private static final String ALL_STATUSES = "All statuses";
    private static final String NOT_STARTED = "Not started";
    private static final String IN_PROGRESS = "In progress";
    private static final String COMPLETE = "Complete";
    private static final String UNFINISHED = "Unfinished";
    private static final String ALL_TYPES = "All goal types";
    private static final String AUTOMATIC = "Automatic";
    private static final String CUSTOM = "Custom";
    private static final String SORT_NAME = "Name (A-Z)";
    private static final String SORT_PROGRESS_DESC = "Progress (highest)";
    private static final String SORT_PROGRESS_ASC = "Progress (lowest)";
    private static final String SORT_STATUS = "Status";
    private static final String SORT_NAME_DESC = "Name (Z-A)";
    private static final String SORT_CLOSEST = "Closest unfinished";

    private static final Color PANEL_COLOR = ColorScheme.DARK_GRAY_COLOR;
    private static final Color HEADER_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color INPUT_COLOR = ColorScheme.CONTROL_COLOR;
    private static final Color TEXT_COLOR = ColorScheme.TEXT_COLOR;
    private static final Color MUTED_COLOR = ColorScheme.LIGHT_GRAY_COLOR;

    private final Client client;
    private final ClientThread clientThread;
    private final GrinderConfig config;

    @Inject
    private net.runelite.client.game.ItemManager itemManager;

    @Inject
    private ItemSearchService itemSearchService;

    private final JPanel main = new GoalListPanel();
    private final JPanel header = new JPanel();
    private final JPanel controlsPanel = new JPanel();
    private final JLabel summaryLabel = new JLabel("No profile loaded");
    private final JLabel updateStatusLabel = new JLabel();
    private final JLabel resultsLabel = new JLabel();
    private final SegmentedProgressBar overallProgress = new SegmentedProgressBar();
    private final IconTextField searchField = new IconTextField();
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<String> sortFilter = new JComboBox<>();
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final JToggleButton newGoalsButton = new JToggleButton("New 0");
    private final JToggleButton activeGoalsButton = new JToggleButton("Active 0");
    private final JToggleButton completeGoalsButton = new JToggleButton("Done 0");
    private final JButton filtersToggle = new JButton("Filters");
    private final JButton clearFiltersButton = new JButton("Clear");
    private final JButton undoDeleteButton = new JButton("Undo delete");
    private final com.grinder.repository.GoalUndo undo = new com.grinder.repository.GoalUndo();
    private final JScrollPane scrollPane;

    private final Map<GoalCategory, Boolean> collapsedCategories = new LinkedHashMap<>();
    private final Map<com.grinder.repository.RepeatSchedule, Boolean> collapsedRepeats = new LinkedHashMap<>();
    private final AtomicInteger renderVersion = new AtomicInteger();
    private final AtomicBoolean renderQueued = new AtomicBoolean();
    private final Timer filterRenderTimer = new Timer(150, event -> render());

    private volatile GrinderProfile profile;
    private boolean updatingFilters;

    @Inject
    public GrinderPanel(Client client, ClientThread clientThread, GrinderConfig config) {
        super(false);

        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        filterRenderTimer.setRepeats(false);

        setLayout(new BorderLayout());
        setBackground(PANEL_COLOR);
        setOpaque(true);

        buildHeader();
        buildControls();

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(PANEL_COLOR);
        topPanel.setOpaque(true);
        topPanel.add(header);
        topPanel.add(controlsPanel);
        add(topPanel, BorderLayout.NORTH);

        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(PANEL_COLOR);
        main.setOpaque(true);
        main.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));

        scrollPane = new JScrollPane(main);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(PANEL_COLOR);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        installListeners();
        render();
    }

    private void buildHeader() {
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(HEADER_COLOR);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Grinder Goals");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        summaryLabel.setForeground(MUTED_COLOR);
        summaryLabel.setFont(FontManager.getRunescapeSmallFont());
        summaryLabel.setAlignmentX(LEFT_ALIGNMENT);

        overallProgress.setStringPainted(false);
        overallProgress.setBorderPainted(false);
        overallProgress.setOpaque(true);
        overallProgress.setBackground(INPUT_COLOR);
        overallProgress.setPreferredSize(new Dimension(100, 8));
        overallProgress.setMinimumSize(new Dimension(0, 8));
        overallProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        overallProgress.setAlignmentX(LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setIcon(new ImageIcon(com.grinder.ui.Icon.MAIN.getImage()
                .getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH)));
        titleRow.add(titleLabel, BorderLayout.CENTER);
        JButton more = new JButton("...");
        styleButton(more);
        more.setToolTipText("More goal options");
        more.putClientProperty("JButton.buttonType", "toolBarButton");
        JPopupMenu moreMenu = new JPopupMenu();
        more.addActionListener(event -> moreMenu.show(more, 0, more.getHeight()));
        titleRow.add(more, BorderLayout.EAST);
        header.add(titleRow);
        header.add(Box.createVerticalStrut(6));
        header.add(summaryLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(overallProgress);
        updateStatusLabel.setFont(UiTypography.META);
        updateStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
        updateStatusLabel.setToolTipText("Client view refresh and last known bank snapshot. Open storage to load or refresh it; this is not an all-storage live total.");
        header.add(Box.createVerticalStrut(4));
        header.add(updateStatusLabel);

        JPanel counters = new JPanel(new GridLayout(1, 3, 4, 0));
        counters.setOpaque(false);
        counters.setAlignmentX(LEFT_ALIGNMENT);
        newGoalsButton.setForeground(SegmentedProgressBar.colorFor(GoalStatus.NOT_STARTED));
        activeGoalsButton.setForeground(SegmentedProgressBar.colorFor(GoalStatus.IN_PROGRESS));
        completeGoalsButton.setForeground(SegmentedProgressBar.colorFor(GoalStatus.COMPLETE));
        newGoalsButton.setToolTipText("Show not-started goals. Click again to show all statuses.");
        activeGoalsButton.setToolTipText("Show in-progress goals (yellow). Click again to show all statuses.");
        completeGoalsButton.setToolTipText("Show complete goals (green), including hidden completions. Click again to show all statuses.");
        for (JToggleButton counter : new JToggleButton[]{newGoalsButton, activeGoalsButton, completeGoalsButton}) {
            counter.setFont(FontManager.getRunescapeSmallFont());
            counter.setMargin(new java.awt.Insets(3, 3, 3, 3));
            counter.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }
        newGoalsButton.addActionListener(event -> toggleQuickStatus(NOT_STARTED));
        activeGoalsButton.addActionListener(event -> toggleQuickStatus(IN_PROGRESS));
        completeGoalsButton.addActionListener(event -> toggleQuickStatus(COMPLETE));
        counters.add(newGoalsButton);
        counters.add(activeGoalsButton);
        counters.add(completeGoalsButton);
        header.add(Box.createVerticalStrut(8));
        header.add(counters);

        JPanel goalActions = new JPanel(new GridLayout(1, 1));
        goalActions.setOpaque(false);
        goalActions.setAlignmentX(LEFT_ALIGNMENT);
        goalActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JButton addGoal = new JButton("+ Add goal");
        styleButton(addGoal);
        addGoal.setToolTipText("Create an automatic tracker or a custom goal with manual progress.");
        addGoal.addActionListener(event -> showAddGoalDialog());
        JButton defaults = new JButton("Add defaults");
        styleButton(defaults);
        defaults.setToolTipText("Add missing starter goals without replacing your goals");
        defaults.addActionListener(event -> {
            GrinderProfile active = profile;
            if (active != null) {
                clientThread.invokeLater(() -> {
                    if (profile == active) {
                        active.ensureDefaultGoals(itemManager);
                        render();
                    }
                });
            }
        });
        goalActions.add(addGoal);
        JMenuItem restoreDefaults = new JMenuItem("Add missing default goals");
        restoreDefaults.setToolTipText("Restore missing starter goals without replacing existing goals or edited targets.");
        restoreDefaults.addActionListener(event -> defaults.doClick());
        moreMenu.add(restoreDefaults);
        JMenuItem allCollectionPages = new JMenuItem("Add all collection-log pages...");
        allCollectionPages.setToolTipText("Read every page from the client's five collection-log tabs and add missing page goals.");
        allCollectionPages.addActionListener(event -> addAllCollectionPages());
        moreMenu.add(allCollectionPages);
        javax.swing.JMenu presets = new javax.swing.JMenu("Add goal preset");
        presets.setToolTipText("Add a focused goal pack without replacing existing goals or edited targets.");
        for (com.grinder.content.goal.GoalPreset preset : com.grinder.content.goal.GoalPreset.values()) {
            JMenuItem option = new JMenuItem(preset.toString());
            option.addActionListener(event -> addPreset(preset));
            presets.add(option);
        }
        moreMenu.add(presets);
        JMenuItem export = new JMenuItem("Export profile backup...");
        export.setToolTipText("Save a private local JSON backup. No network is used.");
        export.addActionListener(event -> transferBackup(false));
        moreMenu.add(export);
        JMenuItem restore = new JMenuItem("Restore / import backup...");
        restore.setToolTipText("Merge missing goals without replacing existing targets.");
        restore.addActionListener(event -> transferBackup(true));
        moreMenu.add(restore);
        JMenuItem expandAll = new JMenuItem("Expand all categories");
        expandAll.setToolTipText("Show the goal cards in every category.");
        expandAll.addActionListener(event -> setAllCategoriesCollapsed(false));
        moreMenu.add(expandAll);
        JMenuItem collapseAll = new JMenuItem("Collapse all categories");
        collapseAll.setToolTipText("Show category headers only. Your goals and progress are kept.");
        collapseAll.addActionListener(event -> setAllCategoriesCollapsed(true));
        moreMenu.add(collapseAll);
        JMenuItem reset = new JMenuItem("Reset search and filters");
        reset.setToolTipText("Clear the search, category, status and type filters, and restore name sorting.");
        reset.addActionListener(event -> clearFiltersButton.doClick());
        moreMenu.addSeparator();
        moreMenu.add(reset);
        header.add(Box.createVerticalStrut(8));
        header.add(goalActions);
        undoDeleteButton.setVisible(false);
        undoDeleteButton.setToolTipText("Restore the most recently deleted goal in this player profile.");
        undoDeleteButton.setAlignmentX(LEFT_ALIGNMENT);
        styleButton(undoDeleteButton);
        undoDeleteButton.addActionListener(event -> undoDelete());
        header.add(Box.createVerticalStrut(4));
        header.add(undoDeleteButton);
    }

    private void buildControls() {
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setBackground(PANEL_COLOR);
        controlsPanel.setOpaque(true);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        searchField.setIcon(IconTextField.Icon.SEARCH);
        UiTooltips.set(searchField, "Search goal titles and descriptions. Use the clear icon to clear the search.");
        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setAlignmentX(LEFT_ALIGNMENT);

        categoryFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_CATEGORIES}));
        statusFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_STATUSES, UNFINISHED, NOT_STARTED, IN_PROGRESS, COMPLETE}));
        sortFilter.setModel(new DefaultComboBoxModel<>(new String[]{SORT_NAME, SORT_NAME_DESC, SORT_CLOSEST, SORT_PROGRESS_DESC, SORT_PROGRESS_ASC, SORT_STATUS}));
        typeFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_TYPES, AUTOMATIC, CUSTOM}));
        styleComboBox(typeFilter);
        typeFilter.setAlignmentX(LEFT_ALIGNMENT);
        typeFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        styleComboBox(categoryFilter);
        styleComboBox(statusFilter);
        styleComboBox(sortFilter);
        UiTooltips.set(categoryFilter, "Show goals from one category, or choose All categories.");
        UiTooltips.set(statusFilter, "Filter by progress state. Complete also shows goals hidden by the completed-goals setting.");
        UiTooltips.set(sortFilter, "Sort within each category. Closest unfinished puts incomplete goals nearest their target first.");
        UiTooltips.set(typeFilter, "Automatic goals read game data; custom goals use progress you enter yourself.");

        categoryFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        statusFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        sortFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        categoryFilter.setAlignmentX(LEFT_ALIGNMENT);
        statusFilter.setAlignmentX(LEFT_ALIGNMENT);
        sortFilter.setAlignmentX(LEFT_ALIGNMENT);

        controlsPanel.add(createControlLabel("Search"));
        controlsPanel.add(Box.createVerticalStrut(3));
        controlsPanel.add(searchField);
        controlsPanel.add(Box.createVerticalStrut(8));
        JPanel filterOptions = new JPanel();
        filterOptions.setLayout(new BoxLayout(filterOptions, BoxLayout.Y_AXIS));
        filterOptions.setOpaque(false);
        filterOptions.setAlignmentX(LEFT_ALIGNMENT);
        filterOptions.add(createControlLabel("Category"));
        filterOptions.add(Box.createVerticalStrut(3));
        filterOptions.add(categoryFilter);
        filterOptions.add(Box.createVerticalStrut(7));
        filterOptions.add(createControlLabel("Status"));
        filterOptions.add(Box.createVerticalStrut(3));
        filterOptions.add(statusFilter);
        filterOptions.add(Box.createVerticalStrut(7));
        filterOptions.add(createControlLabel("Sort"));
        filterOptions.add(Box.createVerticalStrut(3));
        filterOptions.add(sortFilter);
        filterOptions.add(Box.createVerticalStrut(7));
        filterOptions.add(createControlLabel("Goal type"));
        filterOptions.add(Box.createVerticalStrut(3));
        filterOptions.add(typeFilter);
        filterOptions.setVisible(false);
        styleButton(filtersToggle);
        filtersToggle.setToolTipText("Show category, status, sort and goal-type options. The number indicates active options.");
        filtersToggle.setAlignmentX(LEFT_ALIGNMENT);
        filtersToggle.addActionListener(event -> {
            boolean show = !filterOptions.isVisible();
            filterOptions.setVisible(show);
            filtersToggle.setToolTipText(show ? "Hide filter options" : "Show filter options");
            revalidate();
            repaint();
        });
        controlsPanel.add(filtersToggle);
        controlsPanel.add(Box.createVerticalStrut(6));
        controlsPanel.add(filterOptions);

        JPanel actionRow = new JPanel(new BorderLayout(8, 0));
        actionRow.setBackground(PANEL_COLOR);
        actionRow.setOpaque(true);
        actionRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        actionRow.setAlignmentX(LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        resultsLabel.setForeground(MUTED_COLOR);
        resultsLabel.setFont(FontManager.getRunescapeSmallFont());

        styleButton(clearFiltersButton);
        clearFiltersButton.setToolTipText("Reset search, all filters and sorting. Saved goals are unaffected.");

        actionRow.add(resultsLabel, BorderLayout.CENTER);
        actionRow.add(clearFiltersButton, BorderLayout.EAST);
        controlsPanel.add(actionRow);
    }

    private JLabel createControlLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED_COLOR);
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFocusable(true);
        comboBox.setMaximumRowCount(12);
    }

    private void styleButton(JButton button) {
        button.setRolloverEnabled(true);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void installListeners() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                requestFilteredRender();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                requestFilteredRender();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                requestFilteredRender();
            }
        });

        categoryFilter.addActionListener(this::onFilterChanged);
        statusFilter.addActionListener(this::onFilterChanged);
        sortFilter.addActionListener(this::onFilterChanged);
        typeFilter.addActionListener(this::onFilterChanged);

        clearFiltersButton.addActionListener(event -> {
            searchField.setText("");
            categoryFilter.setSelectedItem(ALL_CATEGORIES);
            statusFilter.setSelectedItem(ALL_STATUSES);
            sortFilter.setSelectedItem(SORT_NAME);
            typeFilter.setSelectedItem(ALL_TYPES);
            render();
        });
    }

    private void onFilterChanged(ActionEvent event) {
        if (!updatingFilters) {
            syncQuickStatus();
            requestFilteredRender();
        }
    }

    private void toggleQuickStatus(String status) {
        statusFilter.setSelectedItem(status.equals(statusFilter.getSelectedItem()) ? ALL_STATUSES : status);
    }

    private void syncQuickStatus() {
        Object status = statusFilter.getSelectedItem();
        newGoalsButton.setSelected(NOT_STARTED.equals(status));
        activeGoalsButton.setSelected(IN_PROGRESS.equals(status));
        completeGoalsButton.setSelected(COMPLETE.equals(status));
    }

    private void setAllCategoriesCollapsed(boolean collapsed) {
        for (GoalCategory category : GoalCategory.values()) {
            collapsedCategories.put(category, collapsed);
        }
        for (com.grinder.repository.RepeatSchedule schedule : com.grinder.repository.RepeatSchedule.values()) { collapsedRepeats.put(schedule, collapsed); }
        render();
    }

    private void requestFilteredRender() {
        filterRenderTimer.restart();
    }

    public void load(GrinderProfile profile) {
        backupActions.cancel();
        this.profile = profile;
        SwingUtilities.invokeLater(() -> {
            undo.clear();
            undoDeleteButton.setVisible(false);
        });
        renderVersion.incrementAndGet();
        SwingUtilities.invokeLater(() -> SwingUtilities.updateComponentTreeUI(this));
        render();
    }

    public void unload() {
        backupActions.cancel();
        profile = null;
        SwingUtilities.invokeLater(() -> {
            undo.clear();
            undoDeleteButton.setVisible(false);
        });
        renderVersion.incrementAndGet();
        render();
    }

    public void render() {
        if (!renderQueued.compareAndSet(false, true)) {
            return;
        }

        clientThread.invokeLater(() -> {
            renderQueued.set(false);
            int version = renderVersion.incrementAndGet();
            GrinderProfile currentProfile = profile;
            List<GoalView> allGoals = new ArrayList<>();

            if (currentProfile != null && config.enabled()) {
                if (client.getGameState() == net.runelite.api.GameState.LOGGED_IN && client.getLocalPlayer() != null && currentProfile.getHash() == client.getAccountHash()) { currentProfile.observeRecurring(client, System.currentTimeMillis()); }
                for (Goal goal : currentProfile.getGoals()) {
                    if (goal == null || (!config.collectionLogEnabled() && isCollectionLogGoal(goal))
                            || (!config.levelGoalsEnabled() && isLevelGoal(goal))
                            || (!config.experienceGoalsEnabled() && isExperienceGoal(goal))
                            || (!config.trackerGoalsEnabled() && goal instanceof TrackerGoal)
                            || (!config.questGoalsEnabled() && goal.getCategory() == GoalCategory.QUEST)
                            || (!config.diaryGoalsEnabled() && goal.getCategory() == GoalCategory.ACHIEVEMENT_DIARY)
                            || (!config.combatAchievementGoalsEnabled() && goal.getCategory() == GoalCategory.COMBAT_ACHIEVEMENT)
                            || (!config.ownedItemGoalsEnabled() && goal instanceof com.grinder.content.goal.impl.item.PossessedItemGoal)
                            || (!config.customGoalsEnabled() && goal instanceof CustomGoal)) {
                        continue;
                    }

                    String title = safeTitle(goal);
                    com.grinder.repository.GoalMetadata metadata = currentProfile.metadata(goal);
                    com.grinder.repository.RecurringGoal recurring = metadata.recurring();
                    boolean repeating = recurring.isActive() && com.grinder.repository.RecurringGoal.supports(goal);
                    GoalComponent component = goal.build(client, config);
                    GoalStatus status = repeating ? recurring.getStatus() : goal.getStatus();
                    long progressCurrent = repeating ? recurring.getProgress() : goal.getProgressCurrent();
                    long progressMaximum = repeating ? recurring.getTarget() : goal.getProgressMaximum();
                    component.setGoalStatus(status);
                    component.putClientProperty("grinder.repeat", repeating ? recurring.getSchedule() : com.grinder.repository.RepeatSchedule.NONE);
                    if (repeating) { component.setProgress(progressCurrent, progressMaximum, String.format(Locale.ROOT, "%,d / %,d this period", progressCurrent, progressMaximum)); }
                    component.setExpansionListener(null);
                    component.setExpanded(!metadata.isCollapsed(config.collapseDetailsByDefault()));
                    component.setExpansionListener(expanded -> clientThread.invokeLater(() -> { if (profile == currentProfile && currentProfile.getGoals().contains(goal)) { currentProfile.metadata(goal).setCollapsed(!expanded); } }));
                    component.setMaximumProgressChunks(config.maxProgressChunks());
                    if (!repeating) { metadata.observe(goal, title, System.currentTimeMillis()); }
                    component.setGoalActions(event -> editGoal(currentProfile, goal, title),
                            event -> deleteGoal(currentProfile, goal, title));
                    if (goal instanceof CustomGoal) {
                        CustomGoal custom = (CustomGoal) goal;
                        component.setManualActions(progressCurrent, progressMaximum, () -> changeManualProgress(currentProfile, custom, value -> Math.min(custom.getProgressMaximum(), value + 1)), () -> changeManualProgress(currentProfile, custom, value -> value >= custom.getProgressMaximum() ? 0 : 1), () -> promptManualProgress(currentProfile, custom, progressCurrent, progressMaximum));
                    }
                    com.grinder.repository.RepeatSchedule repeatSchedule = recurring.getSchedule();
                    long repeatTarget = recurring.getTarget();
                    boolean pinned = metadata.isPinned();
                    component.putClientProperty("grinder.pinned", pinned);
                    component.addGoalAction(pinned ? "Unpin goal" : "Pin goal", "Pinned goals appear first, while still respecting your search and filters.", event -> clientThread.invokeLater(() -> {
                        if (profile == currentProfile && currentProfile.getGoals().contains(goal)) { currentProfile.metadata(goal).setPinned(!pinned); currentProfile.touch(); render(); }
                    }));
                    if (config.showCompletionHistory()) { component.addGoalAction("Goal history...", "View recurring periods, progress, outcomes and first-observed completion dates.", event -> clientThread.invokeLater(() -> {
                        if (profile != currentProfile || !currentProfile.getGoals().contains(goal)) { return; }
                        java.util.function.Supplier<com.grinder.ui.components.GoalHistoryPanel> history = com.grinder.ui.components.GoalHistoryPanel.snapshot(metadata);
                        SwingUtilities.invokeLater(() -> { if (profile == currentProfile) { JOptionPane.showMessageDialog(this, history.get(), title + " — history", JOptionPane.PLAIN_MESSAGE); } });
                    })); }
                    if (com.grinder.repository.RecurringGoal.supports(goal)) { component.addGoalAction("Repeat schedule...", "Make this a daily, weekly or monthly goal, or return to lifetime tracking.", event -> editRepeat(currentProfile, goal, repeatSchedule, repeatTarget)); }
                    component.setAchievementNote(repeating ? recurring.getSchedule() + " · " + (recurring.getBaselineAt() == 0 ? "Waiting for login baseline" : "Since " + java.time.format.DateTimeFormatter.ofPattern("MMM d HH:mm 'UTC'", Locale.ROOT).withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.ofEpochMilli(recurring.getBaselineAt()))) : config.showCompletionHistory() && !metadata.getCompletions().isEmpty() && status != GoalStatus.COMPLETE ? "Previously reached a target · currently incomplete" : null);

                    allGoals.add(new GoalView(
                            goal,
                            component,
                            safeTitle(goal),
                            safeCategory(goal),
                            status,
                            goal.hasProgress(),
                            progressCurrent,
                            progressMaximum));
                }
            }

            String updateText = currentProfile == null ? "Not tracking" : "Updated " + java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss z", Locale.ROOT).withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.now()) + "<br>" + HtmlUtils.escape(currentProfile.getPlayerStorage().freshness());
            SwingUtilities.invokeLater(() -> {
                if (version != renderVersion.get()) {
                    return;
                }

                updateStatusLabel.setText("<html>" + updateText + "</html>");
                updateStatusLabel.setVisible(config.showUpdateStatus());
                renderPrepared(currentProfile, allGoals);
            });
        });
    }

    private void renderPrepared(GrinderProfile currentProfile, List<GoalView> allGoals) {
        main.removeAll();

        if (currentProfile == null) {
            updateSummary(new ArrayList<>());
            resultsLabel.setText("0 goals");
            addMessage("No profile loaded", "Load a profile to view your goals.");
            refreshPanel();
            return;
        }

        if (!config.enabled()) {
            updateSummary(new ArrayList<>());
            resultsLabel.setText("0 goals");
            addMessage("Grinder is disabled", "Enable Grinder in the plugin configuration to display your goals.");
            refreshPanel();
            return;
        }

        updateCategoryOptions(allGoals);
        updateSummary(allGoals);

        List<GoalView> filteredGoals = filterGoals(allGoals);
        sortGoals(filteredGoals);
        resultsLabel.setText(filteredGoals.size() + " of " + allGoals.size() + " goals");
        resultsLabel.setToolTipText("Goals matching your search, filters and visibility settings.");

        if (filteredGoals.isEmpty()) {
            addMessage("No matching goals", "Try another search term or clear your filters.");
            refreshPanel();
            return;
        }

        List<GoalView> pinnedGoals = new ArrayList<>();
        List<GoalView> remainingGoals = new ArrayList<>();
        for (GoalView view : filteredGoals) {
            if (config.showPinnedSection() && Boolean.TRUE.equals(view.component.getClientProperty("grinder.pinned"))) { pinnedGoals.add(view); }
            else { remainingGoals.add(view); }
        }
        if (!pinnedGoals.isEmpty()) {
            JLabel pinnedHeading = new JLabel("Pinned goals");
            pinnedHeading.setFont(UiTypography.SECTION);
            pinnedHeading.setAlignmentX(LEFT_ALIGNMENT);
            main.add(pinnedHeading);
            main.add(Box.createVerticalStrut(8));
            for (GoalView view : pinnedGoals) { view.component.setAlignmentX(LEFT_ALIGNMENT); main.add(view.component); main.add(Box.createVerticalStrut(12)); }
        }
        if (config.groupRecurringGoals()) {
            for (com.grinder.repository.RepeatSchedule schedule : com.grinder.repository.RepeatSchedule.values()) {
                if (schedule == com.grinder.repository.RepeatSchedule.NONE) { continue; }
                List<GoalView> repeats = new ArrayList<>();
                for (GoalView view : remainingGoals) { if (view.component.getClientProperty("grinder.repeat") == schedule) { repeats.add(view); } }
                if (repeats.isEmpty()) { continue; }
                addRepeatSection(schedule, repeats);
                remainingGoals.removeAll(repeats);
            }
        }
        if (!config.categorizedGoals()) {
            for (GoalView view : remainingGoals) { view.component.setAlignmentX(LEFT_ALIGNMENT); main.add(view.component); main.add(Box.createVerticalStrut(12)); }
            refreshPanel();
            return;
        }
        Map<GoalCategory, List<GoalView>> groupedGoals = groupGoals(remainingGoals);

        for (Map.Entry<GoalCategory, List<GoalView>> entry : groupedGoals.entrySet()) {
            addCategorySection(entry.getKey(), entry.getValue());
            main.add(Box.createVerticalStrut(10));
        }

        refreshPanel();
    }

    private void updateSummary(List<GoalView> goals) {
        int completed = 0;
        int active = 0;
        int notStarted = 0;
        double ratios = 0;

        for (GoalView view : goals) {
            if (isComplete(view.status)) {
                completed++;
            }
            else if (view.status == GoalStatus.IN_PROGRESS) {
                active++;
            }
            else { notStarted++; }
            ratios += getProgressRatio(view);
        }

        int percentage = goals.isEmpty() ? 0 : (int) Math.round(ratios * 100 / goals.size());
        overallProgress.setMaximumChunks(config.maxProgressChunks());
        overallProgress.setProgress(percentage, 100);
        overallProgress.setStatus(!goals.isEmpty() && completed == goals.size() ? GoalStatus.COMPLETE
                : active > 0 || completed > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED);
        summaryLabel.setText(completed + " / " + goals.size() + " complete  •  " + percentage + "%");
        summaryLabel.setToolTipText("Average progress across your goals; each goal has equal weight");
        overallProgress.setToolTipText(percentage + "% average progress. Each goal has equal weight; yellow is active and green is complete.");
        newGoalsButton.setText("New " + notStarted);
        activeGoalsButton.setText("Active " + active);
        completeGoalsButton.setText("Done " + completed);
        syncQuickStatus();
    }

    private List<GoalView> filterGoals(List<GoalView> goals) {
        List<GoalView> filtered = new ArrayList<>();
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();
        String selectedType = (String) typeFilter.getSelectedItem();
        int filterCount = (ALL_CATEGORIES.equals(selectedCategory) ? 0 : 1)
                + (ALL_STATUSES.equals(selectedStatus) ? 0 : 1)
                + (ALL_TYPES.equals(selectedType) ? 0 : 1)
                + (SORT_NAME.equals(sortFilter.getSelectedItem()) ? 0 : 1);
        filtersToggle.setText(filterCount == 0 ? "Filters" : "Filters (" + filterCount + ")");

        for (GoalView view : goals) {
            if (!query.isEmpty() && !view.title.toLowerCase(Locale.ROOT).contains(query)
                    && !view.goal.getDescription().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }

            if (selectedCategory != null && !ALL_CATEGORIES.equals(selectedCategory) &&
                    !selectedCategory.equals(view.category.getDisplayName())) {
                continue;
            }

            if (!matchesStatus(view.status, selectedStatus, config.hideCompletedGoals())) {
                continue;
            }
            if (CUSTOM.equals(selectedType) && !(view.goal instanceof CustomGoal)
                    || AUTOMATIC.equals(selectedType) && view.goal instanceof CustomGoal) {
                continue;
            }

            filtered.add(view);
        }

        return filtered;
    }

    static boolean matchesStatus(GoalStatus status, String selectedStatus, boolean hideCompleted) {
        if (selectedStatus == null || ALL_STATUSES.equals(selectedStatus)) {
            return !hideCompleted || status != GoalStatus.COMPLETE;
        }

        if (UNFINISHED.equals(selectedStatus)) { return status != GoalStatus.COMPLETE; }

        String value = status == null ? "" : status.name();

        if (COMPLETE.equals(selectedStatus)) {
            return "COMPLETE".equalsIgnoreCase(value) || "COMPLETED".equalsIgnoreCase(value);
        }

        if (IN_PROGRESS.equals(selectedStatus)) {
            return "IN_PROGRESS".equalsIgnoreCase(value);
        }

        if (NOT_STARTED.equals(selectedStatus)) {
            return "NOT_STARTED".equalsIgnoreCase(value);
        }

        return true;
    }

    private void sortGoals(List<GoalView> goals) {
        String sort = (String) sortFilter.getSelectedItem();

        if (SORT_NAME_DESC.equals(sort)) {
            goals.sort(Comparator.comparing((GoalView view) -> view.title, String.CASE_INSENSITIVE_ORDER).reversed());
        }
        else if (SORT_CLOSEST.equals(sort)) {
            goals.sort(Comparator.comparing((GoalView view) -> isComplete(view.status))
                    .thenComparing(Comparator.comparingDouble(GrinderPanel::getProgressRatio).reversed())
                    .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
        else if (SORT_PROGRESS_DESC.equals(sort)) {
            goals.sort(Comparator.comparingDouble(GrinderPanel::getProgressRatio).reversed()
                    .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
        else if (SORT_PROGRESS_ASC.equals(sort)) {
            goals.sort(Comparator.comparingDouble(GrinderPanel::getProgressRatio)
                    .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
        else if (SORT_STATUS.equals(sort)) {
            goals.sort(Comparator.comparingInt((GoalView view) -> getStatusOrder(view.status))
                    .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
        else {
            goals.sort(Comparator.comparing((GoalView view) -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
    }

    private static double getProgressRatio(GoalView view) {
        if (!view.hasProgress || view.progressMaximum <= 0) {
            return isComplete(view.status) ? 1.0 : 0.0;
        }

        long current = Math.max(0, Math.min(view.progressCurrent, view.progressMaximum));
        return current / (double) view.progressMaximum;
    }

    private static int getStatusOrder(GoalStatus status) {
        if (status == GoalStatus.IN_PROGRESS) {
            return 0;
        }

        if (status == GoalStatus.NOT_STARTED) {
            return 1;
        }

        return 2;
    }

    private Map<GoalCategory, List<GoalView>> groupGoals(List<GoalView> goals) {
        Map<GoalCategory, List<GoalView>> grouped = new LinkedHashMap<>();

        for (GoalView view : goals) {
            grouped.computeIfAbsent(view.category, key -> new ArrayList<>()).add(view);
        }

        return grouped;
    }

    private void updateCategoryOptions(List<GoalView> goals) {
        String previous = (String) categoryFilter.getSelectedItem();
        Set<String> categories = new LinkedHashSet<>();
        categories.add(ALL_CATEGORIES);

        for (GoalView view : goals) {
            categories.add(view.category.getDisplayName());
        }

        updatingFilters = true;
        try {
            categoryFilter.setModel(new DefaultComboBoxModel<>(categories.toArray(new String[0])));
            categoryFilter.setSelectedItem(previous != null && categories.contains(previous) ? previous : ALL_CATEGORIES);
        }
        finally {
            updatingFilters = false;
        }
    }

    private void addRepeatSection(com.grinder.repository.RepeatSchedule schedule, List<GoalView> goals) {
        boolean collapsed = Boolean.TRUE.equals(collapsedRepeats.get(schedule));
        long now = System.currentTimeMillis();
        long minutes = Math.max(1, (schedule.next(schedule.start(now)) - now + 59_999) / 60_000);
        String countdown = minutes >= 1440 ? minutes / 1440 + "d " + minutes % 1440 / 60 + "h" : minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
        long completed = goals.stream().filter(view -> isComplete(view.status)).count();
        JButton heading = new JButton("<html>" + (collapsed ? "▶ " : "▼ ") + schedule + " · " + completed + "/" + goals.size() + "<br>Resets in " + countdown + " (UTC)</html>");
        heading.setFont(UiTypography.SECTION);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        heading.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 48));
        heading.setHorizontalAlignment(SwingConstants.LEFT);
        heading.setToolTipText("Click to expand or collapse. Resets use UTC: midnight daily, Monday weekly, first of the month monthly. Countdown refreshes while logged in.");
        heading.addActionListener(event -> { collapsedRepeats.put(schedule, !Boolean.TRUE.equals(collapsedRepeats.get(schedule))); render(); });
        main.add(heading);
        main.add(Box.createVerticalStrut(8));
        if (!collapsed) {
            for (GoalView view : goals) { view.component.setAlignmentX(LEFT_ALIGNMENT); main.add(view.component); main.add(Box.createVerticalStrut(12)); }
        }
    }

    private void addCategorySection(GoalCategory category, List<GoalView> goals) {
        boolean collapsed = Boolean.TRUE.equals(collapsedCategories.get(category));

        JPanel categorySection = new ContentHeightPanel();
        categorySection.setLayout(new BoxLayout(categorySection, BoxLayout.Y_AXIS));
        categorySection.setBackground(PANEL_COLOR);
        categorySection.setOpaque(true);
        categorySection.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        categorySection.setAlignmentX(LEFT_ALIGNMENT);

        JPanel categoryHeader = new JPanel(new BorderLayout(8, 0));
        categoryHeader.setBackground(HEADER_COLOR);
        categoryHeader.setOpaque(true);
        categoryHeader.setBorder(BorderFactory.createEmptyBorder(8, 9, 8, 9));
        categoryHeader.setAlignmentX(LEFT_ALIGNMENT);
        JButton toggleButton = new JButton(collapsed ? "\u25B6" : "\u25BC");
        toggleButton.setToolTipText(collapsed ? "Expand category" : "Collapse category");
        styleButton(toggleButton);
        toggleButton.putClientProperty("JButton.buttonType", "toolBarButton");
        toggleButton.addActionListener(event -> {
            collapsedCategories.put(category, !Boolean.TRUE.equals(collapsedCategories.get(category)));
            render();
        });

        JLabel iconLabel = new JLabel();
        if (category.getIcon() != null && category.getIcon().getImage() != null) {
            iconLabel.setIcon(new ImageIcon(category.getIcon().getImage().getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH)));
        }

        JLabel titleLabel = new JLabel(category.getDisplayName());
        titleLabel.setToolTipText("Click to " + (collapsed ? "expand " : "collapse ") + category.getDisplayName() + ".");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(UiTypography.SECTION);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
        titlePanel.setBackground(HEADER_COLOR);
        titlePanel.setOpaque(false);
        titlePanel.add(iconLabel);
        titlePanel.add(Box.createHorizontalStrut(7));
        titlePanel.add(titleLabel);

        JLabel countLabel = new JLabel(String.valueOf(goals.size()), SwingConstants.RIGHT);
        countLabel.setToolTipText("Goals in this category matching your current filters.");
        countLabel.setForeground(MUTED_COLOR);
        countLabel.setFont(FontManager.getRunescapeSmallFont());
        countLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        JPanel headerLeft = new JPanel();
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.X_AXIS));
        headerLeft.setBackground(HEADER_COLOR);
        headerLeft.setOpaque(false);
        headerLeft.add(toggleButton);
        headerLeft.add(Box.createHorizontalStrut(5));
        headerLeft.add(titlePanel);

        JPanel trailing = new JPanel(new BorderLayout(6, 0));
        trailing.setOpaque(false);
        trailing.add(countLabel, BorderLayout.WEST);

        java.awt.event.MouseAdapter categoryInteraction = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    toggleButton.doClick();
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                categoryHeader.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                categoryHeader.setBackground(HEADER_COLOR);
            }
        };
        for (Component part : new Component[]{categoryHeader, headerLeft, titlePanel,
                titleLabel, iconLabel, trailing, countLabel}) {
            part.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            part.addMouseListener(categoryInteraction);
        }

        categoryHeader.add(headerLeft, BorderLayout.CENTER);
        categoryHeader.add(trailing, BorderLayout.EAST);
        categorySection.add(categoryHeader);

        if (!collapsed) {
            JPanel goalsPanel = new ContentHeightPanel();
            goalsPanel.setLayout(new BoxLayout(goalsPanel, BoxLayout.Y_AXIS));
            goalsPanel.setBackground(PANEL_COLOR);
            goalsPanel.setOpaque(true);
            goalsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
            goalsPanel.setAlignmentX(LEFT_ALIGNMENT);

            for (GoalView view : goals) {
                if (goalsPanel.getComponentCount() > 0) {
                    goalsPanel.add(Box.createVerticalStrut(12));
                }
                view.component.setAlignmentX(LEFT_ALIGNMENT);
                goalsPanel.add(view.component);
            }

            categorySection.add(goalsPanel);
        }

        categoryHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, categoryHeader.getPreferredSize().height));
        main.add(categorySection);
    }

    private void addMessage(String title, String message) {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setBackground(PANEL_COLOR);
        emptyPanel.setOpaque(true);
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(18, 4, 18, 4));
        emptyPanel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(FontManager.getRunescapeBoldFont());

        JLabel messageLabel = new JLabel("<html><div style='width: 220px'>" + HtmlUtils.escape(message) + "</div></html>");
        messageLabel.setForeground(MUTED_COLOR);
        messageLabel.setFont(FontManager.getRunescapeSmallFont());

        emptyPanel.add(titleLabel);
        emptyPanel.add(Box.createVerticalStrut(6));
        emptyPanel.add(messageLabel);
        main.add(emptyPanel);
    }
    private void editRepeat(GrinderProfile active, Goal goal, com.grinder.repository.RepeatSchedule schedule, long target) {
        if (active == null || profile != active) { return; }
        com.grinder.ui.editor.RepeatGoalForm form = new com.grinder.ui.editor.RepeatGoalForm();
        form.load(schedule, target);
        while (JOptionPane.showConfirmDialog(this, form, "Repeat schedule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                form.commit();
                com.grinder.repository.RepeatSchedule selected = form.getSchedule();
                long selectedTarget = form.getTarget();
                clientThread.invokeLater(() -> {
                    if (profile == active && active.getGoals().contains(goal)) {
                        Goal collision = active.findMatchingGoal(goal, selected);
                        if (collision != null && collision != goal) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "That tracker already has a goal with this schedule.")); return; }
                        java.util.function.Consumer<Goal> restore = active.snapshotEdit(goal);
                        configureRepeat(active, goal, selected, selectedTarget);
                        active.touch();
                        recordGoalUndo(active, goal, "schedule change", () -> restore.accept(goal));
                        render();
                    }
                });
                return;
            } catch (java.text.ParseException exception) { JOptionPane.showMessageDialog(this, "Enter a valid period target."); }
        }
    }
    private void configureRepeat(GrinderProfile active, Goal goal, com.grinder.repository.RepeatSchedule schedule, long target) {
        if (!com.grinder.repository.RecurringGoal.supports(goal)) { return; }
        com.grinder.repository.RecurringGoal recurring = active.metadata(goal).recurring();
        boolean newSchedule = recurring.getSchedule() != schedule;
        recurring.configure(schedule, target, System.currentTimeMillis());
        if (goal instanceof CustomGoal && schedule != com.grinder.repository.RepeatSchedule.NONE) {
            CustomGoal custom = (CustomGoal) goal;
            custom.update(custom.getTitle(null), custom.getDescription(), custom.getCategory(), newSchedule ? 0 : Math.min(custom.getProgressCurrent(), target), target);
        }
    }
    private void transferBackup(boolean importing) {
        GrinderProfile active = profile;
        backupActions.show(this, active, () -> profile == active, () -> {
            if (profile == active) {
                for (Goal goal : active.getGoals()) { goal.refresh(client); }
                render();
            }
        }, importing);
    }
    private void addPreset(com.grinder.content.goal.GoalPreset preset) {
        GrinderProfile active = profile;
        if (active == null) { return; }
        List<Goal> goals = preset.createGoals();
        if (JOptionPane.showConfirmDialog(this, "Add " + goals.size() + " goals from " + preset + "? Existing goals and targets are kept.", "Add goal preset", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) { return; }
        clientThread.invokeLater(() -> {
            if (profile != active) { return; }
            int added = 0;
            for (Goal goal : goals) { if (active.add(goal)) { goal.refresh(client); added++; } }
            int count = added;
            render();
            SwingUtilities.invokeLater(() -> { if (profile == active) { JOptionPane.showMessageDialog(this, count + " goals added. Existing matches were skipped."); } });
        });
    }
    private void addAllCollectionPages() {
        GrinderProfile active = profile;
        if (active == null) { return; }
        clientThread.invokeLater(() -> {
            if (profile != active) { return; }
            Map<Integer, String> pages = com.grinder.content.collectionlog.CollectionLogPages.discover(client);
            SwingUtilities.invokeLater(() -> {
                if (profile != active) { return; }
                if (pages.isEmpty()) { JOptionPane.showMessageDialog(this, "Collection-log metadata is not ready. Log in and try again."); return; }
                if (JOptionPane.showConfirmDialog(this, "Add missing goals from all " + pages.size() + " collection-log pages? Existing goals are kept.", "Add collection-log pages", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) { return; }
                clientThread.invokeLater(() -> {
                    if (profile != active) { return; }
                    pages.forEach((id, name) -> active.add(new CollectionLogEntryGoal(itemManager, id, name)));
                    render();
                });
            });
        });
    }

    private void showAddGoalDialog() {
        GrinderProfile active = profile;
        if (active == null) {
            JOptionPane.showMessageDialog(this, "Log in to add goals to your player profile.");
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || client.getGameState() != net.runelite.api.GameState.LOGGED_IN) { return; }
            java.util.Map<Integer, java.util.Map<String, java.util.function.Supplier<Goal>>> choices =
                    com.grinder.ui.editor.CompletionGoalChoices.create(client);
            SwingUtilities.invokeLater(() -> {
                if (profile == active) { showAddGoalDialog(active, choices); }
            });
        });
    }

    private void showAddGoalDialog(GrinderProfile active,
            java.util.Map<Integer, java.util.Map<String, java.util.function.Supplier<Goal>>> choices) {
        GoalEditorPanel form = new GoalEditorPanel(itemManager, itemSearchService, choices);
        Goal candidate = GoalCreationDialog.showDialog(this, form);
        if (candidate == null) { return; }
        com.grinder.repository.RepeatSchedule schedule = form.getRepeatSchedule();
        long periodTarget = form.getRepeatTarget();
        clientThread.invokeLater(() -> {
            if (profile != active) { return; }
            Goal existing = active.findMatchingGoal(candidate, schedule);
            if (existing != null) {
                String title = safeTitle(existing);
                long oldTarget = existing.getProgressMaximum();
                long newTarget = candidate.getProgressMaximum();
                SwingUtilities.invokeLater(() -> {
                    if (profile != active) { return; }
                    if (existing.supportsTarget()) {
                        if (JOptionPane.showConfirmDialog(this,
                                title + " is already tracked.\nUpdate its target from "
                                        + String.format(Locale.ROOT, "%,d to %,d?", oldTarget, newTarget) + "\nRepeat: " + schedule + (schedule == com.grinder.repository.RepeatSchedule.NONE ? " (lifetime tracking)" : String.format(Locale.ROOT, " · %,d per period", periodTarget)) + "\nChanging repeat settings archives the current observed period.",
                                "Update existing goal", JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                            clientThread.invokeLater(() -> {
                                if (profile == active && active.getGoals().contains(existing)) {
                                    java.util.function.Consumer<Goal> restore = active.snapshotEdit(existing);
                                    existing.setTarget(newTarget);
                                    configureRepeat(active, existing, schedule, periodTarget);
                                    active.touch();
                                    recordGoalUndo(active, existing, "edit", () -> restore.accept(existing));
                                    render();
                                }
                            });
                        }
                    }
                    else {
                        JOptionPane.showMessageDialog(this, "This goal is already in your profile.");
                    }
                });
            }
            else {
                candidate.refresh(client);
                configureRepeat(active, candidate, schedule, periodTarget);
                active.add(candidate, active.metadata(candidate));
                recordUndo(active, "add goal", () -> active.remove(candidate), () -> active.getGoals().contains(candidate));
                SwingUtilities.invokeLater(() -> {
                    if (profile != active) { return; }
                    collapsedCategories.put(candidate.getCategory(), false);
                    clearFiltersButton.doClick();
                });
                render();
            }
        });
    }

    private void editGoal(GrinderProfile active, Goal goal, String title) {
        if (profile != active) { return; }
        if (goal instanceof CustomGoal) { editCustomGoal(active, (CustomGoal) goal, title); return; }
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) { return; }
            java.util.Map<Integer, java.util.Map<String, java.util.function.Supplier<Goal>>> choices = com.grinder.ui.editor.CompletionGoalChoices.create(client);
            com.grinder.repository.RepeatSchedule schedule = active.metadata(goal).recurring().getSchedule();
            long periodTarget = active.metadata(goal).recurring().getTarget();
            SwingUtilities.invokeLater(() -> {
                if (profile != active) { return; }
                GoalEditorPanel form = new GoalEditorPanel(itemManager, itemSearchService, choices);
                try { form.loadGoal(goal); }
                catch (IllegalArgumentException exception) { form.stopSearch(); JOptionPane.showMessageDialog(this, exception.getMessage()); return; }
                form.loadRepeat(schedule, periodTarget);
                Goal replacement = GoalCreationDialog.showDialog(this, form, "Edit " + title, "Save changes");
                if (replacement == null) { return; }
                com.grinder.repository.RepeatSchedule selectedSchedule = form.getRepeatSchedule();
                long selectedTarget = form.getRepeatTarget();
                clientThread.invokeLater(() -> {
                    if (profile != active) { return; }
                    try {
                        java.util.function.Consumer<Goal> restore = active.snapshotEdit(goal);
                        if (active.replaceGoal(goal, replacement, selectedSchedule)) { replacement.refresh(client); configureRepeat(active, replacement, selectedSchedule, selectedTarget); recordGoalUndo(active, replacement, "edit", () -> restore.accept(replacement)); render(); }
                    } catch (IllegalArgumentException exception) {
                        SwingUtilities.invokeLater(() -> { if (profile == active) { JOptionPane.showMessageDialog(this, exception.getMessage()); } });
                    }
                });
            });
        });
    }

    private void editCustomGoal(GrinderProfile active, CustomGoal goal, String title) {
        CustomGoalForm form = new CustomGoalForm();
        form.load(goal);
        com.grinder.ui.editor.RepeatGoalForm repeat = new com.grinder.ui.editor.RepeatGoalForm();
        repeat.load(active.metadata(goal).recurring().getSchedule(), active.metadata(goal).recurring().getTarget());
        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.add(form, BorderLayout.CENTER);
        editor.add(repeat, BorderLayout.SOUTH);
        while (JOptionPane.showConfirmDialog(this, editor, "Edit " + title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                CustomGoal draft = form.read();
                repeat.commit();
                com.grinder.repository.RepeatSchedule schedule = repeat.getSchedule();
                long periodTarget = repeat.getTarget();
                clientThread.invokeLater(() -> {
                    if (profile == active && active.getGoals().contains(goal)) {
                        java.util.function.Consumer<Goal> restore = active.snapshotEdit(goal);
                        goal.update(draft.getTitle(null), draft.getDescription(), draft.getCategory(),
                                draft.getProgressCurrent(), draft.getProgressMaximum());
                        configureRepeat(active, goal, schedule, periodTarget);
                        active.touch();
                        recordGoalUndo(active, goal, "edit", () -> restore.accept(goal));
                        render();
                    }
                });
                return;
            }
            catch (java.text.ParseException | IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(this, exception instanceof IllegalArgumentException
                        ? exception.getMessage() : "Enter valid progress and target numbers.",
                        "Check goal details", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteGoal(GrinderProfile active, Goal goal, String title) {
        if (profile != active || JOptionPane.showConfirmDialog(this,
                "Remove " + title + " from your goals?\nYour game progress is unaffected.",
                "Delete goal", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile == active && active.remove(goal)) {
                recordUndo(active, "delete", () -> active.add(goal, active.metadata(goal)), () -> active.findMatchingGoal(goal, active.metadata(goal).recurring().getSchedule()) == null);
                render();
            }
        });
    }

    private void undoDelete() {
        undoDeleteButton.setVisible(false);
        clientThread.invokeLater(() -> {
            if (!undo.undo(profile)) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Undo expired after a later edit, profile change or period reset.")); }
            render();
        });
    }

    private void recordUndo(GrinderProfile active, String label, Runnable action, java.util.function.BooleanSupplier allowed) {
        undo.record(active, label, action, allowed);
        SwingUtilities.invokeLater(() -> { if (profile == active) { undoDeleteButton.setText("Undo " + label); undoDeleteButton.setToolTipText("Undo the last change. Expires after a later edit or period reset."); undoDeleteButton.setVisible(true); } });
    }

    private void recordGoalUndo(GrinderProfile active, Goal goal, String label, Runnable action) {
        com.grinder.repository.RepeatSchedule schedule = active.metadata(goal).recurring().getSchedule();
        long period = schedule.start(System.currentTimeMillis());
        recordUndo(active, label, action, () -> active.getGoals().contains(goal) && (schedule == com.grinder.repository.RepeatSchedule.NONE || schedule.start(System.currentTimeMillis()) == period));
    }

    private void promptManualProgress(GrinderProfile active, CustomGoal goal, long current, long target) {
        javax.swing.JSpinner value = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(current, 0L, target, 1L));
        value.setEditor(new javax.swing.JSpinner.NumberEditor(value, "#,##0"));
        while (JOptionPane.showConfirmDialog(this, value, "Set progress (0–" + String.format(Locale.ROOT, "%,d", target) + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try { value.commitEdit(); changeManualProgress(active, goal, ((Number) value.getValue()).longValue()); return; }
            catch (java.text.ParseException exception) { JOptionPane.showMessageDialog(this, "Enter a whole number within the target."); }
        }
    }

    private void changeManualProgress(GrinderProfile active, CustomGoal goal, long value) {
        changeManualProgress(active, goal, current -> value);
    }
    private void changeManualProgress(GrinderProfile active, CustomGoal goal, java.util.function.LongUnaryOperator change) {
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) { return; }
            long now = System.currentTimeMillis();
            com.grinder.repository.RecurringGoal recurring = active.metadata(goal).recurring();
            if (recurring.isActive()) { recurring.observe(goal, now); }
            java.util.function.Consumer<Goal> restore = active.snapshotEdit(goal);
            goal.update(goal.getTitle(null), goal.getDescription(), goal.getCategory(), Math.min(change.applyAsLong(goal.getProgressCurrent()), goal.getProgressMaximum()), goal.getProgressMaximum());
            if (recurring.isActive()) { recurring.observe(goal, now); }
            else { active.metadata(goal).observe(goal, goal.getTitle(null), now); }
            active.touch();
            recordGoalUndo(active, goal, "progress", () -> restore.accept(goal));
            render();
        });
    }

    private String safeTitle(Goal goal) {
        String title = goal.getTitle(client);
        return title == null ? "" : title;
    }

    private GoalCategory safeCategory(Goal goal) {
        GoalCategory category = goal.getCategory();
        return category == null ? GoalCategory.OTHER : category;
    }

    private static boolean isComplete(GoalStatus status) {
        return status != null && ("COMPLETE".equalsIgnoreCase(status.name()) ||
                "COMPLETED".equalsIgnoreCase(status.name()));
    }

    private boolean isCollectionLogGoal(Goal goal) {
        return goal instanceof CollectionLogEntryGoal || goal instanceof CollectionLogItemGoal;
    }

    private boolean isLevelGoal(Goal goal) {
        return goal instanceof SkillLevelGoal || goal instanceof CombatLevelGoal || goal instanceof TotalLevelGoal;
    }

    private boolean isExperienceGoal(Goal goal) {
        return goal instanceof SkillExperienceGoal || goal instanceof TotalExperienceGoal;
    }


    private void refreshPanel() {
        main.revalidate();
        main.repaint();
        scrollPane.getViewport().revalidate();
        scrollPane.revalidate();
        scrollPane.repaint();
        revalidate();
        repaint();
    }
    public void refreshGoal(Goal goal) {
        if (goal == null) {
            return;
        }

        render();
    }

    public void refreshCategory(GoalCategory category) {
        if (category == null) {
            return;
        }

        refresh();
    }

    public void refreshAll() {
        render();
    }

    private void refresh() {
        render();
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/18/2026
     */
    private static final class GoalView {
        private final Goal goal;
        private final GoalComponent component;
        private final String title;
        private final GoalCategory category;
        private final GoalStatus status;
        private final boolean hasProgress;
        private final long progressCurrent;
        private final long progressMaximum;

        private GoalView(Goal goal, GoalComponent component, String title, GoalCategory category,
                         GoalStatus status, boolean hasProgress, long progressCurrent, long progressMaximum) {
            this.goal = goal;
            this.component = component;
            this.title = title;
            this.category = category;
            this.status = status;
            this.hasProgress = hasProgress;
            this.progressCurrent = progressCurrent;
            this.progressMaximum = progressMaximum;
        }
    }
}
