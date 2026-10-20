package com.grindstone.ui;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.storage.PlayerStorageSource;
import com.grindstone.content.milestone.BaseSkillMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestonePreset;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.ManualMilestone;
import com.grindstone.content.milestone.TrackingState;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogItemMilestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.BaseLevelMilestone;
import com.grindstone.content.milestone.impl.level.CombatLevelMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.milestone.impl.level.TotalLevelMilestone;
import com.grindstone.notification.MilestoneCompletionNotifications;
import com.grindstone.overlay.GrindstoneProgressOverlay;
import com.grindstone.overlay.ProgressOverlayEntry;
import com.grindstone.repository.BulkMilestoneAction;
import com.grindstone.repository.BulkMilestoneChanges;
import com.grindstone.repository.MilestoneMetadata;
import com.grindstone.repository.MilestoneUndo;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.HistoryRecord;
import com.grindstone.repository.RecurringMilestone;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.components.ContentHeightPanel;
import com.grindstone.ui.components.MilestoneComponent;
import com.grindstone.ui.components.MilestoneHistoryPanel;
import com.grindstone.ui.components.MilestoneListPanel;
import com.grindstone.ui.components.HistoryBrowserPanel;
import com.grindstone.ui.components.SegmentedProgressBar;
import com.grindstone.ui.components.SnapshotStatusPanel;
import com.grindstone.ui.editor.BulkMilestoneForm;
import com.grindstone.ui.editor.CompletionMilestoneChoices;
import com.grindstone.ui.editor.CustomMilestoneForm;
import com.grindstone.ui.editor.DeadlineForm;
import com.grindstone.ui.editor.MilestoneCreationDialog;
import com.grindstone.ui.editor.MilestoneEditorPanel;
import com.grindstone.ui.editor.LinkedMilestoneForm;
import com.grindstone.ui.editor.RepeatMilestoneForm;
import com.grindstone.ui.editor.RoutinePresetForm;
import com.grindstone.ui.item.ItemSearchService;
import com.grindstone.ui.util.DialogPlacement;
import com.grindstone.ui.util.MilestoneCardCache;
import com.grindstone.ui.util.SkillMilestoneGrouping;
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
import java.time.ZoneId;
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
import net.runelite.api.gameval.VarPlayerID;
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
    private GrindstoneProgressOverlay progressOverlay;
    @Inject
    private MilestoneCompletionNotifications completionNotifications;
    @Inject
    private ProfileBackupActions backupActions;
    private static final String ALL_CATEGORIES = "All";
    private static final String ALL_STATUSES = "All statuses";
    private static final String NOT_STARTED = "New";
    private static final String IN_PROGRESS = "Active";
    private static final String COMPLETE = "Done";
    private static final String UNFINISHED = "Unfinished";
    private static final String WAITING = "Waiting";
    private static final String ALL_SCHEDULES = "All";
    private static final String ONE_OFF = "One-off milestones";
    private static final String ALL_TYPES = "All";
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

    private final JPanel main = new MilestoneListPanel();
    private final JPanel header = new JPanel();
    private final JPanel controlsPanel = new JPanel();
    private final JPanel dashboardOverview = new JPanel();
    private final JPanel dashboardSnapshots = new JPanel(new BorderLayout());
    private final SnapshotStatusPanel snapshots = new SnapshotStatusPanel();
    private final JLabel summaryLabel = new JLabel("No profile loaded");
    private final JLabel progressPercent = new JLabel();
    private final JLabel filterSummary = new JLabel();
    private final JLabel updateStatusLabel = new JLabel();
    private final JButton catchUpButton = new JButton();
    private String catchUpText = "";
    private final JLabel resultsLabel = new JLabel();
    private final SegmentedProgressBar overallProgress = new SegmentedProgressBar();
    private final IconTextField searchField = new IconTextField();
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<String> sortFilter = new JComboBox<>();
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final JComboBox<String> scheduleFilter = new JComboBox<>();
    private final JCheckBox pinnedOnly = new JCheckBox("Pinned only");
    private final JToggleButton allStatusesButton = new JToggleButton("All 0");
    private final JToggleButton unfinishedStatusesButton = new JToggleButton("Unfinished 0");
    private final JToggleButton newGoalsButton = new JToggleButton("New 0");
    private final JToggleButton activeGoalsButton = new JToggleButton("Active 0");
    private final JToggleButton completeGoalsButton = new JToggleButton("Done 0");
    private final JToggleButton waitingGoalsButton = new JToggleButton("Waiting 0");
    private final JButton filtersToggle = new JButton("Filters");
    private final JButton clearFiltersButton = new JButton("Clear");
    private final JButton undoDeleteButton = new JButton("Undo delete");
    private final JToggleButton todayButton = new JToggleButton("Routines");
    private final JToggleButton allGoalsButton = new JToggleButton("All", true);
    private boolean todayMode;
    private String pendingReveal;
    private List<MilestoneView> preparedGoals = new ArrayList<>();
    private final MilestoneCardCache<MilestoneView> cardCache = new MilestoneCardCache<>();
    private List<?> layoutKey;
    private final Map<Milestone, MilestoneComponent> displayedCards = new IdentityHashMap<>();
    private final Map<RepeatSchedule, JButton> repeatHeadings = new LinkedHashMap<>();
    private JPanel routineOverview;
    private JLabel routineTitle;
    private JLabel routineDue;
    private SegmentedProgressBar routineProgress;
    private final Component routineGap = Box.createVerticalStrut(12);
    private final MilestoneUndo undo = new MilestoneUndo();
    private final JScrollPane scrollPane;

    private final Map<MilestoneCategory, Boolean> collapsedCategories = new LinkedHashMap<>();
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

        JLabel titleLabel = new JLabel("Grindstone");
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
        more.setToolTipText("More milestone options");
        more.putClientProperty("JButton.buttonType", "toolBarButton");
        JPopupMenu moreMenu = new JPopupMenu();
        more.addActionListener(event -> moreMenu.show(more, 0, more.getHeight()));
        JButton dashboardToggle = new JButton("-");
        styleButton(dashboardToggle);
        dashboardToggle.setToolTipText("Collapse dashboard overview and snapshots");
        dashboardToggle.getAccessibleContext().setAccessibleName("Collapse dashboard");
        dashboardToggle.addActionListener(event -> {
            boolean expanded = !dashboardOverview.isVisible();
            dashboardOverview.setVisible(expanded);
            dashboardSnapshots.setVisible(expanded);
            dashboardToggle.setText(expanded ? "-" : "+");
            dashboardToggle.setToolTipText(expanded ? "Collapse dashboard overview and snapshots" : "Expand dashboard overview and snapshots");
            dashboardToggle.getAccessibleContext().setAccessibleName(expanded ? "Collapse dashboard" : "Expand dashboard");
            revalidate();
            repaint();
        });
        JPanel titleActions = new JPanel(new GridLayout(1, 2, 4, 0));
        titleActions.setOpaque(false);
        titleActions.add(dashboardToggle);
        titleActions.add(more);
        titleRow.add(titleActions, BorderLayout.EAST);
        header.add(titleRow);
        dashboardOverview.setOpaque(false);
        dashboardOverview.setLayout(new BoxLayout(dashboardOverview, BoxLayout.Y_AXIS));
        dashboardOverview.setAlignmentX(LEFT_ALIGNMENT);
        dashboardOverview.add(Box.createVerticalStrut(6));
        JPanel summaryRow = new JPanel(new BorderLayout(6, 0));
        summaryRow.setOpaque(false);
        summaryRow.setAlignmentX(LEFT_ALIGNMENT);
        summaryLabel.setFont(UiTypography.BODY);
        progressPercent.setFont(UiTypography.TITLE);
        progressPercent.setForeground(TEXT_COLOR);
        progressPercent.setToolTipText("Percentage of tracked milestones and routines that are complete.");
        summaryRow.add(summaryLabel, BorderLayout.CENTER);
        summaryRow.add(progressPercent, BorderLayout.EAST);
        dashboardOverview.add(summaryRow);
        dashboardOverview.add(Box.createVerticalStrut(8));
        dashboardOverview.add(overallProgress);
        updateStatusLabel.setFont(UiTypography.META);
        updateStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
        updateStatusLabel.setToolTipText("Storage and save messages. See Snapshots for captured game data.");
        dashboardOverview.add(Box.createVerticalStrut(4));
        dashboardOverview.add(updateStatusLabel);
        catchUpButton.setAlignmentX(LEFT_ALIGNMENT);
        catchUpButton.setFont(UiTypography.META);
        catchUpButton.setVisible(false);
        catchUpButton.setToolTipText("Review progress discovered since your previous Grindstone reading, including play on mobile or another computer.");
        catchUpButton.addActionListener(event -> {
            JTextArea details = new JTextArea(catchUpText, 12, 36);
            details.setEditable(false);
            details.setLineWrap(true);
            details.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), new JScrollPane(details), "Since last seen", JOptionPane.INFORMATION_MESSAGE);
        });
        dashboardOverview.add(catchUpButton);


        header.add(dashboardOverview);

        JPanel goalActions = new JPanel(new GridLayout(2, 1, 0, 6));
        goalActions.setOpaque(false);
        goalActions.setAlignmentX(LEFT_ALIGNMENT);
        goalActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        goalActions.add(createMilestoneActionButton("Add milestone", "Create an automatic tracker or a custom milestone with manual progress.", () -> showAddGoalDialog()));
        goalActions.add(createMilestoneActionButton("Link milestones", "Combine existing trackers into a linked milestone.", () -> editLinkedGoal(profile, null)));
        JMenuItem allCollectionPages = new JMenuItem("Add all collection-log pages...");
        allCollectionPages.setToolTipText("Read every page from the client's five collection-log tabs and add missing page milestones.");
        allCollectionPages.addActionListener(event -> addAllCollectionPages());

        JMenu presets = new JMenu("Add presets");
        presets.setToolTipText("Add a focused milestone pack without replacing existing milestones or edited targets.");
        for (MilestonePreset preset : MilestonePreset.values()) {
            JMenuItem option = new JMenuItem(preset.toString());
            option.addActionListener(event -> addPreset(preset));
            presets.add(option);
        }

        JMenu routines = new JMenu("Routines");
        JMenuItem dailyRoutine = new JMenuItem("Daily skilling & personal");
        dailyRoutine.addActionListener(event -> addRoutine(false));
        JMenuItem weeklyRoutine = new JMenuItem("Weekly activities & personal");
        weeklyRoutine.addActionListener(event -> addRoutine(true));
        routines.add(dailyRoutine);
        routines.add(weeklyRoutine);
        presets.addSeparator();
        presets.add(routines);
        presets.add(allCollectionPages);
        moreMenu.add(presets);
        JMenu milestoneActions = new JMenu("Manage milestones");
        JMenuItem manage = new JMenuItem("Select and edit...");
        manage.setToolTipText("Search and select milestones to pin, delete, categorize, change schedules or set deadlines.");
        manage.addActionListener(event -> showBulkManager());
        milestoneActions.add(manage);
        JMenuItem clearGoals = new JMenuItem("Clear all milestones...");
        clearGoals.setToolTipText("Remove all milestones from this profile, including hidden milestones. Confirmation and undo are available.");
        clearGoals.addActionListener(event -> clearAllGoals());
        milestoneActions.addSeparator();
        milestoneActions.add(clearGoals);
        moreMenu.add(milestoneActions);
        JMenu backups = new JMenu("Profile backups");
        JMenuItem export = new JMenuItem("Export profile backup...");
        export.setToolTipText("Save a private local JSON backup. No network is used.");
        export.addActionListener(event -> transferBackup(false));
        backups.add(export);
        JMenuItem restore = new JMenuItem("Restore / import backup...");
        restore.setToolTipText("Merge missing milestones without replacing existing targets.");
        restore.addActionListener(event -> transferBackup(true));
        backups.add(restore);
        JMenu viewOptions = new JMenu("Category display");
        JMenuItem expandAll = new JMenuItem("Expand all categories");
        expandAll.setToolTipText("Show the milestone cards in every category.");
        expandAll.addActionListener(event -> setAllCategoriesCollapsed(false));
        viewOptions.add(expandAll);
        JMenuItem collapseAll = new JMenuItem("Collapse all categories");
        collapseAll.setToolTipText("Show category headers only. Your milestones and progress are kept.");
        collapseAll.addActionListener(event -> setAllCategoriesCollapsed(true));
        viewOptions.add(collapseAll);
        moreMenu.add(viewOptions);
        moreMenu.add(backups);
        moreMenu.addSeparator();
        JMenuItem history = new JMenuItem("History...");
        history.setToolTipText("Browse this account's milestone history by date, schedule, milestone and outcome.");
        history.addActionListener(event -> showAccountHistory());
        moreMenu.add(history);
        header.add(Box.createVerticalStrut(8));
        header.add(goalActions);
        JPanel navigation = new JPanel(new GridLayout(1, 2, 4, 0));
        navigation.setOpaque(false);
        navigation.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup views = new ButtonGroup();
        views.add(todayButton);
        views.add(allGoalsButton);
        todayButton.setToolTipText("Focus on routines, ordered by next reset, with a period progress overview.");
        allGoalsButton.setToolTipText("Show milestones and routines with your usual filters.");
        todayButton.addActionListener(event -> {
            todayMode = true;
            render();
        });
        allGoalsButton.addActionListener(event -> {
            todayMode = false;
            render();
        });
        for (AbstractButton button : new AbstractButton[]{todayButton, allGoalsButton}) {
            button.setFont(UiTypography.META);
            button.setMargin(new Insets(5, 3, 5, 3));
            navigation.add(button);
        }
        header.add(Box.createVerticalStrut(6));
        header.add(navigation);
        dashboardSnapshots.setOpaque(false);
        dashboardSnapshots.setAlignmentX(LEFT_ALIGNMENT);
        dashboardSnapshots.add(snapshots, BorderLayout.CENTER);
        header.add(dashboardSnapshots);
        undoDeleteButton.setVisible(false);
        undoDeleteButton.setToolTipText("Restore the most recently deleted milestone in this player profile.");
        undoDeleteButton.setAlignmentX(LEFT_ALIGNMENT);
        styleButton(undoDeleteButton);
        undoDeleteButton.addActionListener(event -> undoDelete());
        header.add(Box.createVerticalStrut(4));
        header.add(undoDeleteButton);
    }

    private JButton createMilestoneActionButton(String title, String tooltip, Runnable action) {
        JButton button = new JButton("+  " + title);
        styleButton(button);
        button.setFont(UiTypography.TITLE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMargin(new Insets(8, 12, 8, 12));
        button.setPreferredSize(new Dimension(200, 32));
        button.setToolTipText(tooltip);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JPanel createStatusCounters() {
        JPanel counters = new JPanel(new GridLayout(3, 2, 4, 4));
        counters.setOpaque(false);
        counters.setAlignmentX(LEFT_ALIGNMENT);
        newGoalsButton.setForeground(SegmentedProgressBar.getColorFor(MilestoneStatus.NOT_STARTED));
        activeGoalsButton.setForeground(SegmentedProgressBar.getColorFor(MilestoneStatus.IN_PROGRESS));
        completeGoalsButton.setForeground(SegmentedProgressBar.getColorFor(MilestoneStatus.COMPLETE));
        newGoalsButton.setToolTipText("Show not-started milestones. Click again to show all statuses.");
        activeGoalsButton.setToolTipText("Show in-progress milestones. Click again to show all statuses.");
        completeGoalsButton.setToolTipText("Show completed milestones, including hidden completions. Click again to show all statuses.");
        waitingGoalsButton.setToolTipText("Show milestones needing a login or native client reading. Click again to show all statuses.");
        for (JToggleButton counter : new JToggleButton[]{allStatusesButton, unfinishedStatusesButton, newGoalsButton, activeGoalsButton, completeGoalsButton, waitingGoalsButton}) {
            counter.setFont(UiTypography.BODY);
            counter.setMargin(new Insets(3, 3, 3, 3));
            counter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        allStatusesButton.setToolTipText("Show all milestone statuses.");
        unfinishedStatusesButton.setToolTipText("Show every milestone that is not done, including those waiting for data.");
        allStatusesButton.addActionListener(event -> statusFilter.setSelectedItem(ALL_STATUSES));
        unfinishedStatusesButton.addActionListener(event -> toggleQuickStatus(UNFINISHED));
        newGoalsButton.addActionListener(event -> toggleQuickStatus(NOT_STARTED));
        activeGoalsButton.addActionListener(event -> toggleQuickStatus(IN_PROGRESS));
        completeGoalsButton.addActionListener(event -> toggleQuickStatus(COMPLETE));
        waitingGoalsButton.addActionListener(event -> toggleQuickStatus(WAITING));
        counters.add(allStatusesButton);
        counters.add(unfinishedStatusesButton);
        counters.add(newGoalsButton);
        counters.add(activeGoalsButton);
        counters.add(waitingGoalsButton);
        counters.add(completeGoalsButton);
        return counters;
    }

    private JPanel createFilterField(String title, JComboBox<String> control) {
        JPanel field = new JPanel(new BorderLayout(0, 3));
        field.setOpaque(false);
        field.add(createControlLabel(title), BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(95, 28));
        control.setMinimumSize(new Dimension(0, 28));
        field.add(control, BorderLayout.CENTER);
        return field;
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
        statusFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_STATUSES, UNFINISHED, NOT_STARTED, IN_PROGRESS, WAITING, COMPLETE}));
        scheduleFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_SCHEDULES, ONE_OFF, "Daily", "Weekly", "Monthly"}));
        styleComboBox(scheduleFilter);
        scheduleFilter.setAlignmentX(LEFT_ALIGNMENT);
        scheduleFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        UiTooltips.set(scheduleFilter, "Show one-off milestones or milestones repeating daily, weekly or monthly.");
        pinnedOnly.setOpaque(false);
        pinnedOnly.setFont(UiTypography.BODY);
        pinnedOnly.setAlignmentX(LEFT_ALIGNMENT);
        pinnedOnly.setToolTipText("Show only milestones you pinned, without changing your pins or other filters.");
        sortFilter.setModel(new DefaultComboBoxModel<>(new String[]{SORT_NAME, SORT_NAME_DESC, SORT_CLOSEST, SORT_PROGRESS_DESC, SORT_PROGRESS_ASC, SORT_STATUS}));
        typeFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_TYPES, AUTOMATIC, CUSTOM, LINKED, BASE, SESSION, DEADLINES, PAST_DUE}));
        styleComboBox(typeFilter);
        typeFilter.setAlignmentX(LEFT_ALIGNMENT);
        typeFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        styleComboBox(categoryFilter);
        styleComboBox(statusFilter);
        styleComboBox(sortFilter);
        UiTooltips.set(categoryFilter, "Show milestones from one category, or choose All categories.");
        UiTooltips.set(statusFilter, "Filter by progress state. Done also shows finished one-time milestones and hidden recurring completions.");
        UiTooltips.set(sortFilter, "Sort within each category. Closest unfinished puts incomplete milestones nearest their target first.");
        UiTooltips.set(typeFilter, "Custom includes manual checklists. Linked milestones combine existing trackers. Filter base stats, session gains or personal deadlines without deleting anything.");

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
        filterOptions.add(createControlLabel("Status"));
        filterOptions.add(Box.createVerticalStrut(4));
        filterOptions.add(createStatusCounters());
        filterOptions.add(Box.createVerticalStrut(10));
        JPanel selectors = new JPanel(new GridLayout(2, 2, 6, 8));
        selectors.setOpaque(false);
        selectors.setAlignmentX(LEFT_ALIGNMENT);
        selectors.add(createFilterField("Category", categoryFilter));
        selectors.add(createFilterField("Type", typeFilter));
        selectors.add(createFilterField("Schedule", scheduleFilter));
        selectors.add(createFilterField("Sort", sortFilter));
        filterOptions.add(selectors);
        filterOptions.add(Box.createVerticalStrut(8));
        JPanel filterActions = new JPanel(new BorderLayout(6, 0));
        filterActions.setOpaque(false);
        filterActions.setAlignmentX(LEFT_ALIGNMENT);
        filterActions.add(pinnedOnly, BorderLayout.CENTER);
        styleButton(clearFiltersButton);
        clearFiltersButton.setText("Reset");
        clearFiltersButton.setToolTipText("Reset search, filters and sorting.");
        filterActions.add(clearFiltersButton, BorderLayout.EAST);
        filterOptions.add(filterActions);
        JScrollPane filterScroll = new JScrollPane(filterOptions);
        filterScroll.setBorder(BorderFactory.createEmptyBorder());
        filterScroll.setOpaque(false);
        filterScroll.getViewport().setOpaque(false);
        filterScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        filterScroll.getVerticalScrollBar().setUnitIncrement(16);
        filterScroll.setAlignmentX(LEFT_ALIGNMENT);
        filterScroll.setPreferredSize(new Dimension(200, 280));
        filterScroll.setMinimumSize(new Dimension(0, 100));
        filterScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
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
        allStatusesButton.setSelected(ALL_STATUSES.equals(status));
        unfinishedStatusesButton.setSelected(UNFINISHED.equals(status));
        newGoalsButton.setSelected(NOT_STARTED.equals(status));
        activeGoalsButton.setSelected(IN_PROGRESS.equals(status));
        completeGoalsButton.setSelected(COMPLETE.equals(status));
        waitingGoalsButton.setSelected(WAITING.equals(status));
    }

    private void setAllCategoriesCollapsed(boolean collapsed) {
        for (MilestoneCategory category : MilestoneCategory.values()) {
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
        if (progressOverlay != null) {
            progressOverlay.clear();
        }
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
            List<Supplier<MilestoneView>> preparedViews = new ArrayList<>();
            List<ProgressOverlayEntry> overlayEntries = new ArrayList<>();
            boolean loggedIn = currentProfile != null && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && currentProfile.getHash() == client.getAccountHash();
            boolean captureOverlay = loggedIn && config.progressOverlayEnabled();

            if (currentProfile != null && config.enabled()) {
                cardCache.retain(currentProfile.getGoals());
                if (loggedIn) {
                    currentProfile.observeRecurring(client, System.currentTimeMillis());
                    completionNotifications.observe(currentProfile);
                }
                for (Milestone goal : currentProfile.getGoals()) {
                    if (!config.linkedGoalsEnabled() && goal instanceof LinkedMilestone) {
                        continue;
                    }
                    if (goal == null || (!config.collectionLogEnabled() && isCollectionLogGoal(goal)) || (!config.levelGoalsEnabled() && isLevelGoal(goal)) || (!config.experienceGoalsEnabled() && isExperienceGoal(goal)) || (!config.trackerGoalsEnabled() && goal instanceof TrackerMilestone) || (!config.questGoalsEnabled() && goal.getCategory() == MilestoneCategory.QUEST) || (!config.diaryGoalsEnabled() && goal.getCategory() == MilestoneCategory.ACHIEVEMENT_DIARY) || (!config.combatAchievementGoalsEnabled() && goal.getCategory() == MilestoneCategory.COMBAT_ACHIEVEMENT) || (!config.ownedItemGoalsEnabled() && goal instanceof PossessedItemMilestone) || (!config.customGoalsEnabled() && goal instanceof ManualMilestone)) {
                        continue;
                    }

                    String title = getSafeTitle(goal);
                    MilestoneMetadata metadata = currentProfile.getMetadata(goal);
                    RecurringMilestone recurring = metadata.getRecurring();
                    boolean repeating = recurring.isActive() && RecurringMilestone.supports(goal);
                    TrackingState tracking = TrackingState.of(goal, recurring, client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == currentProfile.getHash(), currentProfile.getPlayerStorage().hasBankSnapshot());
                    List<?> key = Arrays.asList(currentProfile, metadata, title, goal.getDescription(), goal.getProgressCurrent(), goal.getProgressMaximum(), goal.getStatus(), recurring.getSchedule(), recurring.getProgress(), recurring.getTarget(), recurring.getBaselineAt(), recurring.getStart(), metadata.isPinned(), metadata.isCollapsed(config.collapseDetailsByDefault()), metadata.getCompletions().size(), tracking, config.showGoalIcons(), config.showProgressBar(), config.maxProgressChunks(), config.showCompletionHistory(), config.showActivityIcons(), config.itemsPerRow(), config.maxItemRows(), config.showTrackingState(), config.showObtainedItems(), config.showItemTooltips());
                    if (goal instanceof ChecklistMilestone) {
                        key = Arrays.asList(key, Arrays.hashCode(((ChecklistMilestone) goal).getChecks()));
                    }
                    key = Arrays.asList(key, metadata.getDeadlineAt(), metadata.getDeadlineAt() > 0 && System.currentTimeMillis() >= metadata.getDeadlineAt(), metadata.getDisplayCategory());
                    MilestoneView cached = cardCache.get(goal, key);
                    if (cached != null && tracking != TrackingState.WAITING && goal.getProgressMaximum() > 0) {
                        if (captureOverlay) {
                            overlayEntries.add(new ProgressOverlayEntry(title, repeating ? recurring.getSchedule() : RepeatSchedule.NONE, repeating ? recurring.getProgress() : goal.getProgressCurrent(), repeating ? recurring.getTarget() : goal.getProgressMaximum(), isComplete(repeating ? recurring.getStatus() : goal.getStatus()), true, metadata.isPinned()));
                        }
                        preparedViews.add(() -> cached);
                        continue;
                    }
                    Supplier<MilestoneComponent> preparedCard = goal.prepare(client, config);
                    tracking = TrackingState.of(goal, recurring, client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == currentProfile.getHash(), currentProfile.getPlayerStorage().hasBankSnapshot());
                    TrackingState displayTracking = tracking;
                    MilestoneStatus status = repeating ? recurring.getStatus() : goal.getStatus();
                    long progressCurrent = repeating ? recurring.getProgress() : goal.getProgressCurrent();
                    long progressMaximum = repeating ? recurring.getTarget() : goal.getProgressMaximum();
                    if (tracking == TrackingState.WAITING) {
                        status = MilestoneStatus.NOT_STARTED;
                        progressCurrent = 0;
                        progressMaximum = 0;
                    }
                    if (loggedIn && tracking != TrackingState.WAITING) {
                        metadata.observeReading(goal, title, System.currentTimeMillis());
                    }
                    if (!repeating && tracking != TrackingState.WAITING) {
                        metadata.observe(goal, title, System.currentTimeMillis());
                    }
                    MilestoneMetadata displayMetadata = metadata.copy();
                    RecurringMilestone displayRecurring = displayMetadata.getRecurring();
                    MilestoneCategory category = getSafeCategory(goal);
                    boolean hasProgress = goal.hasProgress();
                    String sourceDetail = goal.getCardDescription().isEmpty() ? goal.getDescription() : "";
                    long shownCurrent = progressCurrent;
                    long shownMaximum = progressMaximum;
                    MilestoneStatus shownStatus = status;
                    if (captureOverlay) {
                        overlayEntries.add(new ProgressOverlayEntry(title, repeating ? recurring.getSchedule() : RepeatSchedule.NONE, shownCurrent, shownMaximum, isComplete(shownStatus), tracking != TrackingState.WAITING, metadata.isPinned()));
                    }
                    List<String> steps = goal instanceof ChecklistMilestone ? new ArrayList<>(((ChecklistMilestone) goal).getSteps()) : new ArrayList<>();
                    boolean[] checks = goal instanceof ChecklistMilestone ? ((ChecklistMilestone) goal).getChecks().clone() : new boolean[0];
                    List<?> cacheKey = key;
                    preparedViews.add(() -> {
                        MilestoneComponent component = preparedCard.get();
                        if (goal instanceof ChecklistMilestone) {
                            ChecklistMilestone checklist = (ChecklistMilestone) goal;
                            boolean[] checked = checks;
                            RepeatSchedule openedSchedule = displayRecurring.getSchedule();
                            long openedPeriod = openedSchedule.getPeriodStart(System.currentTimeMillis());
                            for (int step = 0; step < checked.length; step++) {
                                int index = step;
                                JCheckBox check = new JCheckBox("", checked[step]);
                                check.getAccessibleContext().setAccessibleName(steps.get(step));
                                check.setOpaque(false);
                                check.setFont(UiTypography.BODY);
                                check.setToolTipText("Manually mark this personal step complete. No game action is performed; repeating checklists reset at their UTC boundary.");
                                check.setAlignmentX(LEFT_ALIGNMENT);
                                check.addActionListener(event -> {
                                    boolean selected = check.isSelected();
                                    clientThread.invokeLater(() -> {
                                        if (profile == currentProfile && currentProfile.getGoals().contains(goal) && currentProfile.getMetadata(goal).getRecurring().getSchedule() == openedSchedule && openedSchedule.getPeriodStart(System.currentTimeMillis()) == openedPeriod) {
                                            Consumer<Milestone> restore = currentProfile.snapshotEdit(goal);
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
                                JTextArea stepText = new JTextArea(steps.get(step), 0, 14);
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
                        component.setGoalStatus(shownStatus);
                        component.putClientProperty("grindstone.repeat", repeating ? displayRecurring.getSchedule() : RepeatSchedule.NONE);
                        component.putClientProperty("grindstone.id", displayMetadata.getId());
                        component.putClientProperty("grindstone.ready", displayTracking != TrackingState.WAITING);
                        if (repeating) {
                            component.setProgress(shownCurrent, shownMaximum, String.format(Locale.ROOT, "%,d / %,d this period", shownCurrent, shownMaximum));
                        }
                        if (displayTracking == TrackingState.WAITING) {
                            component.setProgress(0, 0, "Waiting for client data");
                        } else if (displayTracking == TrackingState.OBSERVED_LOG) {
                            component.setProgress(shownCurrent, shownMaximum, String.format(Locale.ROOT, "%,d / %,d", shownCurrent, shownMaximum));
                        } else if (displayTracking == TrackingState.PARTIAL_STORAGE || displayTracking == TrackingState.SNAPSHOT) {
                            component.setProgress(shownCurrent, shownMaximum, String.format(Locale.ROOT, "%,d / %,d", shownCurrent, shownMaximum));
                        }
                        String baselineDetail = displayRecurring.getBaselineAt() == 0 ? "" : " Baseline: " + Instant.ofEpochMilli(displayRecurring.getBaselineAt()) + ".";
                        long observedAt = repeating ? displayRecurring.getLastObservedAt() : displayMetadata.getLastObservedAt();
                        component.setReliability(config.showTrackingState() ? displayTracking.getCardLabel() : null, displayTracking.getDetail() + baselineDetail + (observedAt == 0 ? "" : " Last reading: " + Instant.ofEpochMilli(observedAt) + ".") + (sourceDetail.isEmpty() ? "" : " " + sourceDetail));
                        component.setDeadlineNote(displayMetadata.getDeadlineText(System.currentTimeMillis()));
                        component.putClientProperty("grindstone.deadline", displayMetadata.getDeadlineAt());
                        component.setExpansionListener(null);
                        component.setExpanded(!displayMetadata.isCollapsed(config.collapseDetailsByDefault()));
                        component.setExpansionListener(expanded -> clientThread.invokeLater(() -> {
                            if (profile == currentProfile && currentProfile.getGoals().contains(goal)) {
                                currentProfile.getMetadata(goal).setCollapsed(!expanded);
                            }
                        }));
                        component.setMaximumProgressChunks(config.maxProgressChunks());
                        component.setGoalActions(event -> editGoal(currentProfile, goal, title), event -> deleteGoal(currentProfile, goal, title));
                        component.addGoalAction("Set deadline...", "Set a personal UTC deadline and optional reminder in Grindstone's header.", event -> editDeadline(currentProfile, goal));
                        if (goal instanceof CustomMilestone) {
                            CustomMilestone custom = (CustomMilestone) goal;
                            long shownTarget = shownMaximum;
                            component.setManualActions(shownCurrent, shownTarget, () -> changeManualProgress(currentProfile, custom, value -> Math.min(custom.getProgressMaximum(), value + 1)), () -> changeManualProgress(currentProfile, custom, value -> value >= custom.getProgressMaximum() ? 0 : 1), () -> promptManualProgress(currentProfile, custom, shownCurrent, shownTarget));
                        }
                        RepeatSchedule repeatSchedule = displayRecurring.getSchedule();
                        long repeatTarget = displayRecurring.getTarget();
                        boolean pinned = displayMetadata.isPinned();
                        component.putClientProperty("grindstone.pinned", pinned);
                        component.addGoalAction(pinned ? "Unpin" : "Pin", "Pinned milestones appear first, while still respecting your search and filters.", event -> clientThread.invokeLater(() -> {
                            if (profile == currentProfile && currentProfile.getGoals().contains(goal)) {
                                currentProfile.getMetadata(goal).setPinned(!pinned);
                                currentProfile.touch();
                                render();
                            }
                        }));
                        if (config.showCompletionHistory()) {
                            component.addGoalAction("History...", "View recurring periods, progress, outcomes and first-observed completion dates.", event -> clientThread.invokeLater(() -> {
                                if (profile != currentProfile || !currentProfile.getGoals().contains(goal)) {
                                    return;
                                }
                                Supplier<MilestoneHistoryPanel> history = MilestoneHistoryPanel.createSnapshot(currentProfile.getMetadata(goal));
                                SwingUtilities.invokeLater(() -> {
                                    if (profile == currentProfile) {
                                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), history.get(), title + " — history", JOptionPane.PLAIN_MESSAGE);
                                    }
                                });
                            }));
                        }
                        if (RecurringMilestone.supports(goal)) {
                            component.addGoalAction(repeating ? "Routine schedule..." : "Make routine...", "Choose a daily, weekly or monthly routine, or return to a one-time milestone.", event -> editRepeat(currentProfile, goal, repeatSchedule, repeatTarget));
                        }
                        if (repeating) {
                            component.addGoalAction("Restart current period...", "Archive observed progress and capture a fresh baseline. Game progress is never reset.", event -> restartPeriod(currentProfile, goal));
                        }
                        if (goal instanceof SessionExperienceMilestone) {
                            component.addGoalAction("Restart session XP...", "Clear only this milestone's observed login gains and capture the current XP as its new baseline.", event -> {
                                if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), "Reset this milestone's session XP progress? Your game XP and saved completion history are unchanged.", "Restart session milestone", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                                    clientThread.invokeLater(() -> {
                                        if (profile == currentProfile && currentProfile.getGoals().contains(goal) && hasClientProfile(currentProfile)) {
                                            ((SessionExperienceMilestone) goal).restart();
                                            goal.refresh(client);
                                            currentProfile.touch();
                                            refreshAll();
                                        }
                                    });
                                }
                            });
                        }
                        component.setAchievementNote(repeating ? displayRecurring.getSchedule() + " · " + (displayRecurring.getBaselineAt() == 0 ? "Baseline pending" : "This period") : config.showCompletionHistory() && !displayMetadata.getCompletions().isEmpty() && shownStatus != MilestoneStatus.COMPLETE ? "Previously reached · now incomplete" : null);

                        MilestoneView view = new MilestoneView(goal, component, title, category, shownStatus, hasProgress, shownCurrent, shownMaximum);
                        cardCache.put(goal, cacheKey, view);
                        return view;
                    });
                }
            }

            if (progressOverlay != null) {
                if (captureOverlay) {
                    progressOverlay.update(overlayEntries, client.getCanvasHeight());
                } else {
                    progressOverlay.clear();
                }
            }
            Map<String, String> snapshotStatuses = new LinkedHashMap<>();
            Map<String, String> snapshotTips = new LinkedHashMap<>();
            Set<String> missingSnapshots = new LinkedHashSet<>();
            prepareSnapshotStatuses(currentProfile, snapshotStatuses, snapshotTips, missingSnapshots);
            long dueCount = currentProfile == null || !config.showDeadlineReminders() ? 0 : currentProfile.getGoals().stream().filter(goal -> {
                MilestoneMetadata metadata = currentProfile.getMetadata(goal);
                return metadata.isDeadlineReminder() && metadata.getDeadlineAt() > 0 && metadata.getDeadlineAt() - System.currentTimeMillis() <= 86_400_000L && (metadata.getRecurring().isActive() ? metadata.getRecurring().getStatus() : goal.getStatus()) != MilestoneStatus.COMPLETE;
            }).count();
            List<String> catchUp = new ArrayList<>();
            if (currentProfile != null) {
                for (Milestone goal : currentProfile.getGoals()) {
                    String reading = currentProfile.getMetadata(goal).getCatchUpText();
                    if (reading != null) {
                        catchUp.add(reading);
                    }
                }
            }
            String capturedCatchUp = "Progress since the previous Grindstone reading:\n\n" + String.join("\n", catchUp) + "\n\nUpdated from your character's available game data when you returned to RuneLite. Exact achievement times and gains across routine resets cannot be reconstructed. Open the collection log, bank or relevant activity interface to refresh data that is not loaded yet.";
            SwingUtilities.invokeLater(() -> {
                if (version != renderVersion.get()) {
                    return;
                }

                snapshots.updateSnapshots(snapshotStatuses, snapshotTips, missingSnapshots);
                snapshots.setVisible(config.showUpdateStatus() && currentProfile != null);
                catchUpText = capturedCatchUp;
                catchUpButton.setText("Since last seen - " + catchUp.size() + " updated");
                catchUpButton.setVisible(!catchUp.isEmpty());
                updateStatusLabel.setText("<html>" + (storageStatus == null ? "" : HtmlUtils.escape(storageStatus) + "<br>") + (dueCount == 0 ? "" : dueCount + " personal deadline(s) due within 24h / past due<br>") + "</html>");
                updateStatusLabel.setVisible(storageStatus != null || dueCount > 0);
                updateStatusLabel.setToolTipText("Storage and save messages. See Snapshots for captured game data.");
                List<MilestoneView> allGoals = new ArrayList<>(preparedViews.size());
                for (Supplier<MilestoneView> prepared : preparedViews) {
                    allGoals.add(prepared.get());
                }
                renderPrepared(currentProfile, allGoals);
            });
        });
    }

    private void prepareSnapshotStatuses(GrindstoneProfile active, Map<String, String> statuses, Map<String, String> tips, Set<String> missing) {
        Map<PlayerStorageSource, Instant> seen = active == null ? Collections.emptyMap() : active.getPlayerStorage().getLastSeen();
        DateTimeFormatter time = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
        for (PlayerStorageSource source : new PlayerStorageSource[]{PlayerStorageSource.INVENTORY, PlayerStorageSource.EQUIPMENT, PlayerStorageSource.BANK}) {
            Instant captured = seen.get(source);
            statuses.put(source.toString(), captured == null ? "Not loaded" : time.format(captured));
            if (captured == null) {
                missing.add(source.toString());
            }
            String refresh = source == PlayerStorageSource.BANK ? "Open your bank to refresh its contents." : "Updated automatically while logged in.";
            tips.put(source.toString(), "<html>" + refresh + "<br>" + (captured == null ? "No snapshot saved yet." : "Last updated " + time.format(captured) + (active.getPlayerStorage().isLoadedThisSession(source) ? " this login." : ". Saved from an earlier session; reopen to refresh.")) + "<br>Only this character's known contents are counted.</html>");
        }
        int known = active == null ? 0 : active.getCollectionLogHistory().getKnownUnlockCount();
        boolean live = active != null && client.getGameState() == GameState.LOGGED_IN && client.getAccountHash() == active.getHash();
        int total = live && client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX) > 0 ? client.getVarpValue(VarPlayerID.COLLECTION_COUNT) : -1;
        boolean complete = active != null && active.getCollectionLogHistory().hasSnapshot();
        boolean current = complete && (total < 0 || known == total);
        String logStatus = total >= 0 && known > total ? known + " known · mismatch" : current ? time.format(Instant.ofEpochMilli(active.getCollectionLogHistory().getSnapshotAt())) : known > 0 || total >= 0 ? known + (total < 0 ? " known" : " / " + total) + " · partial" : "Not loaded";
        statuses.put("Collection log", logStatus);
        if (!current) {
            missing.add("Collection log");
        }
        String captured = complete ? "<br>Last full capture: " + time.format(Instant.ofEpochMilli(active.getCollectionLogHistory().getSnapshotAt())) + "." : "<br>No full snapshot yet; collection milestones remain unavailable.";
        tips.put("Collection log", "<html>Open your in-game collection log to capture available unlocks.<br>" + (current ? "Full snapshot verified: " + known + " unlocks." : "Incomplete data: " + known + " known unlocks" + (total < 0 ? "." : " out of " + total + ".") + "<br>The game may not expose every unlock just by opening the log.<br>Its Search interface can expose further collection data.") + captured + "<br>Reopen after playing on another device.</html>");
        int loaded = 0;
        Instant latestOther = null;
        StringBuilder other = new StringBuilder("<html>Open each storage in game to refresh it. Last known snapshots are saved between sessions; reopen storage to refresh.");
        for (PlayerStorageSource source : PlayerStorageSource.values()) {
            if (source == PlayerStorageSource.INVENTORY || source == PlayerStorageSource.EQUIPMENT || source == PlayerStorageSource.BANK) {
                continue;
            }
            Instant capturedAt = seen.get(source);
            if (capturedAt != null) {
                loaded++;
                if (latestOther == null || capturedAt.isAfter(latestOther)) {
                    latestOther = capturedAt;
                }
            }
            other.append("<br>").append(HtmlUtils.escape(source.toString())).append(": ").append(capturedAt == null ? "Not loaded" : time.format(capturedAt));
        }
        statuses.put("Other storage", latestOther == null ? "Not loaded" : time.format(latestOther));
        if (loaded < PlayerStorageSource.values().length - 3) {
            missing.add("Other storage");
        }
        tips.put("Other storage", other.append("</html>").toString());
    }

    private void renderPrepared(GrindstoneProfile currentProfile, List<MilestoneView> allGoals) {
        if (currentProfile == null) {
            main.removeAll();
            layoutKey = null;
            updateSummary(new ArrayList<>());
            resultsLabel.setText("0 tracked");
            addMessage("Log in to get started", "Your milestones load automatically for your logged-in character.");
            refreshPanel();
            return;
        }

        if (!config.enabled()) {
            main.removeAll();
            layoutKey = null;
            updateSummary(new ArrayList<>());
            resultsLabel.setText("0 tracked");
            addMessage("Grindstone is disabled", "Enable Grindstone in the plugin configuration to display your milestones.");
            refreshPanel();
            return;
        }

        preparedGoals = allGoals;
        updateCategoryOptions(allGoals);
        updateSummary(allGoals);

        List<MilestoneView> filteredGoals = filterGoals(allGoals);
        if (todayMode) {
            filteredGoals.removeIf(view -> view.component.getClientProperty("grindstone.repeat") == RepeatSchedule.NONE);
        }
        sortGoals(filteredGoals);
        if (todayMode) {
            filteredGoals.sort(Comparator.comparingLong((MilestoneView view) -> getNextReset(view)).thenComparing(view -> isComplete(view.status)));
        }
        resultsLabel.setText("Showing " + filteredGoals.size() + " of " + allGoals.size());
        resultsLabel.setToolTipText("Milestones matching your search, filters and visibility settings.");

        List<Object> rows = new ArrayList<>();
        for (MilestoneView view : filteredGoals) {
            rows.add(Arrays.asList(view.goal, view.category, view.component.getClientProperty("grindstone.repeat"), view.component.getClientProperty("grindstone.pinned")));
        }
        List<?> nextLayout = Arrays.asList(currentProfile, todayMode, config.showTodayOverview(), config.categorizedGoals(), config.groupRecurringGoals(), config.groupSkillGoals(), config.showPinnedSection(), new LinkedHashMap<>(collapsedCategories), new LinkedHashMap<>(collapsedRepeats), rows, allGoals.size());
        if (nextLayout.equals(layoutKey)) {
            for (MilestoneView view : filteredGoals) {
                MilestoneComponent previous = displayedCards.put(view.goal, view.component);
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
                List<MilestoneView> repeats = new ArrayList<>();
                for (MilestoneView view : filteredGoals) {
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
        for (MilestoneView view : filteredGoals) {
            displayedCards.put(view.goal, view.component);
        }
        if (todayMode || config.showTodayOverview()) {
            addTodayOverview(allGoals);
        }

        if (filteredGoals.isEmpty()) {
            if (currentProfile.getGoals().isEmpty()) {
                addMessage("No milestones yet", "Choose Add milestone to start tracking a skill, activity, item or personal milestone.");
            } else if (!todayMode && !allGoals.isEmpty() && allGoals.stream().allMatch(view -> isComplete(view.status))) {
                addMessage("All milestones complete", "Choose Add milestone for your next target, or Done to see your completed milestones.");
            } else {
                addMessage(todayMode ? "No matching routines" : "No matching milestones", todayMode ? "Clear filters or choose Make routine from a milestone's actions." : "Try another search term or clear your filters.");
            }
            refreshPanel();
            return;
        }

        List<MilestoneView> pinnedGoals = new ArrayList<>();
        List<MilestoneView> remainingGoals = new ArrayList<>();
        for (MilestoneView view : filteredGoals) {
            if (config.showPinnedSection() && Boolean.TRUE.equals(view.component.getClientProperty("grindstone.pinned"))) {
                pinnedGoals.add(view);
            } else {
                remainingGoals.add(view);
            }
        }
        if (!pinnedGoals.isEmpty()) {
            JLabel pinnedHeading = new JLabel("Pinned");
            pinnedHeading.setFont(UiTypography.SECTION);
            pinnedHeading.setAlignmentX(LEFT_ALIGNMENT);
            main.add(pinnedHeading);
            main.add(Box.createVerticalStrut(8));
            for (MilestoneView view : pinnedGoals) {
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
                List<MilestoneView> repeats = new ArrayList<>();
                for (MilestoneView view : remainingGoals) {
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
            for (MilestoneView view : remainingGoals) {
                view.component.setAlignmentX(LEFT_ALIGNMENT);
                main.add(view.component);
                main.add(Box.createVerticalStrut(12));
            }
            refreshPanel();
            return;
        }
        Map<MilestoneCategory, List<MilestoneView>> groupedGoals = groupGoals(remainingGoals);

        for (Map.Entry<MilestoneCategory, List<MilestoneView>> entry : groupedGoals.entrySet()) {
            addCategorySection(entry.getKey(), entry.getValue());
            main.add(Box.createVerticalStrut(10));
        }

        refreshPanel();
        revealPrepared(allGoals);
    }

    private void addTodayOverview(List<MilestoneView> goals) {
        List<MilestoneView> recurring = new ArrayList<>();
        for (MilestoneView view : goals) {
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
            long next = recurring.stream().mapToLong(this::getNextReset).min().orElse(0);
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
        progress.setToolTipText("Average milestone completion, not a sum of different units. Includes all visible routines, independent of search filters.");
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progress.setAlignmentX(LEFT_ALIGNMENT);
        overview.add(progress);
        long next = recurring.stream().mapToLong(this::getNextReset).min().orElse(0);
        JLabel due = new JLabel("<html>Next reset: " + DateTimeFormatter.ofPattern("EEE HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(next)) + "<br>" + (waiting == recurring.size() ? "Waiting for readings" : Math.round(ratio * 100) + "% of known progress") + (waiting > 0 ? " · " + waiting + " waiting" : "") + "</html>");
        routineDue = due;
        due.setFont(UiTypography.META);
        due.setAlignmentX(LEFT_ALIGNMENT);
        overview.add(Box.createVerticalStrut(6));
        overview.add(due);
        main.add(overview, 0);
        main.add(routineGap, 1);
    }

    private long getNextReset(MilestoneView view) {
        RepeatSchedule schedule = (RepeatSchedule) view.component.getClientProperty("grindstone.repeat");
        return schedule == RepeatSchedule.NONE ? Long.MAX_VALUE : schedule.getNextReset(schedule.getPeriodStart(System.currentTimeMillis()));
    }

    private void updateSummary(List<MilestoneView> goals) {
        int completed = 0;
        int active = 0;
        int notStarted = 0;
        int known = 0;

        for (MilestoneView view : goals) {
            if (!Boolean.TRUE.equals(view.component.getClientProperty("grindstone.ready"))) {
                continue;
            }
            if (isComplete(view.status)) {
                completed++;
            } else if (view.status == MilestoneStatus.IN_PROGRESS) {
                active++;
            } else {
                notStarted++;
            }
            known++;
        }

        int percentage = goals.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / goals.size());
        overallProgress.setVisible(!goals.isEmpty());
        overallProgress.setMaximumChunks(config.maxProgressChunks());
        overallProgress.setProgress(completed, goals.size());
        overallProgress.setStatus(!goals.isEmpty() && completed == goals.size() ? MilestoneStatus.COMPLETE : active > 0 || completed > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED);
        summaryLabel.setText(goals.isEmpty() ? "Create your first milestone" : completed + " of " + goals.size() + " completed");
        summaryLabel.setToolTipText("Overview of all enabled milestones, independent of search and filters.");
        progressPercent.setText(percentage + "%");
        overallProgress.setToolTipText("Completed milestones out of all enabled milestones.");
        allStatusesButton.setText("All " + goals.size());
        unfinishedStatusesButton.setText("Unfinished " + (goals.size() - completed));
        newGoalsButton.setText("New " + notStarted);
        activeGoalsButton.setText("Active " + active);
        completeGoalsButton.setText("Done " + completed);
        waitingGoalsButton.setText("Waiting " + (goals.size() - known));
        syncQuickStatus();
    }

    private List<MilestoneView> filterGoals(List<MilestoneView> goals) {
        List<MilestoneView> filtered = new ArrayList<>();
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
            selections.add("Completed routines hidden by settings");
        }
        filterSummary.setVisible(!selections.isEmpty());
        String selectionText = String.join(" · ", selections);
        filterSummary.setText("<html><div style='width: 190px'>" + HtmlUtils.escape(selectionText) + "</div></html>");
        filterSummary.setToolTipText(selectionText);

        for (MilestoneView view : goals) {
            RepeatSchedule schedule = (RepeatSchedule) view.component.getClientProperty("grindstone.repeat");
            if (isComplete(view.status) && schedule == RepeatSchedule.NONE && !COMPLETE.equals(selectedStatus)) {
                continue;
            }
            if (!matchesSearch(query, view.title, view.goal.getDescription(), view.category.getDisplayName())) {
                continue;
            }

            if (selectedCategory != null && !ALL_CATEGORIES.equals(selectedCategory) && !selectedCategory.equals(view.category.getDisplayName())) {
                continue;
            }

            if (!matchesStatus(view.status, selectedStatus, config.hideCompletedGoals(), Boolean.TRUE.equals(view.component.getClientProperty("grindstone.ready")))) {
                continue;
            }
            if (!matchesSchedule(schedule, selectedSchedule) || pinnedOnly.isSelected() && !Boolean.TRUE.equals(view.component.getClientProperty("grindstone.pinned"))) {
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

    static boolean matchesStatus(MilestoneStatus status, String selected, boolean hideCompleted, boolean ready) {
        if (WAITING.equals(selected)) {
            return !ready;
        }
        if (!ready && (NOT_STARTED.equals(selected) || IN_PROGRESS.equals(selected) || COMPLETE.equals(selected))) {
            return false;
        }
        return matchesStatus(status, selected, hideCompleted);
    }

    static boolean matchesType(Milestone goal, MilestoneStatus status, String selectedType, long deadline, long now) {
        if (CUSTOM.equals(selectedType)) {
            return goal instanceof ManualMilestone;
        }
        if (AUTOMATIC.equals(selectedType)) {
            return !(goal instanceof ManualMilestone) && !(goal instanceof LinkedMilestone);
        }
        if (LINKED.equals(selectedType)) {
            return goal instanceof LinkedMilestone;
        }
        if (BASE.equals(selectedType)) {
            return goal instanceof BaseSkillMilestone;
        }
        if (SESSION.equals(selectedType)) {
            return goal instanceof SessionExperienceMilestone;
        }
        if (DEADLINES.equals(selectedType)) {
            return deadline > 0;
        }
        if (PAST_DUE.equals(selectedType)) {
            return deadline > 0 && now >= deadline && !isComplete(status);
        }
        return true;
    }

    static boolean matchesStatus(MilestoneStatus status, String selectedStatus, boolean hideCompleted) {
        if (selectedStatus == null || ALL_STATUSES.equals(selectedStatus)) {
            return !hideCompleted || status != MilestoneStatus.COMPLETE;
        }

        if (UNFINISHED.equals(selectedStatus)) {
            return status != MilestoneStatus.COMPLETE;
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

    private void sortGoals(List<MilestoneView> goals) {
        String sort = (String) sortFilter.getSelectedItem();

        if (SORT_NAME_DESC.equals(sort)) {
            goals.sort(Comparator.comparing((MilestoneView view) -> view.title, String.CASE_INSENSITIVE_ORDER).reversed());
        } else if (SORT_CLOSEST.equals(sort)) {
            goals.sort(Comparator.comparing((MilestoneView view) -> isComplete(view.status)) .thenComparing(Comparator.comparingDouble(GrindstonePanel::getProgressRatio).reversed()) .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_PROGRESS_DESC.equals(sort)) {
            goals.sort(Comparator.comparingDouble(GrindstonePanel::getProgressRatio).reversed() .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_PROGRESS_ASC.equals(sort)) {
            goals.sort(Comparator.comparingDouble(GrindstonePanel::getProgressRatio) .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        } else if (SORT_STATUS.equals(sort)) {
            goals.sort(Comparator.comparingInt((MilestoneView view) -> getStatusOrder(view.status)) .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        } else {
            goals.sort(Comparator.comparing((MilestoneView view) -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
    }

    private static double getProgressRatio(MilestoneView view) {
        if (!view.hasProgress || view.progressMaximum <= 0) {
            return isComplete(view.status) ? 1.0 : 0.0;
        }

        long current = Math.max(0, Math.min(view.progressCurrent, view.progressMaximum));
        return current / (double) view.progressMaximum;
    }

    private static int getStatusOrder(MilestoneStatus status) {
        if (status == MilestoneStatus.IN_PROGRESS) {
            return 0;
        }

        if (status == MilestoneStatus.NOT_STARTED) {
            return 1;
        }

        return 2;
    }

    private Map<MilestoneCategory, List<MilestoneView>> groupGoals(List<MilestoneView> goals) {
        Map<MilestoneCategory, List<MilestoneView>> grouped = new LinkedHashMap<>();

        for (MilestoneView view : goals) {
            grouped.computeIfAbsent(view.category, key -> new ArrayList<>()).add(view);
        }

        return grouped;
    }

    private void updateCategoryOptions(List<MilestoneView> goals) {
        String previous = (String) categoryFilter.getSelectedItem();
        Set<String> categories = new LinkedHashSet<>();
        categories.add(ALL_CATEGORIES);

        for (MilestoneView view : goals) {
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

    private void addRepeatSection(RepeatSchedule schedule, List<MilestoneView> goals) {
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
            for (MilestoneView view : goals) {
                view.component.setAlignmentX(LEFT_ALIGNMENT);
                main.add(view.component);
                main.add(Box.createVerticalStrut(12));
            }
        }
    }
    private void updateRepeatHeading(RepeatSchedule schedule, List<MilestoneView> goals, JButton heading) {
        boolean collapsed = Boolean.TRUE.equals(collapsedRepeats.get(schedule));
        long now = System.currentTimeMillis();
        long minutes = Math.max(1, (schedule.getNextReset(schedule.getPeriodStart(now)) - now + 59_999) / 60_000);
        String countdown = minutes >= 1440 ? minutes / 1440 + "d " + minutes % 1440 / 60 + "h" : minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
        long completed = goals.stream().filter(view -> isComplete(view.status)).count();
        heading.setText("<html>" + (collapsed ? "▶ " : "▼ ") + schedule + " · " + completed + "/" + goals.size() + "<br>Resets in " + countdown + " (UTC)</html>");
    }

    private void addCategorySection(MilestoneCategory category, List<MilestoneView> goals) {
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
        countLabel.setToolTipText("Milestones in this category matching your current filters.");
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

            for (List<MilestoneView> group : config.groupSkillGoals() ? SkillMilestoneGrouping.group(goals, view -> view.goal) : goals.stream().map(Collections::singletonList).collect(Collectors.toList())) {
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
                JLabel heading = new JLabel(SkillMilestoneGrouping.getSkill(group.get(0).goal).getName());
                heading.setFont(UiTypography.TITLE);
                heading.setBorder(BorderFactory.createEmptyBorder(0, 5, 6, 0));
                heading.setToolTipText("Related milestones for this skill. Each target keeps its own progress, actions and history.");
                skillGroup.add(heading);
                for (MilestoneView view : group) {
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
    private void editRepeat(GrindstoneProfile active, Milestone goal, RepeatSchedule schedule, long target) {
        if (active == null || profile != active) {
            return;
        }
        RepeatMilestoneForm form = new RepeatMilestoneForm();
        form.load(schedule, target);
        if (goal instanceof ChecklistMilestone) {
            form.setFixedTarget(goal.getProgressMaximum());
        }
        while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), form, "Repeat schedule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                form.commit();
                RepeatSchedule selected = form.getSchedule();
                long selectedTarget = form.getTarget();
                if ((schedule != selected || target != selectedTarget) && JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), "Repeat: " + schedule + " → " + selected + "\nPeriod target: " + target + " → " + selectedTarget + "\nThe current observed period will be archived. Undo is available until a later edit or reset.", "Preview schedule change", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile == active && active.getGoals().contains(goal)) {
                        Milestone collision = active.findMatchingGoal(goal, selected);
                        if (collision != null && collision != goal) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "That tracker already has a milestone with this schedule."));
                            return;
                        }
                        Consumer<Milestone> restore = active.snapshotEdit(goal);
                        configureRepeat(active, goal, selected, selectedTarget);
                        active.touch();
                        recordGoalUndo(active, goal, "schedule change", () -> restore.accept(goal));
                        render();
                    }
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Enter a valid period target.");
            }
        }
    }
    private void restartPeriod(GrindstoneProfile active, Milestone goal) {
        if (profile != active || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), "Archive the current observed period and capture a fresh baseline?\nCustom progress resets to zero. Your game progress is unaffected.\nUndo is available until a later edit or scheduled reset.", "Restart current period", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) {
                return;
            }
            RecurringMilestone recurring = active.getMetadata(goal).getRecurring();
            if (!recurring.isActive()) {
                return;
            }
            Consumer<Milestone> restore = active.snapshotEdit(goal);
            recurring.restart(System.currentTimeMillis());
            if (goal instanceof ManualMilestone) {
                ((ManualMilestone) goal).resetProgress();
            }
            active.touch();
            recordGoalUndo(active, goal, "period restart", () -> restore.accept(goal));
            render();
        });
    }

    private void configureRepeat(GrindstoneProfile active, Milestone goal, RepeatSchedule schedule, long target) {
        if (!RecurringMilestone.supports(goal)) {
            return;
        }
        RecurringMilestone recurring = active.getMetadata(goal).getRecurring();
        if (goal instanceof ChecklistMilestone) {
            target = goal.getProgressMaximum();
            if (schedule != RepeatSchedule.NONE && (recurring.getSchedule() != schedule || recurring.getTarget() != target)) {
                ((ChecklistMilestone) goal).resetProgress();
            }
        }
        boolean newSchedule = recurring.getSchedule() != schedule;
        recurring.configure(schedule, target, System.currentTimeMillis());
        if (goal instanceof CustomMilestone && schedule != RepeatSchedule.NONE) {
            CustomMilestone custom = (CustomMilestone) goal;
            custom.update(custom.getTitle(null), custom.getDescription(), custom.getCategory(), newSchedule ? 0 : Math.min(custom.getProgressCurrent(), target), target);
        }
    }
    private void transferBackup(boolean importing) {
        GrindstoneProfile active = profile;
        backupActions.show(this, active, () -> profile == active, () -> {
            if (profile == active) {
                for (Milestone goal : active.getGoals()) {
                    goal.refresh(client);
                }
                render();
            }
        }, importing);
    }
    private boolean isCompletedQuestOrDiary(Milestone goal) {
        if (goal.getCategory() != MilestoneCategory.QUEST && goal.getCategory() != MilestoneCategory.ACHIEVEMENT_DIARY) {
            return false;
        }
        goal.refresh(client);
        return goal.getStatus() == MilestoneStatus.COMPLETE;
    }

    private void addPreset(MilestonePreset preset) {
        GrindstoneProfile active = profile;
        if (active == null) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            List<Milestone> goals = preset.createGoals();
            goals.removeIf(this::isCompletedQuestOrDiary);
            String summary = additionSummary(active, goals, goal -> RepeatSchedule.NONE);
            SwingUtilities.invokeLater(() -> {
                if (profile != active || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), previewPane(summary), "Preview " + preset, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    List<Milestone> before = new ArrayList<>(active.getGoals());
                    for (Milestone goal : goals) {
                        if (!isCompletedQuestOrDiary(goal) && active.add(goal)) {
                            goal.refresh(client);
                        }
                    }
                    recordAddedUndo(active, before, "preset");
                    render();
                });
            });
        });
    }


    private String additionSummary(GrindstoneProfile active, List<Milestone> candidates, Function<Milestone, RepeatSchedule> schedule) {
        StringBuilder rows = new StringBuilder();
        int missing = 0;
        for (Milestone goal : candidates) {
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
        return missing + " new milestones · " + (candidates.size() - missing) + " existing matches kept\nExisting targets and schedules are never overwritten. Additions can be undone.\n\n" + rows;
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
            if (!active.getCollectionLogHistory().hasSnapshot()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "First open your in-game collection log to load a full snapshot. Then add collection-log milestones once the snapshot is complete."));
                return;
            }
            Map<Integer, String> pages = CollectionLogPages.discover(client);
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                if (pages.isEmpty()) {
                    JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Collection-log metadata is not ready. Log in and try again.");
                    return;
                }
                if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), "Add missing milestones from all " + pages.size() + " collection-log pages? Existing milestones are kept.", "Add collection-log pages", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    List<Milestone> before = new ArrayList<>(active.getGoals());
                    pages.forEach((id, name) -> active.add(new CollectionLogEntryMilestone(itemManager, id, name)));
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
        while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), form, "Preview recurring routine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                List<Milestone> selected = form.selectedGoals();
                Map<Milestone, MilestoneMetadata> metadata = new IdentityHashMap<>();
                for (Milestone goal : selected) {
                    metadata.put(goal, form.getMetadata(goal, System.currentTimeMillis()));
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    List<Milestone> before = new ArrayList<>(active.getGoals());
                    for (Milestone goal : selected) {
                        if (active.add(goal, metadata.get(goal))) {
                            goal.refresh(client);
                        }
                    }
                    recordAddedUndo(active, before, "routine");
                    render();
                    int count = active.getGoals().size() - before.size();
                    SwingUtilities.invokeLater(() -> {
                        if (profile == active) {
                            JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), count + " routines added. Existing schedule matches were skipped.");
                        }
                    });
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Enter valid targets for the selected milestones.");
            }
        }
    }

    private void recordAddedUndo(GrindstoneProfile active, List<Milestone> before, String label) {
        List<Milestone> added = new ArrayList<>(active.getGoals());
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
            List<HistoryRecord> records = HistoryRecord.createSnapshot(active, this::getSafeTitle);
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Account milestone history", Dialog.ModalityType.APPLICATION_MODAL);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setContentPane(new HistoryBrowserPanel(records, id -> {
                    dialog.dispose();
                    revealGoal(active, id);
                }));
                dialog.pack();
                DialogPlacement.center(dialog, this);
                dialog.setVisible(true);
            });
        });
    }

    private void revealGoal(GrindstoneProfile active, String id) {
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            Milestone goal = active.getGoals().stream().filter(value -> active.getMetadata(value).getId().equals(id)).findFirst().orElse(null);
            if (goal == null) {
                return;
            }
            String title = getSafeTitle(goal);
            boolean complete = active.getMetadata(goal).getRecurring().isActive() ? active.getMetadata(goal).getRecurring().getStatus() == MilestoneStatus.COMPLETE : goal.getStatus() == MilestoneStatus.COMPLETE;
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

    private void revealPrepared(List<MilestoneView> goals) {
        if (pendingReveal == null) {
            return;
        }
        String id = pendingReveal;
        pendingReveal = null;
        for (MilestoneView view : goals) {
            if (id.equals(view.component.getClientProperty("grindstone.id")) && main.isAncestorOf(view.component)) {
                SwingUtilities.invokeLater(() -> view.component.scrollRectToVisible(new Rectangle(0, 0, view.component.getWidth(), view.component.getHeight())));
                return;
            }
        }
        JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "This milestone is hidden by its tracker visibility setting. Enable that milestone type in Grindstone configuration to reveal it.");
    }

    private void showAddGoalDialog() {
        showAddGoalDialog(null);
    }

    public void showAddGoalDialog(Milestone initialGoal) {
        GrindstoneProfile active = profile;
        if (active == null) {
            JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Log in to add milestones to your player profile.");
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || client.getGameState() != GameState.LOGGED_IN) {
                return;
            }
            Map<Integer, Map<String, Supplier<Milestone>>> choices =
            CompletionMilestoneChoices.getAvailable(client, active.getCollectionLogHistory());
            Map<Milestone, RepeatSchedule> existing = new LinkedHashMap<>();
            for (Milestone goal : active.getGoals()) {
                existing.put(goal, active.getMetadata(goal).getRecurring().getSchedule());
            }
            Set<Integer> obtained = new LinkedHashSet<>(active.getCollectionLogHistory().getSnapshot().keySet());
            SwingUtilities.invokeLater(() -> {
                if (profile == active) {
                    showAddGoalDialog(active, choices, existing, initialGoal, obtained);
                }
            });
        });
    }

    private void showAddGoalDialog(GrindstoneProfile active, Map<Integer, Map<String, Supplier<Milestone>>> choices, Map<Milestone, RepeatSchedule> existingGoalsSnapshot, Milestone initialGoal, Set<Integer> obtained) {
        MilestoneEditorPanel form = new MilestoneEditorPanel(itemManager, itemSearchService, choices);
        form.setExistingGoals(existingGoalsSnapshot);
        form.setCollectionSnapshotStatus(active.getCollectionLogHistory().hasSnapshot(), active.getCollectionLogHistory().getKnownUnlockCount(), active.getCollectionLogHistory().getSnapshotAt());
        form.hideObtainedCollectionItems(obtained);
        if (initialGoal != null) {
            try {
                form.loadGoal(initialGoal);
            } catch (IllegalArgumentException exception) {
                form.stopSearch();
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), exception.getMessage(), "Add milestone", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        Milestone candidate = MilestoneCreationDialog.showDialog(this, form);
        if (candidate == null) {
            return;
        }
        RepeatSchedule schedule = form.getRepeatSchedule();
        long periodTarget = form.getRepeatTarget();
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            if (isCollectionLogGoal(candidate) && !active.getCollectionLogHistory().hasSnapshot()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "A full collection-log snapshot is required before adding collection milestones. Open your collection log to capture it automatically."));
                return;
            }
            Milestone existing = active.findMatchingGoal(candidate, schedule);
            if (existing != null) {
                String title = getSafeTitle(existing);
                long oldTarget = existing.getProgressMaximum();
                long newTarget = candidate.getProgressMaximum();
                SwingUtilities.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    if (existing.supportsTarget()) {
                        if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), title + " is already tracked.\nUpdate its target from " + String.format(Locale.ROOT, "%,d to %,d?", oldTarget, newTarget) + "\nRepeat: " + schedule + (schedule == RepeatSchedule.NONE ? " (lifetime tracking)" : String.format(Locale.ROOT, " · %,d per period", periodTarget)) + "\nChanging repeat settings archives the current observed period.", "Update existing milestone", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                            clientThread.invokeLater(() -> {
                                if (profile == active && active.getGoals().contains(existing)) {
                                    Consumer<Milestone> restore = active.snapshotEdit(existing);
                                    existing.setTarget(newTarget);
                                    configureRepeat(active, existing, schedule, periodTarget);
                                    active.touch();
                                    recordGoalUndo(active, existing, "edit", () -> restore.accept(existing));
                                    render();
                                }
                            });
                        }
                    } else {
                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "This milestone is already in your profile.");
                    }
                });
            } else {
                candidate.refresh(client);
                boolean diary = candidate.getCategory() == MilestoneCategory.ACHIEVEMENT_DIARY;
                boolean quest = candidate.getCategory() == MilestoneCategory.QUEST;
                if ((diary || quest) && candidate.getStatus() == MilestoneStatus.COMPLETE) {
                    SwingUtilities.invokeLater(() -> {
                        if (profile == active) {
                            JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), diary ? "This achievement diary is already complete. Choose an unfinished diary to track." : "This quest or quest series is already complete. Choose an unfinished quest to track.");
                        }
                    });
                    return;
                }
                configureRepeat(active, candidate, schedule, periodTarget);
                active.add(candidate, active.getMetadata(candidate));
                recordUndo(active, "add milestone", () -> active.remove(candidate), () -> active.getGoals().contains(candidate));
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

    private void editGoal(GrindstoneProfile active, Milestone goal, String title) {
        if (profile != active) {
            return;
        }
        if (goal instanceof LinkedMilestone) {
            editLinkedGoal(active, (LinkedMilestone) goal);
            return;
        }
        if (goal instanceof CustomMilestone) {
            editCustomGoal(active, (CustomMilestone) goal, title);
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) {
                return;
            }
            if (isCollectionLogGoal(goal) && !active.getCollectionLogHistory().hasSnapshot()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Load a full collection-log snapshot before editing collection milestones. Open your collection log to capture it automatically."));
                return;
            }
            Map<Integer, Map<String, Supplier<Milestone>>> choices = CompletionMilestoneChoices.getForEditing(client, active.getCollectionLogHistory());
            RepeatSchedule schedule = active.getMetadata(goal).getRecurring().getSchedule();
            long periodTarget = active.getMetadata(goal).getRecurring().getTarget();
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                MilestoneEditorPanel form = new MilestoneEditorPanel(itemManager, itemSearchService, choices, true);
                form.setCollectionSnapshotStatus(active.getCollectionLogHistory().hasSnapshot(), active.getCollectionLogHistory().getKnownUnlockCount(), active.getCollectionLogHistory().getSnapshotAt());
                try {
                    form.loadGoal(goal);
                } catch (IllegalArgumentException exception) {
                    form.stopSearch();
                    JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), exception.getMessage());
                    return;
                }
                form.loadRepeat(schedule, periodTarget);
                Milestone replacement = MilestoneCreationDialog.showDialog(this, form, "Edit " + title, "Save changes");
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
                        Consumer<Milestone> restore = active.snapshotEdit(goal);
                        if (active.replaceGoal(goal, replacement, selectedSchedule)) {
                            replacement.refresh(client);
                            configureRepeat(active, replacement, selectedSchedule, selectedTarget);
                            recordGoalUndo(active, replacement, "edit", () -> restore.accept(replacement));
                            render();
                        }
                    } catch (IllegalArgumentException exception) {
                        SwingUtilities.invokeLater(() -> {
                            if (profile == active) {
                                JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), exception.getMessage());
                            }
                        });
                    }
                });
            });
        });
    }

    private void editLinkedGoal(GrindstoneProfile active, LinkedMilestone original) {
        if (active == null || profile != active) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || original != null && !active.getGoals().contains(original)) {
                return;
            }
            List<LinkedMilestoneForm.Choice> sources = new ArrayList<>();
            for (Milestone goal : active.getGoals()) {
                if (!(goal instanceof LinkedMilestone)) {
                    MilestoneMetadata metadata = active.getMetadata(goal);
                    boolean recurring = metadata.getRecurring().isActive();
                    sources.add(new LinkedMilestoneForm.Choice(metadata.getId(), getSafeTitle(goal) + (recurring ? " · " + metadata.getRecurring().getSchedule() : ""), recurring ? metadata.getRecurring().getTarget() : goal.getProgressMaximum(), goal instanceof CollectionLogItemMilestone, recurring || !metadata.getRecurring().getHistory().isEmpty()));
                }
            }
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                LinkedMilestoneForm form = new LinkedMilestoneForm(sources, original);
                JScrollPane scroll = new JScrollPane(form);
                scroll.setPreferredSize(new Dimension(520, 570));
                while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), scroll, original == null ? "Add linked milestone" : "Edit linked milestone", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        LinkedMilestone candidate = form.read();
                        if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), previewPane(candidate.getTitle(null) + "\n" + candidate.getMode() + " · " + candidate.getSources().size() + " source(s)\nTargets: " + Arrays.toString(candidate.getTargets()) + "\nMissing sources remain unavailable; this never performs game actions."), "Preview linked milestone", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                            continue;
                        }
                        clientThread.invokeLater(() -> {
                            if (profile != active || original != null && !active.getGoals().contains(original)) {
                                return;
                            }
                            if (original == null) {
                                active.add(candidate);
                                recordUndo(active, "add linked milestone", () -> active.remove(candidate), () -> active.getGoals().contains(candidate));
                            } else {
                                Consumer<Milestone> restore = active.snapshotEdit(original);
                                active.replaceGoal(original, candidate);
                                recordGoalUndo(active, candidate, "edit linked milestone", () -> restore.accept(candidate));
                            }
                            active.updateLinkedGoals(hasClientProfile(active));
                            render();
                        });
                        return;
                    } catch (IllegalArgumentException | ParseException failure) {
                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), failure.getMessage(), "Check linked milestone", JOptionPane.WARNING_MESSAGE);
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
            long revision = active.getRevision();
            List<BulkMilestoneForm.Row> rows = new ArrayList<>();
            for (Milestone goal : active.getGoals()) {
                MilestoneMetadata metadata = active.getMetadata(goal);
                rows.add(new BulkMilestoneForm.Row(goal, getSafeTitle(goal), getSafeCategory(goal), metadata.getRecurring().getSchedule()));
            }
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                BulkMilestoneForm form = new BulkMilestoneForm(rows);
                while (profile == active && JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), form, "Manage milestones", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        List<Milestone> selected = form.selectedGoals();
                        if (selected.isEmpty()) {
                            throw new IllegalArgumentException("Tick at least one milestone first.");
                        }
                        BulkMilestoneAction action = form.getAction();
                        MilestoneCategory category = form.getCategory();
                        RepeatSchedule schedule = form.getSchedule();
                        long target = action == BulkMilestoneAction.SCHEDULE ? form.getPeriodTarget() : 1;
                        long deadlineAt = form.getDeadlineAt();
                        boolean reminder = form.isReminderEnabled();
                        String details = action == BulkMilestoneAction.CATEGORY ? "\nCategory: " + category.getDisplayName() : action == BulkMilestoneAction.SCHEDULE ? "\nRepeat: " + schedule + " · target " + target + "\nChecklists require all their steps. Active manual progress resets when its period settings change." : action == BulkMilestoneAction.DEADLINE ? "\n" + (deadlineAt == 0 ? "Remove deadlines" : "Due by end of " + Instant.ofEpochMilli(deadlineAt - 1).atZone(ZoneOffset.UTC).toLocalDate() + " UTC") : "";
                        if (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), action + " · " + selected.size() + " milestones?" + details + (action == BulkMilestoneAction.DELETE ? "\nDeleting source milestones can make linked milestones unavailable.\nObserved history is archived." : "") + "\nThe action is validated before changes. One-step undo is available until a later edit or relevant reset.", "Preview bulk action", JOptionPane.OK_CANCEL_OPTION, action == BulkMilestoneAction.DELETE ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
                            continue;
                        }
                        clientThread.invokeLater(() -> {
                            if (profile != active) {
                                return;
                            }
                            try {
                                if (active.getRevision() != revision) {
                                    throw new IllegalArgumentException("Milestones changed while this manager was open. No changes applied; reopen it to review the current milestones.");
                                }
                                BulkMilestoneChanges.Change change = BulkMilestoneChanges.apply(active, selected, action, category, schedule, target, deadlineAt, reminder, System.currentTimeMillis());
                                recordUndo(active, "bulk " + action, change.undo, change.allowed);
                                render();
                            } catch (IllegalArgumentException failure) {
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), failure.getMessage(), "Bulk action not applied", JOptionPane.WARNING_MESSAGE));
                            }
                        });
                        return;
                    } catch (IllegalArgumentException | ParseException failure) {
                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), failure.getMessage(), "Review selections", JOptionPane.WARNING_MESSAGE);
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
            List<Milestone> selected = new ArrayList<>(active.getGoals());
            long revision = active.getRevision();
            if (selected.isEmpty()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "There are no milestones to clear."));
                return;
            }
            SwingUtilities.invokeLater(() -> {
                if (profile != active || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), "Remove all " + selected.size() + " milestones, including hidden milestones?\nRecorded history is retained within the 500-milestone archive limit.\nYou can undo this until another edit or a relevant period reset.", "Clear all milestones", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile != active) {
                        return;
                    }
                    if (active.getRevision() != revision) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Milestones changed while confirmation was open. Nothing was cleared; try again."));
                        return;
                    }
                    BulkMilestoneChanges.Change change = BulkMilestoneChanges.apply(active, selected, BulkMilestoneAction.DELETE, null, RepeatSchedule.NONE, 1, 0, false, System.currentTimeMillis());
                    recordUndo(active, "clear all milestones", change.undo, change.allowed);
                    render();
                });
            });
        });
    }

    private void editDeadline(GrindstoneProfile active, Milestone goal) {
        if (active == null || profile != active) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) {
                return;
            }
            MilestoneMetadata metadata = active.getMetadata(goal);
            long originalDeadline = metadata.getDeadlineAt();
            boolean originalReminder = metadata.isDeadlineReminder();
            SwingUtilities.invokeLater(() -> {
                DeadlineForm form = new DeadlineForm(originalDeadline, originalReminder);
                while (profile == active && JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), form, "Personal deadline", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                    try {
                        long deadlineAt = form.getDeadlineAt();
                        boolean reminder = form.isReminderEnabled();
                        clientThread.invokeLater(() -> {
                            if (profile == active && active.getGoals().contains(goal) && active.getMetadata(goal).getDeadlineAt() == originalDeadline && active.getMetadata(goal).isDeadlineReminder() == originalReminder) {
                                Consumer<Milestone> restore = active.snapshotEdit(goal);
                                active.getMetadata(goal).setDeadline(deadlineAt, reminder);
                                active.touch();
                                recordGoalUndo(active, goal, "deadline", () -> restore.accept(goal));
                                render();
                            }
                        });
                        return;
                    } catch (IllegalArgumentException failure) {
                        JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), failure.getMessage());
                    }
                }
            });
        });
    }

    private void editCustomGoal(GrindstoneProfile active, CustomMilestone goal, String title) {
        RepeatSchedule openedSchedule = active.getMetadata(goal).getRecurring().getSchedule();
        long openedPeriod = openedSchedule.getPeriodStart(System.currentTimeMillis());
        CustomMilestoneForm form = new CustomMilestoneForm();
        form.load(goal);
        RepeatMilestoneForm repeat = new RepeatMilestoneForm();
        repeat.load(active.getMetadata(goal).getRecurring().getSchedule(), active.getMetadata(goal).getRecurring().getTarget());
        form.setRecurring(repeat.getSchedule() != RepeatSchedule.NONE);
        repeat.addScheduleListener(() -> form.setRecurring(repeat.getSchedule() != RepeatSchedule.NONE));
        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.add(form, BorderLayout.CENTER);
        editor.add(repeat, BorderLayout.SOUTH);
        while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), editor, "Edit " + title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                repeat.commit();
                RepeatSchedule schedule = repeat.getSchedule();
                long periodTarget = repeat.getTarget();
                CustomMilestone draft = form.read(schedule == RepeatSchedule.NONE ? 0 : periodTarget);
                if (!confirmEdit(title, goal, draft, active.getMetadata(goal).getRecurring().getSchedule(), schedule, active.getMetadata(goal).getRecurring().getTarget(), periodTarget)) {
                    return;
                }
                clientThread.invokeLater(() -> {
                    if (profile == active && active.getGoals().contains(goal)) {
                        if (openedSchedule != RepeatSchedule.NONE && openedSchedule.getPeriodStart(System.currentTimeMillis()) != openedPeriod) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "The period reset while you were editing. Reopen the editor to avoid applying the previous period's progress."));
                            render();
                            return;
                        }
                        Milestone collision = active.findMatchingGoal(goal, schedule);
                        if (collision != null && collision != goal) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "That personal milestone already has this schedule."));
                            return;
                        }
                        Consumer<Milestone> restore = active.snapshotEdit(goal);
                        goal.update(draft.getTitle(null), draft.getDescription(), draft.getCategory(), draft.getProgressCurrent(), draft.getProgressMaximum());
                        configureRepeat(active, goal, schedule, periodTarget);
                        active.touch();
                        recordGoalUndo(active, goal, "edit", () -> restore.accept(goal));
                        render();
                    }
                });
                return;
            } catch (ParseException | IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), exception instanceof IllegalArgumentException ? exception.getMessage() : "Enter valid progress and target numbers.", "Check milestone details", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean confirmEdit(String title, Milestone original, Milestone replacement, RepeatSchedule before, RepeatSchedule after, long previousTarget, long nextTarget) {
        String summary = title + " → " + replacement.getTitle(null) + "\nTarget: " + String.format(Locale.ROOT, "%,d → %,d", original.getProgressMaximum(), replacement.getProgressMaximum()) + "\nRepeat: " + before + " → " + after + "\nPeriod target: " + String.format(Locale.ROOT, "%,d → %,d", previousTarget, nextTarget) + "\n\nChanged subjects retain old observations in account history.\nChanged repeat settings archive the current observed period.\nUndo expires after a later edit or period reset.";
        JTextArea preview = new JTextArea(summary, 10, 38);
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setFont(UiTypography.BODY);
        return JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), new JScrollPane(preview), "Preview milestone changes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void deleteGoal(GrindstoneProfile active, Milestone goal, String title) {
        if (profile != active || JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), "Remove " + title + " from your milestones?\nYour game progress is unaffected.", "Delete milestone", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        clientThread.invokeLater(() -> {
            if (profile == active && active.remove(goal)) {
                recordUndo(active, "delete", () -> active.add(goal, active.getMetadata(goal)), () -> active.findMatchingGoal(goal, active.getMetadata(goal).getRecurring().getSchedule()) == null);
                render();
            }
        });
    }

    private void undoDelete() {
        undoDeleteButton.setVisible(false);
        clientThread.invokeLater(() -> {
            if (!undo.undo(profile)) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Undo expired after a later edit, profile change or period reset."));
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

    private void recordGoalUndo(GrindstoneProfile active, Milestone goal, String label, Runnable action) {
        RepeatSchedule schedule = active.getMetadata(goal).getRecurring().getSchedule();
        long period = schedule.getPeriodStart(System.currentTimeMillis());
        recordUndo(active, label, action, () -> active.getGoals().contains(goal) && (schedule == RepeatSchedule.NONE || schedule.getPeriodStart(System.currentTimeMillis()) == period));
    }

    private void promptManualProgress(GrindstoneProfile active, CustomMilestone goal, long current, long target) {
        RepeatSchedule schedule = active.getMetadata(goal).getRecurring().getSchedule();
        long window = schedule.getPeriodStart(System.currentTimeMillis());
        JSpinner value = new JSpinner(new SpinnerNumberModel(current, 0L, target, 1L));
        value.setEditor(new JSpinner.NumberEditor(value, "#,##0"));
        while (JOptionPane.showConfirmDialog(DialogPlacement.getOwner(this), value, "Set progress (0–" + String.format(Locale.ROOT, "%,d", target) + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                value.commitEdit();
                long requested = ((Number) value.getValue()).longValue();
                changeManualProgress(active, goal, previous -> {
                    if (schedule != RepeatSchedule.NONE && schedule.getPeriodStart(System.currentTimeMillis()) != window) {
                        throw new IllegalStateException("The period reset while this dialog was open. Reopen Set progress for the new period.");
                    }
                    return requested;
                });
                return;
            } catch (ParseException exception) {
                JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Enter a whole number within the target.");
            }
        }
    }

    private void changeManualProgress(GrindstoneProfile active, CustomMilestone goal, LongUnaryOperator change) {
        clientThread.invokeLater(() -> {
            if (profile != active || !active.getGoals().contains(goal)) {
                return;
            }
            long now = System.currentTimeMillis();
            RecurringMilestone recurring = active.getMetadata(goal).getRecurring();
            if (recurring.isActive()) {
                recurring.observe(goal, now);
            }
            long requested;
            try {
                requested = change.applyAsLong(goal.getProgressCurrent());
            } catch (IllegalStateException exception) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), exception.getMessage()));
                render();
                return;
            }
            Consumer<Milestone> restore = active.snapshotEdit(goal);
            goal.update(goal.getTitle(null), goal.getDescription(), goal.getCategory(), Math.min(requested, goal.getProgressMaximum()), goal.getProgressMaximum());
            if (recurring.isActive()) {
                recurring.observe(goal, now);
            } else {
                active.getMetadata(goal).observe(goal, goal.getTitle(null), now);
            }
            active.touch();
            recordGoalUndo(active, goal, "progress", () -> restore.accept(goal));
            render();
        });
    }

    private String getSafeTitle(Milestone goal) {
        String title = goal.getTitle(client);
        return title == null ? "" : title;
    }

    private MilestoneCategory getSafeCategory(Milestone goal) {
        if (profile != null && profile.getMetadata(goal).getDisplayCategory() != null) {
            return profile.getMetadata(goal).getDisplayCategory();
        }
        MilestoneCategory category = goal.getCategory();
        return category == null ? MilestoneCategory.OTHER : category;
    }

    private static boolean isComplete(MilestoneStatus status) {
        return status != null && ("COMPLETE".equalsIgnoreCase(status.name()) || "COMPLETED".equalsIgnoreCase(status.name()));
    }

    private boolean isCollectionLogGoal(Milestone goal) {
        return goal.getCategory() == MilestoneCategory.COLLECTION_LOG;
    }

    private boolean isLevelGoal(Milestone goal) {
        return goal instanceof SkillLevelMilestone || goal instanceof CombatLevelMilestone || goal instanceof TotalLevelMilestone || goal instanceof BaseLevelMilestone;
    }

    private boolean isExperienceGoal(Milestone goal) {
        return goal instanceof SkillExperienceMilestone || goal instanceof TotalExperienceMilestone || goal instanceof BaseExperienceMilestone || goal instanceof SessionExperienceMilestone;
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
    public void refreshGoal(Milestone goal) {
        if (goal == null) {
            return;
        }

        cardCache.invalidate(goal);
        render();
    }

    public void refreshCategory(MilestoneCategory category) {
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
    private static final class MilestoneView {
        private final Milestone goal;
        private final MilestoneComponent component;
        private final String title;
        private final MilestoneCategory category;
        private final MilestoneStatus status;
        private final boolean hasProgress;
        private final long progressCurrent;
        private final long progressMaximum;

        private MilestoneView(Milestone goal, MilestoneComponent component, String title, MilestoneCategory category, MilestoneStatus status, boolean hasProgress, long progressCurrent, long progressMaximum) {
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
