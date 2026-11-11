package com.grindstone.ui;

import static com.grindstone.ui.MilestoneFilters.ALL_CATEGORIES;
import static com.grindstone.ui.MilestoneFilters.ALL_SCHEDULES;
import static com.grindstone.ui.MilestoneFilters.ALL_STATUSES;
import static com.grindstone.ui.MilestoneFilters.ALL_TYPES;
import static com.grindstone.ui.MilestoneFilters.AUTOMATIC;
import static com.grindstone.ui.MilestoneFilters.BASE;
import static com.grindstone.ui.MilestoneFilters.COMPLETE;
import static com.grindstone.ui.MilestoneFilters.CUSTOM;
import static com.grindstone.ui.MilestoneFilters.DEADLINES;
import static com.grindstone.ui.MilestoneFilters.IN_PROGRESS;
import static com.grindstone.ui.MilestoneFilters.LINKED;
import static com.grindstone.ui.MilestoneFilters.NOT_STARTED;
import static com.grindstone.ui.MilestoneFilters.ONE_OFF;
import static com.grindstone.ui.MilestoneFilters.PAST_DUE;
import static com.grindstone.ui.MilestoneFilters.SESSION;
import static com.grindstone.ui.MilestoneFilters.SORT_CLOSEST;
import static com.grindstone.ui.MilestoneFilters.SORT_DEADLINE;
import static com.grindstone.ui.MilestoneFilters.SORT_NAME;
import static com.grindstone.ui.MilestoneFilters.SORT_NAME_DESC;
import static com.grindstone.ui.MilestoneFilters.SORT_PROGRESS_ASC;
import static com.grindstone.ui.MilestoneFilters.SORT_PROGRESS_DESC;
import static com.grindstone.ui.MilestoneFilters.SORT_STATUS;
import static com.grindstone.ui.MilestoneFilters.UNFINISHED;
import static com.grindstone.ui.MilestoneFilters.WAITING;
import static com.grindstone.ui.MilestoneFilters.isComplete;
import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.notification.MilestoneCompletionNotifications;
import com.grindstone.notification.MilestoneCompletionOverlay;
import com.grindstone.overlay.GrindstoneProgressOverlay;
import com.grindstone.overlay.ProgressOverlayEntry;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.repository.SidebarViewState;
import com.grindstone.ui.components.SegmentedProgressBar;
import com.grindstone.ui.components.SnapshotStatusPanel;
import com.grindstone.ui.item.ItemSearchService;
import com.grindstone.ui.util.DialogPlacement;
import com.grindstone.ui.util.UiTooltips;
import com.grindstone.ui.util.UiTypography;
import com.grindstone.util.HtmlUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
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
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.FlatTextField;
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

    private static final Color PANEL_COLOR = ColorScheme.DARK_GRAY_COLOR;
    private static final Color HEADER_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color INPUT_COLOR = ColorScheme.CONTROL_COLOR;
    private static final Color TEXT_COLOR = ColorScheme.TEXT_COLOR;
    private static final Color MUTED_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
    private static final ImageIcon DATA_COLLAPSED_ICON = new ImageIcon(ImageUtil.loadImageResource(GrindstonePanel.class, "/util/arrow_right.png"));
    private static final ImageIcon DATA_EXPANDED_ICON = new ImageIcon(ImageUtil.rotateImage((BufferedImage) DATA_COLLAPSED_ICON.getImage(), Math.PI / 2));

    private final MilestoneActionSupport actionSupport;
    private final MilestoneCreationActions creationActions;
    private final MilestoneEditActions editActions;
    private final BulkMilestoneActions bulkActions;
    private final MilestoneCardPresenter cardPresenter;
    private final MilestoneListRenderer listRenderer;

    private final Client client;
    private final ClientThread clientThread;
    private final GrindstoneConfig config;

    @Inject
    private MilestoneCompletionOverlay completionOverlay;

    private final JPanel header = new JPanel();
    private final JPanel controlsPanel = new JPanel();
    private final JPanel dashboardOverview = new JPanel();
    private final SnapshotStatusPanel snapshots = new SnapshotStatusPanel();
    private final JLabel summaryLabel = new JLabel("No profile loaded");
    private final JLabel updateStatusLabel = new JLabel();
    private final JButton recoveryButton = new JButton("Recover / resolve sync...");
    private Runnable recoveryAction;

    public void setRecoveryAction(Runnable action) {
        SwingUtilities.invokeLater(() -> {
            recoveryAction = action;
            recoveryButton.setVisible(action != null);
        });
    }
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
    private final Map<String, Integer> statusCounts = new LinkedHashMap<>();
    private final JButton filtersToggle = new JButton("Filters");
    private final JButton clearFiltersButton = new JButton("Clear");
    private final JToggleButton todayButton = new JToggleButton("Routines");
    private final JToggleButton allMilestonesButton = new JToggleButton("All", true);
    private final JMenuItem dashboardToggle = new JMenuItem("Game data");
    private JScrollPane filterScroll;
    private SidebarViewControls viewControls;
    private GrindstoneProfile viewProfile;
    private boolean todayMode;

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
    public GrindstonePanel(Client client, ClientThread clientThread, GrindstoneConfig config, ItemManager itemManager, ItemSearchService itemSearchService, @Named("developerMode") boolean developerMode) {
        super(false);

        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
        actionSupport = new MilestoneActionSupport(this, client, clientThread, () -> profile, this::render);
        editActions = new MilestoneEditActions(actionSupport, itemManager, itemSearchService);
        creationActions = new MilestoneCreationActions(actionSupport, itemManager, itemSearchService, category -> {
            collapsedCategories.put(category, false);
            clearFiltersButton.doClick();
        });
        bulkActions = new BulkMilestoneActions(actionSupport);
        cardPresenter = new MilestoneCardPresenter(actionSupport, config, editActions);
        listRenderer = new MilestoneListRenderer(config, collapsedCategories, collapsedRepeats, this::render);
        filterRenderTimer.setRepeats(false);

        setLayout(new BorderLayout());
        setBackground(PANEL_COLOR);
        setOpaque(true);

        buildHeader(developerMode, itemManager);
        buildControls();

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(PANEL_COLOR);
        topPanel.setOpaque(true);
        topPanel.add(header);
        topPanel.add(controlsPanel);
        add(topPanel, BorderLayout.NORTH);

        add(listRenderer, BorderLayout.CENTER);

        viewControls = new SidebarViewControls(searchField, Map.of("category", categoryFilter, "status", statusFilter, "sort", sortFilter, "type", typeFilter, "schedule", scheduleFilter), pinnedOnly, filterScroll, dashboardOverview, collapsedCategories, collapsedRepeats);
        installListeners();
        render();
    }

    private void buildHeader(boolean developerMode, ItemManager itemManager) {
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
        overallProgress.setPreferredSize(new Dimension(100, 5));
        overallProgress.setMinimumSize(new Dimension(0, 5));
        overallProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        overallProgress.setAlignmentX(LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setIcon(new ImageIcon(Icon.MAIN.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
        titleRow.add(titleLabel, BorderLayout.CENTER);
        JButton add = new JButton("+ Add milestone");
        styleButton(add);
        add.setToolTipText("Track game progress or create a personal milestone.");
        add.getAccessibleContext().setAccessibleName("Add milestone");
        add.addActionListener(event -> creationActions.showAddMilestoneDialog());
        add.setAlignmentX(LEFT_ALIGNMENT);
        add.setMargin(new Insets(6, 8, 6, 8));
        add.setMaximumSize(new Dimension(Integer.MAX_VALUE, add.getPreferredSize().height));
        JButton more = new JButton("...");
        styleButton(more);
        more.setToolTipText("More milestone options");
        more.putClientProperty("JButton.buttonType", "toolBarButton");
        JPopupMenu moreMenu = SidebarMenu.create(creationActions, editActions, bulkActions, () -> profile, dashboardToggle, developerMode, () -> clientThread.invokeLater(() -> completionOverlay.preview(itemManager.getImage(ItemID.OCELLUSPET))), this::transferBackup, this::setAllCategoriesCollapsed, () -> MilestoneHistoryActions.showAccountHistory(actionSupport, this::revealMilestone));
        more.addActionListener(event -> moreMenu.show(more, 0, more.getHeight()));
        dashboardToggle.addActionListener(event -> {
            dashboardOverview.setVisible(!dashboardOverview.isVisible());
            updateDashboardToggle();
            saveView();
            revalidate();
            repaint();
        });
        titleRow.add(more, BorderLayout.EAST);
        header.add(titleRow);
        dashboardOverview.setOpaque(false);
        dashboardOverview.setLayout(new BoxLayout(dashboardOverview, BoxLayout.Y_AXIS));
        dashboardOverview.setAlignmentX(LEFT_ALIGNMENT);
        dashboardOverview.setVisible(false);
        updateDashboardToggle();
        header.add(Box.createVerticalStrut(4));
        summaryLabel.setFont(UiTypography.META);
        header.add(summaryLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(overallProgress);
        updateStatusLabel.setFont(UiTypography.META);
        updateStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
        updateStatusLabel.setToolTipText("Save status. Open Game data for storage and collection-log updates.");
        updateStatusLabel.setVisible(false);
        catchUpButton.setAlignmentX(LEFT_ALIGNMENT);
        catchUpButton.setFont(UiTypography.META);
        catchUpButton.setVisible(false);
        catchUpButton.setToolTipText("Review progress since your previous visit.");
        catchUpButton.addActionListener(event -> {
            JTextArea details = new JTextArea(catchUpText, 12, 36);
            details.setEditable(false);
            details.setLineWrap(true);
            details.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), new JScrollPane(details), "Since last seen", JOptionPane.INFORMATION_MESSAGE);
        });
        snapshots.setAlignmentX(LEFT_ALIGNMENT);
        dashboardOverview.add(snapshots);
        header.add(dashboardOverview);
        header.add(updateStatusLabel);
        header.add(catchUpButton);
        recoveryButton.setVisible(false);
        recoveryButton.setAlignmentX(LEFT_ALIGNMENT);
        recoveryButton.addActionListener(event -> {
            if (recoveryAction != null) {
                recoveryAction.run();
            }
        });
        header.add(recoveryButton);

        JPanel navigation = new JPanel(new GridLayout(1, 2, 4, 0));
        navigation.setOpaque(false);
        navigation.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup views = new ButtonGroup();
        views.add(todayButton);
        views.add(allMilestonesButton);
        todayButton.setToolTipText("Show routines ordered by next reset.");
        allMilestonesButton.setToolTipText("Show all milestones and routines.");
        todayButton.addActionListener(event -> {
            todayMode = true;
            render();
        });
        allMilestonesButton.addActionListener(event -> {
            todayMode = false;
            render();
        });
        for (AbstractButton button : new AbstractButton[]{allMilestonesButton, todayButton}) {
            button.setFont(UiTypography.BODY);
            button.setMargin(new Insets(5, 3, 5, 3));
            navigation.add(button);
        }
        header.add(Box.createVerticalStrut(10));
        header.add(navigation);
        header.add(Box.createVerticalStrut(8));
        header.add(add);
    }

    private void updateDashboardToggle() {
        boolean expanded = dashboardOverview.isVisible();
        dashboardToggle.setText(expanded ? "Hide game data" : "Game data");
        dashboardToggle.setIcon(expanded ? DATA_EXPANDED_ICON : DATA_COLLAPSED_ICON);
        dashboardToggle.setToolTipText(expanded ? "Hide captured game data" : "Show game data and refresh tips");
        dashboardToggle.getAccessibleContext().setAccessibleName(expanded ? "Hide game data" : "Show game data");
    }

    private JPanel createFilterField(String title, JComboBox<String> control) {
        JPanel field = new JPanel(new BorderLayout());
        field.setOpaque(false);
        control.getAccessibleContext().setAccessibleName(title);
        control.setRenderer(new SidebarFilterRenderer(title, Map.of()));
        control.setPreferredSize(new Dimension(200, 28));
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
        UiTooltips.set(searchField, "Search names, notes and categories. All search words must match.");
        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setAlignmentX(LEFT_ALIGNMENT);
        for (Component component : searchField.getComponents()) {
            if (component instanceof FlatTextField) {
                JTextField textField = ((FlatTextField) component).getTextField();
                textField.setFont(UiTypography.BODY);
                textField.putClientProperty("JTextField.placeholderText", "Search milestones");
                textField.getAccessibleContext().setAccessibleName("Search milestones");
            }
        }

        categoryFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_CATEGORIES}));
        statusFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_STATUSES, UNFINISHED, NOT_STARTED, IN_PROGRESS, WAITING, COMPLETE}));
        scheduleFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_SCHEDULES, ONE_OFF, "Daily", "Weekly", "Monthly"}));
        styleComboBox(scheduleFilter);
        scheduleFilter.setAlignmentX(LEFT_ALIGNMENT);
        scheduleFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        UiTooltips.set(scheduleFilter, "Filter by one-time, daily, weekly or monthly.");
        pinnedOnly.setOpaque(false);
        pinnedOnly.setFont(UiTypography.BODY);
        pinnedOnly.setAlignmentX(LEFT_ALIGNMENT);
        pinnedOnly.setToolTipText("Show pinned milestones only.");
        sortFilter.setModel(new DefaultComboBoxModel<>(new String[]{SORT_NAME, SORT_NAME_DESC, SORT_CLOSEST, SORT_DEADLINE, SORT_PROGRESS_DESC, SORT_PROGRESS_ASC, SORT_STATUS}));
        typeFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_TYPES, AUTOMATIC, CUSTOM, LINKED, BASE, SESSION, DEADLINES, PAST_DUE}));
        styleComboBox(typeFilter);
        typeFilter.setAlignmentX(LEFT_ALIGNMENT);
        typeFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        styleComboBox(categoryFilter);
        styleComboBox(statusFilter);
        statusFilter.setRenderer(new SidebarFilterRenderer("Status", statusCounts));
        statusFilter.getAccessibleContext().setAccessibleName("Milestone status");
        styleComboBox(sortFilter);
        UiTooltips.set(categoryFilter, "Filter by category.");
        UiTooltips.set(statusFilter, "Done includes completed routines hidden by settings.");
        UiTooltips.set(sortFilter, "Sort within each section. Deadline (soonest) puts unfinished deadlines first.");
        UiTooltips.set(typeFilter, "Filter by milestone type. Groups combine existing milestones.");

        categoryFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        statusFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        sortFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        categoryFilter.setAlignmentX(LEFT_ALIGNMENT);
        statusFilter.setAlignmentX(LEFT_ALIGNMENT);
        sortFilter.setAlignmentX(LEFT_ALIGNMENT);

        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(LEFT_ALIGNMENT);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(filtersToggle, BorderLayout.EAST);
        controlsPanel.add(searchRow);
        controlsPanel.add(Box.createVerticalStrut(6));

        JPanel statusRow = new JPanel(new BorderLayout(6, 0));
        statusRow.setOpaque(false);
        statusRow.setAlignmentX(LEFT_ALIGNMENT);
        statusRow.setPreferredSize(new Dimension(200, 28));
        statusRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        statusRow.add(statusFilter, BorderLayout.CENTER);
        styleButton(clearFiltersButton);
        clearFiltersButton.setText("Reset");
        clearFiltersButton.setToolTipText("Reset search, filters and sorting.");
        clearFiltersButton.setVisible(false);
        statusRow.add(clearFiltersButton, BorderLayout.EAST);
        controlsPanel.add(statusRow);

        JPanel filterOptions = new JPanel();
        filterOptions.setLayout(new BoxLayout(filterOptions, BoxLayout.Y_AXIS));
        filterOptions.setOpaque(false);
        filterOptions.setAlignmentX(LEFT_ALIGNMENT);
        filterOptions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JPanel selectors = new JPanel(new GridLayout(4, 1, 0, 6));
        selectors.setOpaque(false);
        selectors.setAlignmentX(LEFT_ALIGNMENT);
        selectors.setPreferredSize(new Dimension(200, 130));
        selectors.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        selectors.add(createFilterField("Category", categoryFilter));
        selectors.add(createFilterField("Type", typeFilter));
        selectors.add(createFilterField("Schedule", scheduleFilter));
        selectors.add(createFilterField("Sort", sortFilter));
        filterOptions.add(selectors);
        filterOptions.add(Box.createVerticalStrut(8));
        filterOptions.add(pinnedOnly);
        filterScroll = new JScrollPane(filterOptions);
        filterScroll.setBorder(BorderFactory.createEmptyBorder());
        filterScroll.setOpaque(false);
        filterScroll.getViewport().setOpaque(false);
        filterScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        filterScroll.getVerticalScrollBar().setUnitIncrement(16);
        filterScroll.setAlignmentX(LEFT_ALIGNMENT);
        filterScroll.setPreferredSize(new Dimension(200, 176));
        filterScroll.setMinimumSize(new Dimension(0, 100));
        filterScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 176));
        filterScroll.setVisible(false);
        styleButton(filtersToggle);
        filtersToggle.setToolTipText("Filter by category, type, schedule or pins. The count includes active search and filters.");
        filtersToggle.setAlignmentX(LEFT_ALIGNMENT);
        filtersToggle.addActionListener(event -> {
            boolean show = !filterScroll.isVisible();
            filterScroll.setVisible(show);
            filtersToggle.setToolTipText(show ? "Hide filter options" : "Show filter options");
            saveView();
            revalidate();
            repaint();
        });
        controlsPanel.add(filterScroll);

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

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(UiTypography.BODY);
        comboBox.setRenderer(new SidebarFilterRenderer("", Map.of()));
        comboBox.setFocusable(true);
        comboBox.setMaximumRowCount(8);
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
            requestFilteredRender();
        }
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
        cardPresenter.clear();
        listRenderer.clearPendingReveal();
        backupActions.cancel();
        this.profile = profile;
        SidebarViewState savedView = profile.getSidebarView();
        SwingUtilities.invokeLater(() -> {
            if (this.profile != profile) {
                return;
            }
            updatingFilters = true;
            try {
                viewControls.apply(savedView);
                todayMode = savedView.isRoutines();
                todayButton.setSelected(todayMode);
                allMilestonesButton.setSelected(!todayMode);
                updateDashboardToggle();
                filtersToggle.setToolTipText(savedView.isFiltersExpanded() ? "Hide filter options" : "Show filter options");
                viewProfile = profile;
            } finally {
                updatingFilters = false;
            }
        });
        renderVersion.incrementAndGet();
        render();
    }

    public void unload() {
        setRecoveryAction(null);
        if (progressOverlay != null) {
            progressOverlay.clear();
        }
        cardPresenter.clear();
        listRenderer.clearPendingReveal();
        backupActions.cancel();
        profile = null;
        renderVersion.incrementAndGet();
        render();
    }

    private void saveView() {
        GrindstoneProfile active = profile;
        if (!SwingUtilities.isEventDispatchThread() || updatingFilters || viewControls == null || active == null || active != viewProfile) {
            return;
        }
        SidebarViewState state = viewControls.capture(todayMode);
        clientThread.invokeLater(() -> {
            if (profile == active) {
                active.setSidebarView(state);
            }
        });
    }

    public void render() {
        saveView();
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
            long now = System.currentTimeMillis();

            if (currentProfile != null) {
                if (loggedIn) {
                    currentProfile.observeRecurring(client, now);
                    completionNotifications.observe(currentProfile);
                }
                preparedViews.addAll(cardPresenter.prepare(currentProfile, loggedIn, now, captureOverlay, overlayEntries));
            }

            if (progressOverlay != null) {
                if (captureOverlay) {
                    progressOverlay.update(overlayEntries, client.getCanvasHeight());
                } else {
                    progressOverlay.clear();
                }
            }
            SidebarStatusSnapshot status = SidebarStatusSnapshot.capture(client, currentProfile, config.showDeadlineReminders());
            SwingUtilities.invokeLater(() -> {
                if (version != renderVersion.get()) {
                    return;
                }

                snapshots.updateSnapshots(status.getStatuses(), status.getTips(), status.getMissing());
                snapshots.setVisible(config.showUpdateStatus() && currentProfile != null);
                dashboardToggle.setVisible(snapshots.isVisible());
                catchUpText = status.getCatchUpText();
                catchUpButton.setText("Since last seen - " + status.getCatchUpCount() + " updated");
                catchUpButton.setVisible(status.getCatchUpCount() > 0);
                updateStatusLabel.setText("<html>" + (storageStatus == null ? "" : HtmlUtils.escape(storageStatus) + "<br>") + (status.getDueCount() == 0 ? "" : status.getDueCount() + " personal deadline(s) due within 24h / past due<br>") + "</html>");
                updateStatusLabel.setVisible(storageStatus != null || status.getDueCount() > 0);
                updateStatusLabel.setToolTipText("Save status. Open Game data for storage and collection-log updates.");
                List<MilestoneView> allGoals = new ArrayList<>(preparedViews.size());
                for (Supplier<MilestoneView> prepared : preparedViews) {
                    allGoals.add(prepared.get());
                }
                renderPrepared(currentProfile, allGoals);
            });
        });
    }

    private void updateSummary(List<MilestoneView> goals) {
        int completed = 0;
        int active = 0;
        int notStarted = 0;
        int known = 0;

        for (MilestoneView view : goals) {
            if (!Boolean.TRUE.equals(view.getComponent().getClientProperty("grindstone.ready"))) {
                continue;
            }
            if (isComplete(view.getStatus())) {
                completed++;
            } else if (view.getStatus() == MilestoneStatus.IN_PROGRESS) {
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
        summaryLabel.setText(goals.isEmpty() ? "No milestones yet" : completed + " / " + goals.size() + " complete");
        summaryLabel.setToolTipText(percentage + "% complete across all enabled milestones, independent of search and filters.");
        overallProgress.setToolTipText(summaryLabel.getToolTipText());
        statusCounts.put(ALL_STATUSES, goals.size());
        statusCounts.put(UNFINISHED, goals.size() - completed);
        statusCounts.put(NOT_STARTED, notStarted);
        statusCounts.put(IN_PROGRESS, active);
        statusCounts.put(COMPLETE, completed);
        statusCounts.put(WAITING, goals.size() - known);
        statusFilter.repaint();
    }

    private void updateCategoryOptions(List<MilestoneView> goals) {
        String previous = (String) categoryFilter.getSelectedItem();
        Set<String> categories = new LinkedHashSet<>();
        categories.add(ALL_CATEGORIES);

        for (MilestoneView view : goals) {
            categories.add(view.getCategory().getDisplayName());
        }

        boolean unchanged = categoryFilter.getItemCount() == categories.size();
        int index = 0;
        for (String category : categories) {
            if (!unchanged || !category.equals(categoryFilter.getItemAt(index++))) {
                unchanged = false;
                break;
            }
        }
        if (unchanged) {
            return;
        }

        updatingFilters = true;
        try {
            categoryFilter.setModel(new DefaultComboBoxModel<>(categories.toArray(new String[0])));
            categoryFilter.setSelectedItem(previous != null && categories.contains(previous) ? previous : ALL_CATEGORIES);
        } finally {
            updatingFilters = false;
        }
    }

    private void transferBackup(boolean importing) {
        transferBackup(importing, false);
    }

    private void transferBackup(boolean importing, boolean sharing) {
        GrindstoneProfile active = profile;
        backupActions.show(this, active, () -> profile == active, () -> {
            if (profile == active) {
                for (Milestone goal : active.getGoals()) {
                    goal.refresh(client);
                }
                render();
            }
        }, importing, sharing);
    }

    private void revealMilestone(GrindstoneProfile active, String id) {
        clientThread.invokeLater(() -> {
            if (profile != active) {
                return;
            }
            Milestone goal = active.getGoals().stream().filter(value -> active.getMetadata(value).getId().equals(id)).findFirst().orElse(null);
            if (goal == null) {
                return;
            }
            String title = actionSupport.getTitle(goal);
            boolean complete = active.getMetadata(goal).getRecurring().isActive() ? active.getMetadata(goal).getRecurring().getStatus() == MilestoneStatus.COMPLETE : goal.getStatus() == MilestoneStatus.COMPLETE;
            SwingUtilities.invokeLater(() -> {
                if (profile != active) {
                    return;
                }
                todayMode = false;
                allMilestonesButton.setSelected(true);
                clearFiltersButton.doClick();
                searchField.setText(title);
                statusFilter.setSelectedItem(complete ? COMPLETE : UNFINISHED);
                listRenderer.reveal(id);
                setAllCategoriesCollapsed(false);
            });
        });
    }

    public void refreshMilestone(Milestone goal) {
        if (goal == null) {
            return;
        }

        cardPresenter.invalidate(goal);
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

    private void updateFilterSummary() {
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
        clearFiltersButton.setVisible(filterCount > 0);
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
        String selectionText = String.join(" · ", selections);
        filtersToggle.setToolTipText(selections.isEmpty() ? "Show or hide additional filter options" : selectionText);

    }

    private void renderPrepared(GrindstoneProfile currentProfile, List<MilestoneView> allGoals) {
        if (currentProfile != null) {
            updateCategoryOptions(allGoals);
            updateSummary(allGoals);
            updateFilterSummary();
        } else {
            updateSummary(new ArrayList<>());
        }
        listRenderer.render(currentProfile, allGoals, viewControls.capture(todayMode), searchField.getText());
        resultsLabel.setText(currentProfile == null ? "0 tracked" : "Showing " + listRenderer.getDisplayedCount() + " of " + allGoals.size());
        resultsLabel.setToolTipText("Milestones matching your filters.");
    }

    public void showAddMilestoneDialog(Milestone initialGoal) {
        creationActions.showAddMilestoneDialog(initialGoal);
    }
}
