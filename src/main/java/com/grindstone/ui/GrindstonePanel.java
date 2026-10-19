package com.grindstone.ui;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.goal.BaseSkillGoal;
import com.grindstone.content.goal.Goal;
import com.grindstone.content.goal.GoalCategory;
import com.grindstone.content.goal.GoalPreset;
import com.grindstone.content.goal.GoalStatus;
import com.grindstone.content.goal.ManualGoal;
import com.grindstone.content.goal.TrackingState;
import com.grindstone.content.goal.impl.ChecklistGoal;
import com.grindstone.content.goal.impl.CustomGoal;
import com.grindstone.content.goal.impl.LinkedGoal;
import com.grindstone.content.goal.impl.TrackerGoal;
import com.grindstone.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grindstone.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grindstone.content.goal.impl.experience.BaseExperienceGoal;
import com.grindstone.content.goal.impl.experience.SessionExperienceGoal;
import com.grindstone.content.goal.impl.experience.SkillExperienceGoal;
import com.grindstone.content.goal.impl.experience.TotalExperienceGoal;
import com.grindstone.content.goal.impl.item.PossessedItemGoal;
import com.grindstone.content.goal.impl.level.BaseLevelGoal;
import com.grindstone.content.goal.impl.level.CombatLevelGoal;
import com.grindstone.content.goal.impl.level.SkillLevelGoal;
import com.grindstone.content.goal.impl.level.TotalLevelGoal;
import com.grindstone.notification.GoalCompletionNotifications;
import com.grindstone.repository.BulkGoalAction;
import com.grindstone.repository.BulkGoalChanges;
import com.grindstone.repository.GoalMetadata;
import com.grindstone.repository.GoalUndo;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.HistoryRecord;
import com.grindstone.repository.RecurringGoal;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.components.ContentHeightPanel;
import com.grindstone.ui.components.GoalComponent;
import com.grindstone.ui.components.GoalHistoryPanel;
import com.grindstone.ui.components.GoalListPanel;
import com.grindstone.ui.components.HistoryBrowserPanel;
import com.grindstone.ui.components.SegmentedProgressBar;
import com.grindstone.ui.editor.BulkGoalForm;
import com.grindstone.ui.editor.CompletionGoalChoices;
import com.grindstone.ui.editor.CustomGoalForm;
import com.grindstone.ui.editor.DeadlineForm;
import com.grindstone.ui.editor.GoalCreationDialog;
import com.grindstone.ui.editor.GoalEditorPanel;
import com.grindstone.ui.editor.LinkedGoalForm;
import com.grindstone.ui.editor.RepeatGoalForm;
import com.grindstone.ui.editor.RoutinePresetForm;
import com.grindstone.ui.item.ItemSearchService;
import com.grindstone.ui.util.GoalCardCache;
import com.grindstone.ui.util.SkillGoalGrouping;
import com.grindstone.ui.util.UiTooltips;
import com.grindstone.ui.util.UiTypography;
import com.grindstone.util.HtmlUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GrindstonePanel extends PluginPanel {
    @Inject
    private GoalCompletionNotifications completionNotifications;
    @Inject
    private ProfileBackupActions backupActions;
    private static final String ALL_CATEGORIES = "All categories";
    private static final String ALL_STATUSES = "All statuses";
    private static final String NOT_STARTED = "Not started";
    private static final String IN_PROGRESS = "In progress";
    private static final String COMPLETE = "Complete";
    private static final String UNFINISHED = "Unfinished";
    private static final String WAITING = "Waiting for data";
    private static final String ALL_SCHEDULES = "All schedules";
    private static final String ONE_OFF = "One-off goals";
    private static final String ALL_TYPES = "All goal types";
    private static final String AUTOMATIC = "Automatic";
    private static final String CUSTOM = "Custom";
    private static final String LINKED = "Linked";
    private static final String BASE = "Base stats";
    private static final String SESSION = "Session XP";
    private static final String DEADLINES = "With deadline";
    private static final String PAST_DUE = "Past due";
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
    private final GrindstoneConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ItemSearchService itemSearchService;

    private final JPanel main = new GoalListPanel();
    private final JPanel header = new JPanel();
    private final JPanel controlsPanel = new JPanel();
    private final JLabel summaryLabel = new JLabel("No profile loaded");
    private final JLabel progressPercent = new JLabel();
    private final JLabel filterSummary = new JLabel();
    private final JLabel updateStatusLabel = new JLabel();
    private final JLabel resultsLabel = new JLabel();
    private final SegmentedProgressBar overallProgress = new SegmentedProgressBar();
    private final IconTextField searchField = new IconTextField();
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<String> sortFilter = new JComboBox<>();
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final JComboBox<String> scheduleFilter = new JComboBox<>();
    private final JCheckBox pinnedOnly = new JCheckBox("Pinned goals only");
    private final JToggleButton newGoalsButton = new JToggleButton("New 0");
    private final JToggleButton activeGoalsButton = new JToggleButton("Active 0");
    private final JToggleButton completeGoalsButton = new JToggleButton("Done 0");
    private final JToggleButton waitingGoalsButton = new JToggleButton("Waiting 0");
    private final JButton filtersToggle = new JButton("Filters");
    private final JButton clearFiltersButton = new JButton("Clear");
    private final JButton undoDeleteButton = new JButton("Undo delete");
    private final JToggleButton todayButton = new JToggleButton("Today");
    private final JToggleButton allGoalsButton = new JToggleButton("All goals", true);
    private boolean todayMode;
    private String pendingReveal;
    private List<GoalView> preparedGoals = new ArrayList<>();
    private final GoalCardCache<GoalView> cardCache = new GoalCardCache<>();
    private List<?> layoutKey;
    private final Map<Goal, GoalComponent> displayedCards = new IdentityHashMap<>();
    private final Map<RepeatSchedule, JButton> repeatHeadings = new LinkedHashMap<>();
    private JPanel routineOverview;
    private JLabel routineTitle;
    private JLabel routineDue;
    private SegmentedProgressBar routineProgress;
    private final Component routineGap = Box.createVerticalStrut(12);
    private final GoalUndo undo = new GoalUndo();
    private final JScrollPane scrollPane;

    private final Map<GoalCategory, Boolean> collapsedCategories = new LinkedHashMap<>();
    private final Map<RepeatSchedule, Boolean> collapsedRepeats = new LinkedHashMap<>();
    private final AtomicInteger renderVersion = new AtomicInteger();
    private final AtomicBoolean renderQueued = new AtomicBoolean();
    private final Timer filterRenderTimer = new Timer(150, event -> render());

    private volatile GrindstoneProfile profile;
    private volatile String storageStatus;
    public void setStorageStatus(String status) {
        storageStatus = status;
        render();
    }
    private boolean updatingFilters;

    @Inject
    public GrindstonePanel(Client client, ClientThread clientThread, GrindstoneConfig config) {
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
        header.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel titleLabel = new JLabel("Grindstone Goals");
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
        titleLabel.setIcon(new ImageIcon(Icon.MAIN.getImage() .getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
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
        JPanel summaryRow = new JPanel(new BorderLayout(6, 0));
        summaryRow.setOpaque(false);
        summaryRow.setAlignmentX(LEFT_ALIGNMENT);
        summaryLabel.setFont(UiTypography.BODY);
        progressPercent.setFont(UiTypography.TITLE);
        progressPercent.setForeground(TEXT_COLOR);
        progressPercent.setToolTipText("Average progress across goals with available readings.");
        summaryRow.add(summaryLabel, BorderLayout.CENTER);
        summaryRow.add(progressPercent, BorderLayout.EAST);
        header.add(summaryRow);
        header.add(Box.createVerticalStrut(8));
        header.add(overallProgress);
        updateStatusLabel.setFont(UiTypography.META);
        updateStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
        updateStatusLabel.setToolTipText("Latest inventory, equipment or storage snapshot captured this login. This does not mean every storage has been loaded.");
        header.add(Box.createVerticalStrut(4));
        header.add(updateStatusLabel);

        JPanel counters = new JPanel(new GridLayout(2, 2, 4, 4));
        counters.setOpaque(false);
        counters.setAlignmentX(LEFT_ALIGNMENT);
        newGoalsButton.setForeground(SegmentedProgressBar.colorFor(GoalStatus.NOT_STARTED));
        activeGoalsButton.setForeground(SegmentedProgressBar.colorFor(GoalStatus.IN_PROGRESS));
        completeGoalsButton.setForeground(SegmentedProgressBar.colorFor(GoalStatus.COMPLETE));
        newGoalsButton.setToolTipText("Show not-started goals. Click again to show all statuses.");
        activeGoalsButton.setToolTipText("Show in-progress goals. Click again to show all statuses.");
        completeGoalsButton.setToolTipText("Show completed goals, including hidden completions. Click again to show all statuses.");
        waitingGoalsButton.setToolTipText("Show goals needing a login or native client reading. Click again to show all statuses.");
        for (JToggleButton counter : new JToggleButton[]{newGoalsButton, activeGoalsButton, completeGoalsButton, waitingGoalsButton}) {
            counter.setFont(UiTypography.BODY);
            counter.setMargin(new Insets(3, 3, 3, 3));
            counter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        newGoalsButton.addActionListener(event -> toggleQuickStatus(NOT_STARTED));
        activeGoalsButton.addActionListener(event -> toggleQuickStatus(IN_PROGRESS));
        completeGoalsButton.addActionListener(event -> toggleQuickStatus(COMPLETE));
        waitingGoalsButton.addActionListener(event -> toggleQuickStatus(WAITING));
        counters.add(newGoalsButton);
        counters.add(activeGoalsButton);
        counters.add(completeGoalsButton);
        counters.add(waitingGoalsButton);
        header.add(Box.createVerticalStrut(8));
        header.add(counters);

        JPanel goalActions = new JPanel(new GridLayout(2, 1, 0, 5));
        goalActions.setOpaque(false);
        goalActions.setAlignmentX(LEFT_ALIGNMENT);
        goalActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        JButton addGoal = new JButton("+ Add goal");
        styleButton(addGoal);
        addGoal.setIcon(new ImageIcon(ImageUtil.resizeImage(Icon.MAIN.getImage(), 16, 16)));
        addGoal.setIconTextGap(7);
        addGoal.setFont(UiTypography.TITLE);
        addGoal.setToolTipText("Create an automatic tracker or a custom goal with manual progress.");
        addGoal.addActionListener(event -> showAddGoalDialog());
        goalActions.add(addGoal);
        JButton addLinked = new JButton("+ Linked goal");
        styleButton(addLinked);
        addLinked.setToolTipText("Create a checklist, any-of goal, milestone ladder, collection subset, recorded achievement, repeat count or prerequisite goal from existing trackers.");
        addLinked.addActionListener(event -> editLinkedGoal(profile, null));
        goalActions.add(addLinked);
        JMenuItem allCollectionPages = new JMenuItem("Add all collection-log pages...");
        allCollectionPages.setToolTipText("Read every page from the client's five collection-log tabs and add missing page goals.");
        allCollectionPages.addActionListener(event -> addAllCollectionPages());
        moreMenu.add(allCollectionPages);
        JMenu presets = new JMenu("Add goal preset");
        presets.setToolTipText("Add a focused goal pack without replacing existing goals or edited targets.");
        for (GoalPreset preset : GoalPreset.values()) {
            JMenuItem option = new JMenuItem(preset.toString());
            option.addActionListener(event -> addPreset(preset));
            presets.add(option);
        }
        moreMenu.add(presets);
        JMenu routines = new JMenu("Recurring routines...");
        JMenuItem dailyRoutine = new JMenuItem("Daily skilling & personal");
        dailyRoutine.addActionListener(event -> addRoutine(false));
        JMenuItem weeklyRoutine = new JMenuItem("Weekly activities & personal");
        weeklyRoutine.addActionListener(event -> addRoutine(true));
        routines.add(dailyRoutine);
        routines.add(weeklyRoutine);
        moreMenu.add(routines);
        JMenuItem manage = new JMenuItem("Manage goals / bulk actions...");
        manage.setToolTipText("Search and select goals to pin, delete, categorize, change schedules or set deadlines.");
        manage.addActionListener(event -> showBulkManager());
        moreMenu.add(manage);
        JMenuItem clearGoals = new JMenuItem("Clear all goals...");
        clearGoals.setToolTipText("Remove all goals from this profile, including hidden goals. Confirmation and undo are available.");
        clearGoals.addActionListener(event -> clearAllGoals());
        moreMenu.add(clearGoals);
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
        JPanel navigation = new JPanel(new GridLayout(1, 3, 4, 0));
        navigation.setOpaque(false);
        navigation.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup views = new ButtonGroup();
        views.add(todayButton);
        views.add(allGoalsButton);
        todayButton.setToolTipText("Focus on recurring goals, ordered by next reset, with a period progress overview.");
        allGoalsButton.setToolTipText("Show lifetime and recurring goals with your normal filters.");
        todayButton.addActionListener(event -> {
            todayMode = true;
            render();
        });
        allGoalsButton.addActionListener(event -> {
            todayMode = false;
            render();
        });
        JButton history = new JButton("History");
        history.setToolTipText("Browse this account's goal history by date, schedule, goal and outcome.");
        history.addActionListener(event -> showAccountHistory());
        for (AbstractButton button : new AbstractButton[]{todayButton, allGoalsButton, history}) {
            button.setFont(UiTypography.META);
            button.setMargin(new Insets(5, 3, 5, 3));
            navigation.add(button);
        }
        header.add(Box.createVerticalStrut(6));
        header.add(navigation);
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
        searchField.setFont(UiTypography.BODY);
        UiTooltips.set(searchField, "Search titles, descriptions and categories. Every search word must match; word order does not matter.");
        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setAlignmentX(LEFT_ALIGNMENT);

        categoryFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_CATEGORIES}));
        statusFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_STATUSES, UNFINISHED, NOT_STARTED, IN_PROGRESS, COMPLETE, WAITING}));
        scheduleFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_SCHEDULES, ONE_OFF, "Daily", "Weekly", "Monthly"}));
        styleComboBox(scheduleFilter);
        scheduleFilter.setAlignmentX(LEFT_ALIGNMENT);
        scheduleFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        UiTooltips.set(scheduleFilter, "Show one-off goals or goals repeating daily, weekly or monthly.");
        pinnedOnly.setOpaque(false);
        pinnedOnly.setFont(UiTypography.BODY);
        pinnedOnly.setAlignmentX(LEFT_ALIGNMENT);
        pinnedOnly.setToolTipText("Show only goals you pinned, without changing your pins or other filters.");
        sortFilter.setModel(new DefaultComboBoxModel<>(new String[]{SORT_NAME, SORT_NAME_DESC, SORT_CLOSEST, SORT_PROGRESS_DESC, SORT_PROGRESS_ASC, SORT_STATUS}));
        typeFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_TYPES, AUTOMATIC, CUSTOM, LINKED, BASE, SESSION, DEADLINES, PAST_DUE}));
        styleComboBox(typeFilter);
        typeFilter.setAlignmentX(LEFT_ALIGNMENT);
        typeFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        styleComboBox(categoryFilter);
        styleComboBox(statusFilter);
        styleComboBox(sortFilter);
        UiTooltips.set(categoryFilter, "Show goals from one category, or choose All categories.");
        UiTooltips.set(statusFilter, "Filter by progress state. Complete also shows goals hidden by the completed-goals setting.");
        UiTooltips.set(sortFilter, "Sort within each category. Closest unfinished puts incomplete goals nearest their target first.");
        UiTooltips.set(typeFilter, "Custom includes manual checklists. Linked goals combine existing trackers. Filter base stats, session gains or personal deadlines without deleting anything.");

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
        filterOptions.add(Box.createVerticalStrut(7));
        filterOptions.add(createControlLabel("Schedule"));
        filterOptions.add(Box.createVerticalStrut(3));
        filterOptions.add(scheduleFilter);
        filterOptions.add(Box.createVerticalStrut(7));
        filterOptions.add(pinnedOnly);
        styleButton(clearFiltersButton);
        clearFiltersButton.setText("Clear filters");
        clearFiltersButton.setToolTipText("Reset search, filters and sorting.");
        clearFiltersButton.setAlignmentX(LEFT_ALIGNMENT);
        clearFiltersButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        filterOptions.add(Box.createVerticalStrut(8));
        filterOptions.add(clearFiltersButton);
        JScrollPane filterScroll = new JScrollPane(filterOptions);
        filterScroll.setBorder(BorderFactory.createEmptyBorder());
        filterScroll.setOpaque(false);
        filterScroll.getViewport().setOpaque(false);
        filterScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        filterScroll.getVerticalScrollBar().setUnitIncrement(16);
        filterScroll.setAlignmentX(LEFT_ALIGNMENT);
        filterScroll.setPreferredSize(new Dimension(200, 180));
        filterScroll.setMinimumSize(new Dimension(0, 100));
        filterScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        filterScroll.setVisible(false);
        styleButton(filtersToggle);
        filtersToggle.setToolTipText("Show category, status, type, schedule, pin and sort options. The number includes search and active selections.");
        filtersToggle.setAlignmentX(LEFT_ALIGNMENT);
        filtersToggle.addActionListener(event -> {
            boolean show = !filterScroll.isVisible();
            filterScroll.setVisible(show);
            filtersToggle.setToolTipText(show ? "Hide filter options" : "Show filter options");
            revalidate();
            repaint();
        });
        controlsPanel.add(filtersToggle);
        controlsPanel.add(Box.createVerticalStrut(6));
        controlsPanel.add(filterScroll);
        filterSummary.setFont(UiTypography.META);
        filterSummary.setForeground(MUTED_COLOR);
        filterSummary.setAlignmentX(LEFT_ALIGNMENT);
        filterSummary.setVisible(false);
        controlsPanel.add(filterSummary);

        JPanel actionRow = new JPanel(new BorderLayout(8, 0));
        actionRow.setBackground(PANEL_COLOR);
        actionRow.setOpaque(true);
        actionRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        actionRow.setAlignmentX(LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        resultsLabel.setForeground(MUTED_COLOR);
        resultsLabel.setFont(FontManager.getRunescapeSmallFont());

        actionRow.add(resultsLabel, BorderLayout.CENTER);
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
        comboBox.setFont(UiTypography.BODY);
        comboBox.setFocusable(true);
        comboBox.setMaximumRowCount(12);
    }

    private void styleButton(JButton button) {
        button.setFont(UiTypography.BODY);
        button.setMargin(new Insets(4, 7, 4, 7));
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        scheduleFilter.addActionListener(this::onFilterChanged);
        pinnedOnly.addActionListener(this::onFilterChanged);

        clearFiltersButton.addActionListener(event -> {
            searchField.setText("");
            categoryFilter.setSelectedItem(ALL_CATEGORIES);
            statusFilter.setSelectedItem(ALL_STATUSES);
            sortFilter.setSelectedItem(SORT_NAME);
            typeFilter.setSelectedItem(ALL_TYPES);
            scheduleFilter.setSelectedItem(ALL_SCHEDULES);
            pinnedOnly.setSelected(false);
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
        waitingGoalsButton.setSelected(WAITING.equals(status));
    }

    private void setAllCategoriesCollapsed(boolean collapsed) {
        for (GoalCategory category : GoalCategory.values()) {
            collapsedCategories.put(category, collapsed);
        }
        for (RepeatSchedule schedule : RepeatSchedule.values()) {
            collapsedRepeats.put(schedule, collapsed);
        }
        render();
    }

    private void requestFilteredRender() {
        filterRenderTimer.restart();
    }

    public void load(GrindstoneProfile profile) {
        cardCache.clear();
        pendingReveal = null;
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
        cardCache.clear();
        pendingReveal = null;
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
            GrindstoneProfile currentProfile = profile;
            List<GoalView> allGoals = new ArrayList<>();

            if (currentProfile != null && config.enabled()) {
                cardCache.retain(currentProfile.getGoals());
                if (client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && currentProfile.getHash() == client.getAccountHash()) {
                    currentProfile.observeRecurring(client, System.currentTimeMillis());
                    completionNotifications.observe(currentProfile);
                }
                for (Goal goal : currentProfile.getGoals()) {
                    if (!config.linkedGoalsEnabled() && goal instanceof LinkedGoal) {
                        continue;
                    }
                    if (goal == null || (!config.collectionLogEnabled() && isCollectionLogGoal(goal)) || (!config.levelGoalsEnabled() && isLevelGoal(goal)) || (!config.experienceGoalsEnabled() && isExperienceGoal(goal)) || (!config.trackerGoalsEnabled() && goal instanceof TrackerGoal) || (!config.questGoalsEnabled() && goal.getCategory() == GoalCategory.QUEST) || (!config.diaryGoalsEnabled() && goal.getCategory() == GoalCategory.ACHIEVEMENT_DIARY) || (!config.combatAchievementGoalsEnabled() && goal.getCategory() == GoalCategory.COMBAT_ACHIEVEMENT) || (!config.ownedItemGoalsEnabled() && goal instanceof PossessedItemGoal) || (!config.customGoalsEnabled() && goal instanceof ManualGoal)) {
                        continue;
                    }

                    String title = safeTitle(goal);
                    GoalMetadata metadata = currentProfile.metadata(goal);
                    RecurringGoal recurring = metadata.recurring();
                    boolean repeating = recurring.isActive() && RecurringGoal.supports(goal);
                    TrackingState tracking = TrackingState.of(goal, recurring, client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == currentProfile.getHash(), currentProfile.getPlayerStorage().hasBankSnapshot());
                    List<?> key = Arrays.asList(currentProfile, metadata, title, goal.getDescription(), goal.getProgressCurrent(), goal.getProgressMaximum(), goal.getStatus(), recurring.getSchedule(), recurring.getProgress(), recurring.getTarget(), recurring.getBaselineAt(), recurring.getStart(), metadata.isPinned(), metadata.isCollapsed(config.collapseDetailsByDefault()), metadata.getCompletions().size(), tracking, config.showGoalIcons(), config.showProgressBar(), config.maxProgressChunks(), config.showCompletionHistory(), config.showActivityIcons(), config.itemsPerRow(), config.maxItemRows(), config.showTrackingState(), config.showObtainedItems(), config.showItemTooltips());
                    if (goal instanceof ChecklistGoal) {
                        key = Arrays.asList(key, Arrays.hashCode(((ChecklistGoal) goal).getChecks()));
                    }
                    key = Arrays.asList(key, metadata.getDeadlineAt(), metadata.getDeadlineAt() > 0 && System.currentTimeMillis() >= metadata.getDeadlineAt(), metadata.getDisplayCategory());
                    GoalView cached = cardCache.get(goal, key);
                    if (cached != null && tracking != TrackingState.WAITING && goal.getProgressMaximum() > 0) {
                        allGoals.add(cached);
                        continue;
                    }
                    GoalComponent component = goal.build(client, config);
                    if (goal instanceof ChecklistGoal) {
                        ChecklistGoal checklist = (ChecklistGoal) goal;
                        boolean[] checked = checklist.getChecks();
                        RepeatSchedule openedSchedule = recurring.getSchedule();
                        long openedPeriod = openedSchedule.start(System.currentTimeMillis());
                        for (int step = 0; step < checked.length; step++) {
                            int index = step;
                            JCheckBox check = new JCheckBox("", checked[step]);
                            check.getAccessibleContext().setAccessibleName(checklist.getSteps().get(step));
                            check.setOpaque(false);
                            check.setFont(UiTypography.BODY);
                            check.setToolTipText("Manually mark this personal step complete. No game action is performed; repeating checklists reset at their UTC boundary.");
                            check.setAlignmentX(LEFT_ALIGNMENT);
                            check.addActionListener(event -> {
                                boolean selected = check.isSelected();
                                clientThread.invokeLater(() -> {
                                    if (profile == currentProfile && currentProfile.getGoals().contains(goal) && currentProfile.metadata(goal).recurring().getSchedule() == openedSchedule && openedSchedule.start(System.currentTimeMillis()) == openedPeriod) {
                                        Consumer<Goal> restore = currentProfile.snapshotEdit(goal);
                                        checklist.setChecked(index, selected);
                                        currentProfile.touch();
                                        recordGoalUndo(currentProfile, goal, "checklist step", () -> restore.accept(goal));
                                    }
                                    refreshAll();
                                });
                            });
                            JPanel stepRow = new ContentHeightPanel();
                            stepRow.setLayout(new BorderLayout(5, 0));
                            stepRow.setOpaque(false);
                            stepRow.setAlignmentX(LEFT_ALIGNMENT);
                            JTextArea stepText = new JTextArea(checklist.getSteps().get(step), 0, 14);
                            stepText.setEditable(false);
                            stepText.setLineWrap(true);
                            stepText.setWrapStyleWord(true);
                            stepText.setOpaque(false);
                            stepText.setFont(UiTypography.BODY);
                            stepText.setToolTipText(check.getToolTipText());
                            stepRow.add(check, BorderLayout.WEST);
                            stepRow.add(stepText, BorderLayout.CENTER);
                            component.addDetail(stepRow);
                        }
                    }
                    tracking = TrackingState.of(goal, recurring, client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == currentProfile.getHash(), currentProfile.getPlayerStorage().hasBankSnapshot());
                    GoalStatus status = repeating ? recurring.getStatus() : goal.getStatus();
                    long progressCurrent = repeating ? recurring.getProgress() : goal.getProgressCurrent();
                    long progressMaximum = repeating ? recurring.getTarget() : goal.getProgressMaximum();
                    if (tracking == TrackingState.WAITING) {
                        status = GoalStatus.NOT_STARTED;
                        progressCurrent = 0;
                        progressMaximum = 0;
                    }
                    component.setGoalStatus(status);
                    component.putClientProperty("grindstone.repeat", repeating ? recurring.getSchedule() : RepeatSchedule.NONE);
                    component.putClientProperty("grindstone.id", metadata.getId());
                    component.putClientProperty("grindstone.ready", tracking != TrackingState.WAITING);
                    if (repeating) {
                        component.setProgress(progressCurrent, progressMaximum, String.format(Locale.ROOT, "%,d / %,d this period", progressCurrent, progressMaximum));
                    }
                    if (tracking == TrackingState.WAITING) {
                        component.setProgress(0, 0, "Waiting for client data");
                    } else if (tracking == TrackingState.OBSERVED_LOG) {
                        component.setProgress(progressCurrent, progressMaximum, String.format(Locale.ROOT, "%,d / %,d", progressCurrent, progressMaximum));
                    } else if (tracking == TrackingState.PARTIAL_STORAGE || tracking == TrackingState.SNAPSHOT) {
                        component.setProgress(progressCurrent, progressMaximum, String.format(Locale.ROOT, "%,d / %,d", progressCurrent, progressMaximum));
                    }
                    String baselineDetail = recurring.getBaselineAt() == 0 ? "" : " Baseline: " + Instant.ofEpochMilli(recurring.getBaselineAt()) + ".";
                    if (!repeating && tracking != TrackingState.WAITING) {
                        metadata.observe(goal, title, System.currentTimeMillis());
                    }
                    long observedAt = repeating ? recurring.getLastObservedAt() : metadata.getLastObservedAt();
                    String sourceDetail = goal.getCardDescription().isEmpty() ? goal.getDescription() : "";
                    component.setReliability(config.showTrackingState() ? tracking.getCardLabel() : null, tracking.getDetail() + baselineDetail + (observedAt == 0 ? "" : " Last reading: " + Instant.ofEpochMilli(observedAt) + ".") + (sourceDetail.isEmpty() ? "" : " " + sourceDetail));
                    component.setDeadlineNote(metadata.deadlineText(System.currentTimeMillis()));
                    component.putClientProperty("grindstone.deadline", metadata.getDeadlineAt());
                    component.setExpansionListener(null);
                    component.setExpanded(!metadata.isCollapsed(config.collapseDetailsByDefault()));
                    component.setExpansionListener(expanded -> clientThread.invokeLater(() -> {
                        if (profile == currentProfile && currentProfile.getGoals().contains(goal)) {
                            currentProfile.metadata(goal).setCollapsed(!expanded);
                        }
                    }));
                    component.setMaximumProgressChunks(config.maxProgressChunks());
                    component.setGoalActions(event -> editGoal(currentProfile, goal, title), event -> deleteGoal(currentProfile, goal, title));
                    component.addGoalAction("Deadline / reminder...", "Set a personal UTC deadline and optional reminder in Grindstone's header.", event -> editDeadline(currentProfile, goal));
                    if (goal instanceof CustomGoal) {
                        CustomGoal custom = (CustomGoal) goal;
                        long shownCurrent = progressCurrent;
                        long shownTarget = progressMaximum;
                        component.setManualActions(shownCurrent, shownTarget, () -> changeManualProgress(currentProfile, custom, value -> Math.min(custom.getProgressMaximum(), value + 1)), () -> changeManualProgress(currentProfile, custom, value -> value >= custom.getProgressMaximum() ? 0 : 1), () -> promptManualProgress(currentProfile, custom, shownCurrent, shownTarget));
                    }
                    RepeatSchedule repeatSchedule = recurring.getSchedule();
                    long repeatTarget = recurring.getTarget();
                    boolean pinned = metadata.isPinned();
                    component.putClientProperty("grindstone.pinned", pinned);
                    component.addGoalAction(pinned ? "Unpin goal" : "Pin goal", "Pinned goals appear first, while still respecting your search and filters.", event -> clientThread.invokeLater(() -> {
                        if (profile == currentProfile && currentProfile.getGoals().contains(goal)) {
                            currentProfile.metadata(goal).setPinned(!pinned);
                            currentProfile.touch();
                            render();
                        }
                    }));
                    if (config.showCompletionHistory()) {
                        component.addGoalAction("Goal history...", "View recurring periods, progress, outcomes and first-observed completion dates.", event -> clientThread.invokeLater(() -> {
                            if (profile != currentProfile || !currentProfile.getGoals().contains(goal)) {
                                return;
                            }
                            Supplier<GoalHistoryPanel> history = GoalHistoryPanel.snapshot(metadata);
                            SwingUtilities.invokeLater(() -> {
                                if (profile == currentProfile) {
                                    JOptionPane.showMessageDialog(this, history.get(), title + " — history", JOptionPane.PLAIN_MESSAGE);
                                }
                            });
                        }));
                    }
                    if (RecurringGoal.supports(goal)) {
                        component.addGoalAction("Repeat schedule...", "Make this a daily, weekly or monthly goal, or return to lifetime tracking.", event -> editRepeat(currentProfile, goal, repeatSchedule, repeatTarget));
                    }
                    if (repeating) {
                        component.addGoalAction("Restart current period...", "Archive observed progress and capture a fresh baseline. Game progress is never reset.", event -> restartPeriod(currentProfile, goal));
                    }
                    if (goal instanceof SessionExperienceGoal) {
                        component.addGoalAction("Restart session XP...", "Clear only this goal's observed login gains and capture the current XP as its new baseline.", event -> {
                            if (JOptionPane.showConfirmDialog(this, "Reset this goal's session XP progress? Your game XP and saved completion history are unchanged.", "Restart session goal", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                                clientThread.invokeLater(() -> {
                                    if (profile == currentProfile && currentProfile.getGoals().contains(goal) && hasClientProfile(currentProfile)) {
                                        ((SessionExperienceGoal) goal).restart();
                                        goal.refresh(client);
                                        currentProfile.touch();
                                        refreshAll();
                                    }
                                });
                            }
                        });
                    }
                    component.setAchievementNote(repeating ? recurring.getSchedule() + " · " + (recurring.getBaselineAt() == 0 ? "Baseline pending" : "This period") : config.showCompletionHistory() && !metadata.getCompletions().isEmpty() && status != GoalStatus.COMPLETE ? "Previously reached · now incomplete" : null);

                    GoalView view = new GoalView( goal, component, safeTitle(goal), safeCategory(goal), status, goal.hasProgress(), progressCurrent, progressMaximum);
                    cardCache.put(goal, key, view);
                    allGoals.add(view);
                }
            }

            String updateText = currentProfile == null ? "" : HtmlUtils.escape(currentProfile.getPlayerStorage().snapshotLabel());
            long dueCount = currentProfile == null || !config.showDeadlineReminders() ? 0 : currentProfile.getGoals().stream().filter(goal -> {
                GoalMetadata metadata = currentProfile.metadata(goal);
                return metadata.isDeadlineReminder() && metadata.getDeadlineAt() > 0 && metadata.getDeadlineAt() - System.currentTimeMillis() <= 86_400_000L && (metadata.recurring().isActive() ? metadata.recurring().getStatus() : goal.getStatus()) != GoalStatus.COMPLETE;
            }).count();
            SwingUtilities.invokeLater(() -> {
                if (version != renderVersion.get()) {
                    return;
                }

                updateStatusLabel.setText("<html>" + (storageStatus == null ? "" : HtmlUtils.escape(storageStatus) + "<br>") + (dueCount == 0 ? "" : dueCount + " personal deadline(s) due within 24h / past due<br>") + (config.showUpdateStatus() ? updateText : "") + "</html>");
                updateStatusLabel.setVisible(config.showUpdateStatus() && currentProfile != null || storageStatus != null || dueCount > 0);
                updateStatusLabel.setToolTipText("Latest inventory, equipment or storage snapshot captured this login. This does not mean every storage has been loaded.");
                renderPrepared(currentProfile, allGoals);
            });
        });
    }

    private void renderPrepared(GrindstoneProfile currentProfile, List<GoalView> allGoals) {
        if (currentProfile == null) {
            main.removeAll();
            layoutKey = null;
            updateSummary(new ArrayList<>());
            resultsLabel.setText("0 goals");
            addMessage("Log in to get started", "Your goals load automatically for your logged-in character.");
            refreshPanel();
            return;
        }

        if (!config.enabled()) {
            main.removeAll();
            layoutKey = null;
            updateSummary(new ArrayList<>());
            resultsLabel.setText("0 goals");
            addMessage("Grindstone is disabled", "Enable Grindstone in the plugin configuration to display your goals.");
            refreshPanel();
            return;
        }

        preparedGoals = allGoals;
        updateCategoryOptions(allGoals);
        updateSummary(allGoals);

        List<GoalView> filteredGoals = filterGoals(allGoals);
        if (todayMode) {
            filteredGoals.removeIf(view -> view.component.getClientProperty("grindstone.repeat") == RepeatSchedule.NONE);
        }
        sortGoals(filteredGoals);
        if (todayMode) {
            filteredGoals.sort(Comparator.comparingLong((GoalView view) -> nextReset(view)).thenComparing(view -> isComplete(view.status)));
        }
        resultsLabel.setText(filteredGoals.size() + " of " + allGoals.size() + " goals");
        resultsLabel.setToolTipText("Goals matching your search, filters and visibility settings.");

        List<Object> rows = new ArrayList<>();
        for (GoalView view : filteredGoals) {
            rows.add(Arrays.asList(view.goal, view.category, view.component.getClientProperty("grindstone.repeat"), view.component.getClientProperty("grindstone.pinned")));
        }
        List<?> nextLayout = Arrays.asList(currentProfile, todayMode, config.showTodayOverview(), config.categorizedGoals(), config.groupRecurringGoals(), config.groupSkillGoals(), config.showPinnedSection(), new LinkedHashMap<>(collapsedCategories), new LinkedHashMap<>(collapsedRepeats), rows, allGoals.size());
        if (nextLayout.equals(layoutKey)) {
            for (GoalView view : filteredGoals) {
                GoalComponent previous = displayedCards.put(view.goal, view.component);
                if (previous != null && previous != view.component && previous.getParent() != null) {
                    Container parent = previous.getParent();
                    int index = Arrays.asList(parent.getComponents()).indexOf(previous);
                    view.component.setAlignmentX(previous.getAlignmentX());
                    parent.remove(previous);
                    parent.add(view.component, index);
                    parent.revalidate();
                    parent.repaint();
                }
            }
            for (Map.Entry<RepeatSchedule, JButton> entry : repeatHeadings.entrySet()) {
                List<GoalView> repeats = new ArrayList<>();
                for (GoalView view : filteredGoals) {
                    if (view.component.getClientProperty("grindstone.repeat") == entry.getKey() && !(config.showPinnedSection() && Boolean.TRUE.equals(view.component.getClientProperty("grindstone.pinned")))) {
                        repeats.add(view);
                    }
                }
                updateRepeatHeading(entry.getKey(), repeats, entry.getValue());
            }
            if (todayMode || config.showTodayOverview()) {
                addTodayOverview(allGoals);
            }
            refreshPanel();
            return;
        }
        layoutKey = nextLayout;
        main.removeAll();
        displayedCards.clear();
        repeatHeadings.clear();
        for (GoalView view : filteredGoals) {
            displayedCards.put(view.goal, view.component);
        }
        if (todayMode || config.showTodayOverview()) {
            addTodayOverview(allGoals);
        }

        if (filteredGoals.isEmpty()) {
            if (currentProfile.getGoals().isEmpty()) {
                addMessage("No goals yet", "Choose Add goal to start tracking a skill, activity, item or personal goal.");
            } else {
                addMessage(todayMode ? "No matching routine goals" : "No matching goals", todayMode ? "Clear filters or choose Repeat in a supported goal's editor." : "Try another search term or clear your filters.");
            }
            refreshPanel();
            return;
        }

        List<GoalView> pinnedGoals = new ArrayList<>();
        List<GoalView> remainingGoals = new ArrayList<>();
        for (GoalView view : filteredGoals) {
            if (config.showPinnedSection() && Boolean.TRUE.equals(view.component.getClientProperty("grindstone.pinned"))) {
                pinnedGoals.add(view);
            } else {
                remainingGoals.add(view);
            }
        }
        if (!pinnedGoals.isEmpty()) {
            JLabel pinnedHeading = new JLabel("Pinned goals");
            pinnedHeading.setFont(UiTypography.SECTION);
            pinnedHeading.setAlignmentX(LEFT_ALIGNMENT);
            main.add(pinnedHeading);
            main.add(Box.createVerticalStrut(8));
            for (GoalView view : pinnedGoals) {
                view.component.setAlignmentX(LEFT_ALIGNMENT);
                main.add(view.component);
                main.add(Box.createVerticalStrut(12));
            }
        }
        if (config.groupRecurringGoals()) {
            for (RepeatSchedule schedule : RepeatSchedule.values()) {
                if (schedule == RepeatSchedule.NONE) {
                    continue;
                }
                List<GoalView> repeats = new ArrayList<>();
                for (GoalView view : remainingGoals) {
                    if (view.component.getClientProperty("grindstone.repeat") == schedule) {
                        repeats.add(view);
                    }
                }
                if (repeats.isEmpty()) {
                    continue;
                }
                addRepeatSection(schedule, repeats);
                remainingGoals.removeAll(repeats);
            }
        }
        if (!config.categorizedGoals()) {
            for (GoalView view : remainingGoals) {
                view.component.setAlignmentX(LEFT_ALIGNMENT);
                main.add(view.component);
                main.add(Box.createVerticalStrut(12));
            }
            refreshPanel();
            return;
        }
        Map<GoalCategory, List<GoalView>> groupedGoals = groupGoals(remainingGoals);

        for (Map.Entry<GoalCategory, List<GoalView>> entry : groupedGoals.entrySet()) {
            addCategorySection(entry.getKey(), entry.getValue());
            main.add(Box.createVerticalStrut(10));
        }

        refreshPanel();
        revealPrepared(allGoals);
    }

    private void addTodayOverview(List<GoalView> goals) {
        List<GoalView> recurring = new ArrayList<>();
        for (GoalView view : goals) {
            if (view.component.getClientProperty("grindstone.repeat") != RepeatSchedule.NONE) {
                recurring.add(view);
            }
        }
        if (recurring.isEmpty()) {
            if (routineOverview != null) {
                main.remove(routineOverview);
                main.remove(routineGap);
            }
            return;
        }
        long completed = recurring.stream().filter(view -> isComplete(view.status)).count();
        long waiting = recurring.stream().filter(view -> !Boolean.TRUE.equals(view.component.getClientProperty("grindstone.ready"))).count();
        double ratio = recurring.stream().filter(view -> Boolean.TRUE.equals(view.component.getClientProperty("grindstone.ready"))).mapToDouble(GrindstonePanel::getProgressRatio).average().orElse(0);
        if (routineOverview != null) {
            routineTitle.setText("Your routine · " + completed + "/" + recurring.size() + " reached");
            routineProgress.setProgress(Math.round(ratio * 100), 100);
            routineProgress.setVisible(waiting < recurring.size());
            long next = recurring.stream().mapToLong(this::nextReset).min().orElse(0);
            routineDue.setText("<html>Next reset: " + DateTimeFormatter.ofPattern("EEE HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(next)) + "<br>" + (waiting == recurring.size() ? "Waiting for readings" : Math.round(ratio * 100) + "% of known progress") + (waiting > 0 ? " · " + waiting + " waiting" : "") + "</html>");
            if (routineOverview.getParent() != main) {
                main.add(routineOverview, 0);
                main.add(routineGap, 1);
            }
            return;
        }
        JPanel overview = new ContentHeightPanel();
        routineOverview = overview;
        overview.setLayout(new BoxLayout(overview, BoxLayout.Y_AXIS));
        overview.setBackground(HEADER_COLOR);
        overview.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        overview.setAlignmentX(LEFT_ALIGNMENT);
        JLabel title = new JLabel("Your routine · " + completed + "/" + recurring.size() + " reached");
        routineTitle = title;
        title.setFont(UiTypography.TITLE);
        title.setAlignmentX(LEFT_ALIGNMENT);
        overview.add(title);
        overview.add(Box.createVerticalStrut(6));
        SegmentedProgressBar progress = new SegmentedProgressBar();
        routineProgress = progress;
        progress.setVisible(waiting < recurring.size());
        progress.setProgress(Math.round(ratio * 100), 100);
        progress.setToolTipText("Average goal completion, not a sum of different units. Includes all visible recurring goals, independent of search filters.");
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progress.setAlignmentX(LEFT_ALIGNMENT);
        overview.add(progress);
        long next = recurring.stream().mapToLong(this::nextReset).min().orElse(0);
        JLabel due = new JLabel("<html>Next reset: " + DateTimeFormatter.ofPattern("EEE HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(next)) + "<br>" + (waiting == recurring.size() ? "Waiting for readings" : Math.round(ratio * 100) + "% of known progress") + (waiting > 0 ? " · " + waiting + " waiting" : "") + "</html>");
        routineDue = due;
        due.setFont(UiTypography.META);
        due.setAlignmentX(LEFT_ALIGNMENT);
        overview.add(Box.createVerticalStrut(6));
        overview.add(due);
        main.add(overview, 0);
        main.add(routineGap, 1);
    }

    private long nextReset(GoalView view) {
        RepeatSchedule schedule = (RepeatSchedule) view.component.getClientProperty("grindstone.repeat");
        return schedule == RepeatSchedule.NONE ? Long.MAX_VALUE : schedule.next(schedule.start(System.currentTimeMillis()));
    }

    private void updateSummary(List<GoalView> goals) {
        int completed = 0;
        int active = 0;
        int notStarted = 0;
        double ratios = 0;
        int known = 0;

        for (GoalView view : goals) {
            if (!Boolean.TRUE.equals(view.component.getClientProperty("grindstone.ready"))) {
                continue;
            }
            if (isComplete(view.status)) {
                completed++;
            } else if (view.status == GoalStatus.IN_PROGRESS) {
                active++;
            } else {
                notStarted++;
            }
            ratios += getProgressRatio(view);
            known++;
        }

        int percentage = known == 0 ? 0 : (int) Math.round(ratios * 100 / known);
        overallProgress.setVisible(known > 0);
        overallProgress.setMaximumChunks(config.maxProgressChunks());
        overallProgress.setProgress(percentage, 100);
        overallProgress.setStatus(!goals.isEmpty() && completed == goals.size() ? GoalStatus.COMPLETE : active > 0 || completed > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED);
        summaryLabel.setText(goals.isEmpty() ? "Create your first goal" : completed + " of " + goals.size() + " completed");
        summaryLabel.setToolTipText("Overview of all enabled goals, independent of search and filters.");
        progressPercent.setText(known == 0 ? "—" : percentage + "%");
        overallProgress.setToolTipText("Average completion of goals with available readings.");
        newGoalsButton.setText("New " + notStarted);
        activeGoalsButton.setText("Active " + active);
        completeGoalsButton.setText("Done " + completed);
        waitingGoalsButton.setText("Waiting " + (goals.size() - known));
        syncQuickStatus();
    }

    private List<GoalView> filterGoals(List<GoalView> goals) {
        List<GoalView> filtered = new ArrayList<>();
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();
        String selectedType = (String) typeFilter.getSelectedItem();
        String selectedSchedule = (String) scheduleFilter.getSelectedItem();
        int filterCount = (ALL_CATEGORIES.equals(selectedCategory) ? 0 : 1)
                + (ALL_STATUSES.equals(selectedStatus) ? 0 : 1)
                + (ALL_TYPES.equals(selectedType) ? 0 : 1)
                + (SORT_NAME.equals(sortFilter.getSelectedItem()) ? 0 : 1)
                + (ALL_SCHEDULES.equals(selectedSchedule) ? 0 : 1)
                + (pinnedOnly.isSelected() ? 1 : 0)
                + (query.isEmpty() ? 0 : 1);
        filtersToggle.setText(filterCount == 0 ? "Filters" : "Filters (" + filterCount + ")");
        clearFiltersButton.setEnabled(filterCount > 0);
        List<String> selections = new ArrayList<>();
        if (!query.isEmpty()) {
            selections.add("Search: " + searchField.getText().trim());
        }
        if (!ALL_CATEGORIES.equals(selectedCategory)) {
            selections.add(selectedCategory);
        }
        if (!ALL_STATUSES.equals(selectedStatus)) {
            selections.add(selectedStatus);
        }
        if (!ALL_TYPES.equals(selectedType)) {
            selections.add(selectedType);
        }
        if (!ALL_SCHEDULES.equals(selectedSchedule)) {
            selections.add(selectedSchedule);
        }
        if (pinnedOnly.isSelected()) {
            selections.add("Pinned only");
        }
        if (!SORT_NAME.equals(sortFilter.getSelectedItem())) {
            selections.add("Sort: " + sortFilter.getSelectedItem());
        }
        if (config.hideCompletedGoals() && ALL_STATUSES.equals(selectedStatus)) {
            selections.add("Completed hidden by settings");
        }
        filterSummary.setVisible(!selections.isEmpty());
        String selectionText = String.join(" · ", selections);
        filterSummary.setText("<html><div style='width: 190px'>" + HtmlUtils.escape(selectionText) + "</div></html>");
        filterSummary.setToolTipText(selectionText);

        for (GoalView view : goals) {
            if (!matchesSearch(query, view.title, view.goal.getDescription(), view.category.getDisplayName())) {
                continue;
            }

            if (selectedCategory != null && !ALL_CATEGORIES.equals(selectedCategory) && !selectedCategory.equals(view.category.getDisplayName())) {
                continue;
            }

            if (!matchesStatus(view.status, selectedStatus, config.hideCompletedGoals(), Boolean.TRUE.equals(view.component.getClientProperty("grindstone.ready")))) {
                continue;
            }
            if (!matchesSchedule((RepeatSchedule) view.component.getClientProperty("grindstone.repeat"), selectedSchedule) || pinnedOnly.isSelected() && !Boolean.TRUE.equals(view.component.getClientProperty("grindstone.pinned"))) {
                continue;
            }
            Object deadline = view.component.getClientProperty("grindstone.deadline");
            if (!matchesType(view.goal, view.status, selectedType, deadline instanceof Long ? (Long) deadline : 0, System.currentTimeMillis())) {
                continue;
            }

            filtered.add(view);
        }

        return filtered;
    }

    static boolean matchesSearch(String query, String title, String description, String category) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String text = ((title == null ? "" : title) + " " + (description == null ? "" : description) + " " + (category == null ? "" : category)).toLowerCase(Locale.ROOT);
        for (String word : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!text.contains(word)) {
                return false;
            }
        }
        return true;
    }

    static boolean matchesSchedule(RepeatSchedule schedule, String selected) {
        if (selected == null || ALL_SCHEDULES.equals(selected)) {
            return true;
        }
        RepeatSchedule actual = schedule == null ? RepeatSchedule.NONE : schedule;
        return ONE_OFF.equals(selected) ? actual == RepeatSchedule.NONE : actual != RepeatSchedule.NONE && actual.name().equalsIgnoreCase(selected);
    }

    static boolean matchesStatus(GoalStatus status, String selected, boolean hideCompleted, boolean ready) {
        if (WAITING.equals(selected)) {
            return !ready;
        }
        if (!ready && (NOT_STARTED.equals(selected) || IN_PROGRESS.equals(selected) || COMPLETE.equals(selected))) {
            return false;
        }
        return matchesStatus(status, selected, hideCompleted);
    }

    static boolean matchesType(Goal goal, GoalStatus status, String selectedType, long deadline, long now) {
        if (CUSTOM.equals(selectedType)) {
            return goal instanceof ManualGoal;
        }
        if (AUTOMATIC.equals(selectedType)) {
            return !(goal instanceof ManualGoal) && !(goal instanceof LinkedGoal);
        }
        if (LINKED.equals(selectedType)) {
            return goal instanceof LinkedGoal;
        }
        if (BASE.equals(selectedType)) {
            return goal instanceof BaseSkillGoal;
        }
        if (SESSION.equals(selectedType)) {
            return goal instanceof SessionExperienceGoal;
        }
        if (DEADLINES.equals(selectedType)) {
            return deadline > 0;
        }
        if (PAST_DUE.equals(selectedType)) {
            return deadline > 0 && now >= deadline && !isComplete(status);
        }
        return true;
    }

    static boolean matchesStatus(GoalStatus status, String selectedStatus, boolean hideCompleted) {
        if (selectedStatus == null || ALL_STATUSES.equals(selectedStatus)) {
            return !hideCompleted || status != GoalStatus.COMPLETE;
        }

        if (UNFINISHED.equals(selectedStatus)) {
            return status != GoalStatus.COMPLETE;
        }

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
        } else if (SORT_CLOSEST.equals(sort)) {
            goals.sort(Comparator.comparing((GoalView view) -> isComplete(view.status)) .thenComparing(Comparator.comparingDouble(GrindstonePanel::getProgressRatio).reversed()) .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_PROGRESS_DESC.equals(sort)) {
            goals.sort(Comparator.comparingDouble(GrindstonePanel::getProgressRatio).reversed() .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_PROGRESS_ASC.equals(sort)) {
            goals.sort(Comparator.comparingDouble(GrindstonePanel::getProgressRatio) .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_STATUS.equals(sort)) {
            goals.sort(Comparator.comparingInt((GoalView view) -> getStatusOrder(view.status)) .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        } else {
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
        } finally {
            updatingFilters = false;
        }
    }

    private void addRepeatSection(RepeatSchedule schedule, List<GoalView> goals) {
        boolean collapsed = Boolean.TRUE.equals(collapsedRepeats.get(schedule));
        JButton heading = new JButton();
        updateRepeatHeading(schedule, goals, heading);
        repeatHeadings.put(schedule, heading);
        heading.setFont(UiTypography.SECTION);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        heading.setHorizontalAlignment(SwingConstants.LEFT);
        heading.setToolTipText("Click to expand or collapse. Resets use UTC: midnight daily, Monday weekly, first of the month monthly. Countdown refreshes while logged in.");
        heading.addActionListener(event -> {
            collapsedRepeats.put(schedule, !Boolean.TRUE.equals(collapsedRepeats.get(schedule)));
            render();
        });
        main.add(heading);
        main.add(Box.createVerticalStrut(8));
        if (!collapsed) {
            for (GoalView view : goals) {
                view.component.setAlignmentX(LEFT_ALIGNMENT);
                main.add(view.component);
                main.add(Box.createVerticalStrut(12));
            }
        }
    }
    private void updateRepeatHeading(RepeatSchedule schedule, List<GoalView> goals, JButton heading) {
        boolean collapsed = Boolean.TRUE.equals(collapsedRepeats.get(schedule));
        long now = System.currentTimeMillis();
        long minutes = Math.max(1, (schedule.next(schedule.start(now)) - now + 59_999) / 60_000);
        String countdown = minutes >= 1440 ? minutes / 1440 + "d " + minutes % 1440 / 60 + "h" : minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
        long completed = goals.stream().filter(view -> isComplete(view.status)).count();
        heading.setText("<html>" + (collapsed ? "▶ " : "▼ ") + schedule + " · " + completed + "/" + goals.size() + "<br>Resets in " + countdown + " (UTC)</html>");
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
            iconLabel.setIcon(new ImageIcon(category.getIcon().getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
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

        MouseAdapter categoryInteraction = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    toggleButton.doClick();
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                categoryHeader.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                categoryHeader.setBackground(HEADER_COLOR);
            }
        };
        for (Component part : new Component[]{categoryHeader, headerLeft, titlePanel, titleLabel, iconLabel, trailing, countLabel}) {
            part.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

            for (List<GoalView> group : config.groupSkillGoals() ? SkillGoalGrouping.group(goals, view -> view.goal) : goals.stream().map(Collections::singletonList).collect(Collectors.toList())) {
                if (goalsPanel.getComponentCount() > 0) {
                    goalsPanel.add(Box.createVerticalStrut(12));
                }
                if (group.size() == 1) {
                    group.get(0).component.setAlignmentX(LEFT_ALIGNMENT);
                    goalsPanel.add(group.get(0).component);
                    continue;
                }
                JPanel skillGroup = new ContentHeightPanel();
                skillGroup.setLayout(new BoxLayout(skillGroup, BoxLayout.Y_AXIS));
                skillGroup.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                skillGroup.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), BorderFactory.createEmptyBorder(6, 4, 4, 4)));
                skillGroup.setAlignmentX(LEFT_ALIGNMENT);
                JLabel heading = new JLabel(SkillGoalGrouping.skill(group.get(0).goal).getName());
                heading.setFont(UiTypography.TITLE);
                heading.setBorder(BorderFactory.createEmptyBorder(0, 5, 6, 0));
                heading.setToolTipText("Related goals for this skill. Each target keeps its own progress, actions and history.");
                skillGroup.add(heading);
                for (GoalView view : group) {
                    view.component.setAlignmentX(LEFT_ALIGNMENT);
                    skillGroup.add(view.component);
                    skillGroup.add(Box.createVerticalStrut(4));
                }
                goalsPanel.add(skillGroup);
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
    private void editRepeat(GrindstoneProfile active, Goal goal, RepeatSchedule schedule, long target) {
        if (active == null || profile != active) {
            return;
        }
        RepeatGoalForm form = new RepeatGoalForm();
        form.load(schedule, target);
        if (goal instanceof ChecklistGoal) {
            form.setFixedTarget(goal.getProgressMaximum());
        }
        while (JOptionPane.showConfirmDialog(this, form, "Repeat schedule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                form.commit();
                RepeatSchedule selected = form.getSchedule();
                long selectedTarget = form.getTarget();
                if ((schedule != selected || target != selectedTarget) && JOptionPane.showConfirmDialog(this, "Repeat: " + schedule + " → " + selected + "\nPeriod target: " + target + " → " + selectedTarget + "\nThe current observed period will be archived. Undo is available until a later edit or reset.", "Preview schedule change", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile == active && active.getGoals().contains(goal)) {
                        Goal collision = active.findMatchingGoal(goal, selected);
                        if (collision != null && collision != goal) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "That tracker already has a goal with this schedule."));
                            return;
                        }
                        Consumer<Goal> restore = active.snapshotEdit(goal);
                        configureRepeat(active, goal, selected, selectedTarget);
                        active.touch();
                        recordGoalUndo(active, goal, "schedule change", () -> restore.accept(goal));
                        render();
                    }
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(this, "Enter a valid period target.");
            }
        }
    }
    private void restartPeriod(GrindstoneProfile active, Goal goal) {
        if (profile != active || JOptionPane.showConfirmDialog(this, "Archive the current observed period and capture a fresh baseline?\nCustom progress resets to zero. Your game progress is unaffected.\nUndo is available until a later edit or scheduled reset.", "Restart current period", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) {
                return;
            }
            RecurringGoal recurring = active.metadata(goal).recurring();
            if (!recurring.isActive()) {
                return;
            }
            Consumer<Goal> restore = active.snapshotEdit(goal);
            recurring.restart(System.currentTimeMillis());
            if (goal instanceof ManualGoal) {
                ((ManualGoal) goal).resetProgress();
            }
            active.touch();
            recordGoalUndo(active, goal, "period restart", () -> restore.accept(goal));
            render();
        });
    }

    private void configureRepeat(GrindstoneProfile active, Goal goal, RepeatSchedule schedule, long target) {
        if (!RecurringGoal.supports(goal)) {
            return;
        }
        RecurringGoal recurring = active.metadata(goal).recurring();
        if (goal instanceof ChecklistGoal) {
            target = goal.getProgressMaximum();
            if (schedule != RepeatSchedule.NONE && (recurring.getSchedule() != schedule || recurring.getTarget() != target)) {
                ((ChecklistGoal) goal).resetProgress();
            }
        }
        boolean newSchedule = recurring.getSchedule() != schedule;
        recurring.configure(schedule, target, System.currentTimeMillis());
        if (goal instanceof CustomGoal && schedule != RepeatSchedule.NONE) {
            CustomGoal custom = (CustomGoal) goal;
            custom.update(custom.getTitle(null), custom.getDescription(), custom.getCategory(), newSchedule ? 0 : Math.min(custom.getProgressCurrent(), target), target);
        }
    }
    private void transferBackup(boolean importing) {
        GrindstoneProfile active = profile;
        backupActions.show(this, active, () -> profile == active, () -> {
            if (profile == active) {
                for (Goal goal : active.getGoals()) {
                    goal.refresh(client);
                }
                render();
            }
        }, importing);
    }
    private void addPreset(GoalPreset preset) {
        GrindstoneProfile active = profile;
        if (active == null) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            List<Goal> goals = preset.createGoals();
            String summary = additionSummary(active, goals, goal -> RepeatSchedule.NONE);
            SwingUtilities.invokeLater(() -> {
                if (profile != active || JOptionPane.showConfirmDialog(this, previewPane(summary), "Preview " + preset, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    List<Goal> before = new ArrayList<>(active.getGoals());
                    for (Goal goal : goals) {
                        if (active.add(goal)) {
                            goal.refresh(client);
                        }
                    }
                    recordAddedUndo(active, before, "preset");
                    render();
                });
            });
        });
    }


    private String additionSummary(GrindstoneProfile active, List<Goal> candidates, Function<Goal, RepeatSchedule> schedule) {
        StringBuilder rows = new StringBuilder();
        int missing = 0;
        for (Goal goal : candidates) {
            RepeatSchedule repeat = schedule.apply(goal);
            boolean exists = active.findMatchingGoal(goal, repeat) != null;
            if (!exists) {
                missing++;
            }
            rows.append(exists ? "Keep existing · " : "Add · ").append(goal.getTitle(null)).append(" · ").append(repeat);
            if (goal.getProgressMaximum() > 0) {
                rows.append(" · target ").append(String.format(Locale.ROOT, "%,d", goal.getProgressMaximum()));
            }
            rows.append('\n');
        }
        return missing + " new goals · " + (candidates.size() - missing) + " existing matches kept\nExisting targets and schedules are never overwritten. Additions can be undone.\n\n" + rows;
    }

    private JScrollPane previewPane(String summary) {
        JTextArea preview = new JTextArea(summary, 14, 44);
        preview.setEditable(false);
        preview.setFont(UiTypography.BODY);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setCaretPosition(0);
        return new JScrollPane(preview);
    }
    private void addAllCollectionPages() {
        GrindstoneProfile active = profile;
        if (active == null) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            Map<Integer, String> pages = CollectionLogPages.discover(client);
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                if (pages.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Collection-log metadata is not ready. Log in and try again.");
                    return;
                }
                if (JOptionPane.showConfirmDialog(this, "Add missing goals from all " + pages.size() + " collection-log pages? Existing goals are kept.", "Add collection-log pages", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    List<Goal> before = new ArrayList<>(active.getGoals());
                    pages.forEach((id, name) -> active.add(new CollectionLogEntryGoal(itemManager, id, name)));
                    recordAddedUndo(active, before, "collection pages");
                    render();
                });
            });
        });
    }

    private void addRoutine(boolean weekly) {
        GrindstoneProfile active = profile;
        if (active == null) {
            return;
        }
        RoutinePresetForm form = new RoutinePresetForm(weekly);
        while (JOptionPane.showConfirmDialog(this, form, "Preview recurring routine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                List<Goal> selected = form.selectedGoals();
                Map<Goal, GoalMetadata> metadata = new IdentityHashMap<>();
                for (Goal goal : selected) {
                    metadata.put(goal, form.metadata(goal, System.currentTimeMillis()));
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    List<Goal> before = new ArrayList<>(active.getGoals());
                    for (Goal goal : selected) {
                        if (active.add(goal, metadata.get(goal))) {
                            goal.refresh(client);
                        }
                    }
                    recordAddedUndo(active, before, "routine");
                    render();
                    int count = active.getGoals().size() - before.size();
                    SwingUtilities.invokeLater(() -> {
                        if (profile == active) {
                            JOptionPane.showMessageDialog(this, count + " routine goals added. Existing schedule matches were skipped.");
                        }
                    });
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(this, "Enter valid targets for the selected goals.");
            }
        }
    }

    private void recordAddedUndo(GrindstoneProfile active, List<Goal> before, String label) {
        List<Goal> added = new ArrayList<>(active.getGoals());
        added.removeAll(before);
        if (added.isEmpty()) {
            return;
        }
        active.touch();
        recordUndo(active, label, () -> added.forEach(active::remove), () -> active.getGoals().containsAll(added));
    }

    private void showAccountHistory() {
        GrindstoneProfile active = profile;
        if (active == null) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            List<HistoryRecord> records = HistoryRecord.snapshot(active, this::safeTitle);
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Account goal history", Dialog.ModalityType.APPLICATION_MODAL);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setContentPane(new HistoryBrowserPanel(records, id -> {
                    dialog.dispose();
                    revealGoal(active, id);
                }));
                dialog.pack();
                Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                dialog.setSize(Math.min(dialog.getWidth(), bounds.width - 40), Math.min(dialog.getHeight(), bounds.height - 40));
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            });
        });
    }

    private void revealGoal(GrindstoneProfile active, String id) {
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            Goal goal = active.getGoals().stream().filter(value -> active.metadata(value).getId().equals(id)).findFirst().orElse(null);
            if (goal == null) {
                return;
            }
            String title = safeTitle(goal);
            boolean complete = active.metadata(goal).recurring().isActive() ? active.metadata(goal).recurring().getStatus() == GoalStatus.COMPLETE : goal.getStatus() == GoalStatus.COMPLETE;
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                todayMode = false;
                allGoalsButton.setSelected(true);
                clearFiltersButton.doClick();
                searchField.setText(title);
                statusFilter.setSelectedItem(complete ? COMPLETE : UNFINISHED);
                pendingReveal = id;
                setAllCategoriesCollapsed(false);
            });
        });
    }

    private void revealPrepared(List<GoalView> goals) {
        if (pendingReveal == null) {
            return;
        }
        String id = pendingReveal;
        pendingReveal = null;
        for (GoalView view : goals) {
            if (id.equals(view.component.getClientProperty("grindstone.id")) && main.isAncestorOf(view.component)) {
                SwingUtilities.invokeLater(() -> view.component.scrollRectToVisible(new Rectangle(0, 0, view.component.getWidth(), view.component.getHeight())));
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "This goal is hidden by its tracker visibility setting. Enable that goal type in Grindstone configuration to reveal it.");
    }

    private void showAddGoalDialog() {
        showAddGoalDialog(null);
    }

    public void showAddGoalDialog(Goal initialGoal) {
        GrindstoneProfile active = profile;
        if (active == null) {
            JOptionPane.showMessageDialog(this, "Log in to add goals to your player profile.");
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || client.getGameState() != GameState.LOGGED_IN) {
                return;
            }
            Map<Integer, Map<String, Supplier<Goal>>> choices =
            CompletionGoalChoices.create(client);
            Map<Goal, RepeatSchedule> existing = new LinkedHashMap<>();
            for (Goal goal : active.getGoals()) {
                existing.put(goal, active.metadata(goal).recurring().getSchedule());
            }
            SwingUtilities.invokeLater(() -> {
                if (profile == active) {
                    showAddGoalDialog(active, choices, existing, initialGoal);
                }
            });
        });
    }

    private void showAddGoalDialog(GrindstoneProfile active, Map<Integer, Map<String, Supplier<Goal>>> choices, Map<Goal, RepeatSchedule> existingGoalsSnapshot, Goal initialGoal) {
        GoalEditorPanel form = new GoalEditorPanel(itemManager, itemSearchService, choices);
        form.setExistingGoals(existingGoalsSnapshot);
        if (initialGoal != null) {
            try {
                form.loadGoal(initialGoal);
            } catch (IllegalArgumentException exception) {
                form.stopSearch();
                JOptionPane.showMessageDialog(this, exception.getMessage(), "Add goal", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        Goal candidate = GoalCreationDialog.showDialog(this, form);
        if (candidate == null) {
            return;
        }
        RepeatSchedule schedule = form.getRepeatSchedule();
        long periodTarget = form.getRepeatTarget();
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            Goal existing = active.findMatchingGoal(candidate, schedule);
            if (existing != null) {
                String title = safeTitle(existing);
                long oldTarget = existing.getProgressMaximum();
                long newTarget = candidate.getProgressMaximum();
                SwingUtilities.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    if (existing.supportsTarget()) {
                        if (JOptionPane.showConfirmDialog(this, title + " is already tracked.\nUpdate its target from " + String.format(Locale.ROOT, "%,d to %,d?", oldTarget, newTarget) + "\nRepeat: " + schedule + (schedule == RepeatSchedule.NONE ? " (lifetime tracking)" : String.format(Locale.ROOT, " · %,d per period", periodTarget)) + "\nChanging repeat settings archives the current observed period.", "Update existing goal", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                            clientThread.invokeLater(() -> {
                                if (profile == active && active.getGoals().contains(existing)) {
                                    Consumer<Goal> restore = active.snapshotEdit(existing);
                                    existing.setTarget(newTarget);
                                    configureRepeat(active, existing, schedule, periodTarget);
                                    active.touch();
                                    recordGoalUndo(active, existing, "edit", () -> restore.accept(existing));
                                    render();
                                }
                            });
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "This goal is already in your profile.");
                    }
                });
            } else {
                candidate.refresh(client);
                configureRepeat(active, candidate, schedule, periodTarget);
                active.add(candidate, active.metadata(candidate));
                recordUndo(active, "add goal", () -> active.remove(candidate), () -> active.getGoals().contains(candidate));
                SwingUtilities.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    collapsedCategories.put(candidate.getCategory(), false);
                    clearFiltersButton.doClick();
                });
                render();
            }
        });
    }

    private void editGoal(GrindstoneProfile active, Goal goal, String title) {
        if (profile != active) {
            return;
        }
        if (goal instanceof LinkedGoal) {
            editLinkedGoal(active, (LinkedGoal) goal);
            return;
        }
        if (goal instanceof CustomGoal) {
            editCustomGoal(active, (CustomGoal) goal, title);
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) {
                return;
            }
            Map<Integer, Map<String, Supplier<Goal>>> choices = CompletionGoalChoices.create(client);
            RepeatSchedule schedule = active.metadata(goal).recurring().getSchedule();
            long periodTarget = active.metadata(goal).recurring().getTarget();
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                GoalEditorPanel form = new GoalEditorPanel(itemManager, itemSearchService, choices);
                try {
                    form.loadGoal(goal);
                } catch (IllegalArgumentException exception) {
                    form.stopSearch();
                    JOptionPane.showMessageDialog(this, exception.getMessage());
                    return;
                }
                form.loadRepeat(schedule, periodTarget);
                Goal replacement = GoalCreationDialog.showDialog(this, form, "Edit " + title, "Save changes");
                if (replacement == null) {
                    return;
                }
                RepeatSchedule selectedSchedule = form.getRepeatSchedule();
                long selectedTarget = form.getRepeatTarget();
                if (!confirmEdit(title, goal, replacement, schedule, selectedSchedule, periodTarget, selectedTarget)) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    try {
                        Consumer<Goal> restore = active.snapshotEdit(goal);
                        if (active.replaceGoal(goal, replacement, selectedSchedule)) {
                            replacement.refresh(client);
                            configureRepeat(active, replacement, selectedSchedule, selectedTarget);
                            recordGoalUndo(active, replacement, "edit", () -> restore.accept(replacement));
                            render();
                        }
                    } catch (IllegalArgumentException exception) {
                        SwingUtilities.invokeLater(() -> {
                            if (profile == active) {
                                JOptionPane.showMessageDialog(this, exception.getMessage());
                            }
                        });
                    }
                });
            });
        });
    }

    private void editLinkedGoal(GrindstoneProfile active, LinkedGoal original) {
        if (active == null || profile != active) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || original != null && !active.getGoals().contains(original)) {
                return;
            }
            List<LinkedGoalForm.Choice> sources = new ArrayList<>();
            for (Goal goal : active.getGoals()) {
                if (!(goal instanceof LinkedGoal)) {
                    GoalMetadata metadata = active.metadata(goal);
                    boolean recurring = metadata.recurring().isActive();
                    sources.add(new LinkedGoalForm.Choice(metadata.getId(), safeTitle(goal) + (recurring ? " · " + metadata.recurring().getSchedule() : ""), recurring ? metadata.recurring().getTarget() : goal.getProgressMaximum(), goal instanceof CollectionLogItemGoal, recurring || !metadata.recurring().getHistory().isEmpty()));
                }
            }
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                LinkedGoalForm form = new LinkedGoalForm(sources, original);
                JScrollPane scroll = new JScrollPane(form);
                scroll.setPreferredSize(new Dimension(520, 570));
                while (JOptionPane.showConfirmDialog(this, scroll, original == null ? "Add linked goal" : "Edit linked goal", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        LinkedGoal candidate = form.read();
                        if (JOptionPane.showConfirmDialog(this, previewPane(candidate.getTitle(null) + "\n" + candidate.getMode() + " · " + candidate.getSources().size() + " source(s)\nTargets: " + Arrays.toString(candidate.getTargets()) + "\nMissing sources remain unavailable; this never performs game actions."), "Preview linked goal", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                            continue;
                        }
                        clientThread.invokeLater(() -> {
                            if (profile != active || original != null && !active.getGoals().contains(original)) {
                                return;
                            }
                            if (original == null) {
                                active.add(candidate);
                                recordUndo(active, "add linked goal", () -> active.remove(candidate), () -> active.getGoals().contains(candidate));
                            } else {
                                Consumer<Goal> restore = active.snapshotEdit(original);
                                active.replaceGoal(original, candidate);
                                recordGoalUndo(active, candidate, "edit linked goal", () -> restore.accept(candidate));
                            }
                            active.updateLinkedGoals(hasClientProfile(active));
                            render();
                        });
                        return;
                    } catch (IllegalArgumentException | ParseException failure) {
                        JOptionPane.showMessageDialog(this, failure.getMessage(), "Check linked goal", JOptionPane.WARNING_MESSAGE);
                    }
                }
            });
        });
    }

    private boolean hasClientProfile(GrindstoneProfile active) {
        return client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == active.getHash();
    }

    private void showBulkManager() {
        GrindstoneProfile active = profile;
        if (active == null) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            long revision = active.revision();
            List<BulkGoalForm.Row> rows = new ArrayList<>();
            for (Goal goal : active.getGoals()) {
                GoalMetadata metadata = active.metadata(goal);
                rows.add(new BulkGoalForm.Row(goal, safeTitle(goal), safeCategory(goal), metadata.recurring().getSchedule()));
            }
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                BulkGoalForm form = new BulkGoalForm(rows);
                while (profile == active && JOptionPane.showConfirmDialog(this, form, "Manage goals", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        List<Goal> selected = form.selectedGoals();
                        if (selected.isEmpty()) {
                            throw new IllegalArgumentException("Tick at least one goal first.");
                        }
                        BulkGoalAction action = form.getAction();
                        GoalCategory category = form.getCategory();
                        RepeatSchedule schedule = form.getSchedule();
                        long target = action == BulkGoalAction.SCHEDULE ? form.getPeriodTarget() : 1;
                        long deadlineAt = form.getDeadlineAt();
                        boolean reminder = form.isReminderEnabled();
                        String details = action == BulkGoalAction.CATEGORY ? "\nCategory: " + category.getDisplayName() : action == BulkGoalAction.SCHEDULE ? "\nRepeat: " + schedule + " · target " + target + "\nChecklists require all their steps. Active manual progress resets when its period settings change." : action == BulkGoalAction.DEADLINE ? "\n" + (deadlineAt == 0 ? "Remove deadlines" : "Due by end of " + Instant.ofEpochMilli(deadlineAt - 1).atZone(ZoneOffset.UTC).toLocalDate() + " UTC") : "";
                        if (JOptionPane.showConfirmDialog(this, action + " · " + selected.size() + " goals?" + details + (action == BulkGoalAction.DELETE ? "\nDeleting source goals can make linked goals unavailable.\nObserved history is archived." : "") + "\nThe action is validated before changes. One-step undo is available until a later edit or relevant reset.", "Preview bulk action", JOptionPane.OK_CANCEL_OPTION, action == BulkGoalAction.DELETE ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                            continue;
                        }
                        clientThread.invokeLater(() -> {
                            if (profile != active) {
                                return;
                            }
                            try {
                                if (active.revision() != revision) {
                                    throw new IllegalArgumentException("Goals changed while this manager was open. No changes applied; reopen it to review the current goals.");
                                }
                                BulkGoalChanges.Change change = BulkGoalChanges.apply(active, selected, action, category, schedule, target, deadlineAt, reminder, System.currentTimeMillis());
                                recordUndo(active, "bulk " + action, change.undo, change.allowed);
                                render();
                            } catch (IllegalArgumentException failure) {
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, failure.getMessage(), "Bulk action not applied", JOptionPane.WARNING_MESSAGE));
                            }
                        });
                        return;
                    } catch (IllegalArgumentException | ParseException failure) {
                        JOptionPane.showMessageDialog(this, failure.getMessage(), "Review selections", JOptionPane.WARNING_MESSAGE);
                    }
                }
            });
        });
    }

    private void clearAllGoals() {
        GrindstoneProfile active = profile;
        if (active == null) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            List<Goal> selected = new ArrayList<>(active.getGoals());
            long revision = active.revision();
            if (selected.isEmpty()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "There are no goals to clear."));
                return;
            }
            SwingUtilities.invokeLater(() -> {
                if (profile != active || JOptionPane.showConfirmDialog(this, "Remove all " + selected.size() + " goals, including hidden goals?\nRecorded history is retained within the 500-goal archive limit.\nYou can undo this until another edit or a relevant period reset.", "Clear all goals", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    if (active.revision() != revision) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Goals changed while confirmation was open. Nothing was cleared; try again."));
                        return;
                    }
                    BulkGoalChanges.Change change = BulkGoalChanges.apply(active, selected, BulkGoalAction.DELETE, null, RepeatSchedule.NONE, 1, 0, false, System.currentTimeMillis());
                    recordUndo(active, "clear all goals", change.undo, change.allowed);
                    render();
                });
            });
        });
    }

    private void editDeadline(GrindstoneProfile active, Goal goal) {
        if (active == null || profile != active) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) {
                return;
            }
            GoalMetadata metadata = active.metadata(goal);
            long originalDeadline = metadata.getDeadlineAt();
            boolean originalReminder = metadata.isDeadlineReminder();
            SwingUtilities.invokeLater(() -> {
                DeadlineForm form = new DeadlineForm(originalDeadline, originalReminder);
                while (profile == active && JOptionPane.showConfirmDialog(this, form, "Personal deadline", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        long deadlineAt = form.getDeadlineAt();
                        boolean reminder = form.isReminderEnabled();
                        clientThread.invokeLater(() -> {
                            if (profile == active && active.getGoals().contains(goal) && active.metadata(goal).getDeadlineAt() == originalDeadline && active.metadata(goal).isDeadlineReminder() == originalReminder) {
                                Consumer<Goal> restore = active.snapshotEdit(goal);
                                active.metadata(goal).setDeadline(deadlineAt, reminder);
                                active.touch();
                                recordGoalUndo(active, goal, "deadline", () -> restore.accept(goal));
                                render();
                            }
                        });
                        return;
                    } catch (IllegalArgumentException failure) {
                        JOptionPane.showMessageDialog(this, failure.getMessage());
                    }
                }
            });
        });
    }

    private void editCustomGoal(GrindstoneProfile active, CustomGoal goal, String title) {
        RepeatSchedule openedSchedule = active.metadata(goal).recurring().getSchedule();
        long openedPeriod = openedSchedule.start(System.currentTimeMillis());
        CustomGoalForm form = new CustomGoalForm();
        form.load(goal);
        RepeatGoalForm repeat = new RepeatGoalForm();
        repeat.load(active.metadata(goal).recurring().getSchedule(), active.metadata(goal).recurring().getTarget());
        form.setRecurring(repeat.getSchedule() != RepeatSchedule.NONE);
        repeat.addScheduleListener(() -> form.setRecurring(repeat.getSchedule() != RepeatSchedule.NONE));
        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.add(form, BorderLayout.CENTER);
        editor.add(repeat, BorderLayout.SOUTH);
        while (JOptionPane.showConfirmDialog(this, editor, "Edit " + title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                repeat.commit();
                RepeatSchedule schedule = repeat.getSchedule();
                long periodTarget = repeat.getTarget();
                CustomGoal draft = form.read(schedule == RepeatSchedule.NONE ? 0 : periodTarget);
                if (!confirmEdit(title, goal, draft, active.metadata(goal).recurring().getSchedule(), schedule, active.metadata(goal).recurring().getTarget(), periodTarget)) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile == active && active.getGoals().contains(goal)) {
                        if (openedSchedule != RepeatSchedule.NONE && openedSchedule.start(System.currentTimeMillis()) != openedPeriod) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "The period reset while you were editing. Reopen the editor to avoid applying the previous period's progress."));
                            render();
                            return;
                        }
                        Goal collision = active.findMatchingGoal(goal, schedule);
                        if (collision != null && collision != goal) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "That personal goal already has this schedule."));
                            return;
                        }
                        Consumer<Goal> restore = active.snapshotEdit(goal);
                        goal.update(draft.getTitle(null), draft.getDescription(), draft.getCategory(), draft.getProgressCurrent(), draft.getProgressMaximum());
                        configureRepeat(active, goal, schedule, periodTarget);
                        active.touch();
                        recordGoalUndo(active, goal, "edit", () -> restore.accept(goal));
                        render();
                    }
                });
                return;
            } catch (ParseException | IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(this, exception instanceof IllegalArgumentException ? exception.getMessage() : "Enter valid progress and target numbers.", "Check goal details", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean confirmEdit(String title, Goal original, Goal replacement, RepeatSchedule before, RepeatSchedule after, long previousTarget, long nextTarget) {
        String summary = title + " → " + replacement.getTitle(null) + "\nTarget: " + String.format(Locale.ROOT, "%,d → %,d", original.getProgressMaximum(), replacement.getProgressMaximum()) + "\nRepeat: " + before + " → " + after + "\nPeriod target: " + String.format(Locale.ROOT, "%,d → %,d", previousTarget, nextTarget) + "\n\nChanged subjects retain old observations in account history.\nChanged repeat settings archive the current observed period.\nUndo expires after a later edit or period reset.";
        JTextArea preview = new JTextArea(summary, 10, 38);
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setFont(UiTypography.BODY);
        return JOptionPane.showConfirmDialog(this, new JScrollPane(preview), "Preview goal changes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void deleteGoal(GrindstoneProfile active, Goal goal, String title) {
        if (profile != active || JOptionPane.showConfirmDialog(this, "Remove " + title + " from your goals?\nYour game progress is unaffected.", "Delete goal", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
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
            if (!undo.undo(profile)) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Undo expired after a later edit, profile change or period reset."));
            }
            render();
        });
    }

    private void recordUndo(GrindstoneProfile active, String label, Runnable action, BooleanSupplier allowed) {
        undo.record(active, label, action, allowed);
        SwingUtilities.invokeLater(() -> {
            if (profile == active) {
                undoDeleteButton.setText("Undo " + label);
                undoDeleteButton.setToolTipText("Undo the last change. Expires after a later edit or period reset.");
                undoDeleteButton.setVisible(true);
            }
        });
    }

    private void recordGoalUndo(GrindstoneProfile active, Goal goal, String label, Runnable action) {
        RepeatSchedule schedule = active.metadata(goal).recurring().getSchedule();
        long period = schedule.start(System.currentTimeMillis());
        recordUndo(active, label, action, () -> active.getGoals().contains(goal) && (schedule == RepeatSchedule.NONE || schedule.start(System.currentTimeMillis()) == period));
    }

    private void promptManualProgress(GrindstoneProfile active, CustomGoal goal, long current, long target) {
        RepeatSchedule schedule = active.metadata(goal).recurring().getSchedule();
        long window = schedule.start(System.currentTimeMillis());
        JSpinner value = new JSpinner(new SpinnerNumberModel(current, 0L, target, 1L));
        value.setEditor(new JSpinner.NumberEditor(value, "#,##0"));
        while (JOptionPane.showConfirmDialog(this, value, "Set progress (0–" + String.format(Locale.ROOT, "%,d", target) + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                value.commitEdit();
                long requested = ((Number) value.getValue()).longValue();
                changeManualProgress(active, goal, previous -> {
                    if (schedule != RepeatSchedule.NONE && schedule.start(System.currentTimeMillis()) != window) {
                        throw new IllegalStateException("The period reset while this dialog was open. Reopen Set progress for the new period.");
                    }
                    return requested;
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(this, "Enter a whole number within the target.");
            }
        }
    }

    private void changeManualProgress(GrindstoneProfile active, CustomGoal goal, LongUnaryOperator change) {
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) {
                return;
            }
            long now = System.currentTimeMillis();
            RecurringGoal recurring = active.metadata(goal).recurring();
            if (recurring.isActive()) {
                recurring.observe(goal, now);
            }
            long requested;
            try {
                requested = change.applyAsLong(goal.getProgressCurrent());
            } catch (IllegalStateException exception) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, exception.getMessage()));
                render();
                return;
            }
            Consumer<Goal> restore = active.snapshotEdit(goal);
            goal.update(goal.getTitle(null), goal.getDescription(), goal.getCategory(), Math.min(requested, goal.getProgressMaximum()), goal.getProgressMaximum());
            if (recurring.isActive()) {
                recurring.observe(goal, now);
            } else {
                active.metadata(goal).observe(goal, goal.getTitle(null), now);
            }
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
        if (profile != null && profile.metadata(goal).getDisplayCategory() != null) {
            return profile.metadata(goal).getDisplayCategory();
        }
        GoalCategory category = goal.getCategory();
        return category == null ? GoalCategory.OTHER : category;
    }

    private static boolean isComplete(GoalStatus status) {
        return status != null && ("COMPLETE".equalsIgnoreCase(status.name()) || "COMPLETED".equalsIgnoreCase(status.name()));
    }

    private boolean isCollectionLogGoal(Goal goal) {
        return goal instanceof CollectionLogEntryGoal || goal instanceof CollectionLogItemGoal;
    }

    private boolean isLevelGoal(Goal goal) {
        return goal instanceof SkillLevelGoal || goal instanceof CombatLevelGoal || goal instanceof TotalLevelGoal || goal instanceof BaseLevelGoal;
    }

    private boolean isExperienceGoal(Goal goal) {
        return goal instanceof SkillExperienceGoal || goal instanceof TotalExperienceGoal || goal instanceof BaseExperienceGoal || goal instanceof SessionExperienceGoal;
    }


    private void refreshPanel() {
        main.revalidate();
        main.repaint();
        scrollPane.getViewport().revalidate();
        scrollPane.revalidate();
        scrollPane.repaint();
        revalidate();
        repaint();
        revealPrepared(preparedGoals);
    }
    public void refreshGoal(Goal goal) {
        if (goal == null) {
            return;
        }

        cardCache.invalidate(goal);
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

        private GoalView(Goal goal, GoalComponent component, String title, GoalCategory category, GoalStatus status, boolean hasProgress, long progressCurrent, long progressMaximum) {
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
