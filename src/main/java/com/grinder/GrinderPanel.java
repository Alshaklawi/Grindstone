package com.grinder;

import com.grinder.config.GrinderConfig;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogItemGoal;
import com.grinder.repository.GrinderProfile;
import com.grinder.ui.GoalComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
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
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class GrinderPanel extends PluginPanel
{
    private static final String ALL_CATEGORIES = "All categories";
    private static final String ALL_STATUSES = "All statuses";
    private static final String NOT_STARTED = "Not started";
    private static final String IN_PROGRESS = "In progress";
    private static final String COMPLETE = "Complete";
    private static final String SORT_NAME = "Name (A-Z)";
    private static final String SORT_PROGRESS_DESC = "Progress (highest)";
    private static final String SORT_PROGRESS_ASC = "Progress (lowest)";
    private static final String SORT_STATUS = "Status";

    private static final Color PANEL_COLOR = ColorScheme.DARK_GRAY_COLOR;
    private static final Color HEADER_COLOR = new Color(38, 38, 38);
    private static final Color CARD_COLOR = new Color(45, 45, 45);
    private static final Color INPUT_COLOR = new Color(34, 34, 34);
    private static final Color INPUT_HOVER_COLOR = new Color(48, 48, 48);
    private static final Color BORDER_COLOR = new Color(68, 68, 68);
    private static final Color BORDER_FOCUS_COLOR = new Color(92, 150, 115);
    private static final Color TEXT_COLOR = new Color(235, 235, 235);
    private static final Color MUTED_COLOR = new Color(190, 190, 190);
    private static final Color SCROLL_TRACK_COLOR = new Color(32, 32, 32);
    private static final Color SCROLL_THUMB_COLOR = new Color(85, 85, 85);
    private static final Color SCROLL_THUMB_HOVER_COLOR = new Color(115, 115, 115);
    private static final Color PROGRESS_COLOR = new Color(65, 160, 100);

    private static final Border INPUT_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 8, 4, 8));

    private final Client client;
    private final ClientThread clientThread;
    private final GrinderConfig config;

    private final JPanel main = new JPanel();
    private final JPanel header = new JPanel();
    private final JPanel controlsPanel = new JPanel();
    private final JLabel summaryLabel = new JLabel("No profile loaded");
    private final JLabel resultsLabel = new JLabel();
    private final JProgressBar overallProgress = new JProgressBar(0, 100);
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<String> sortFilter = new JComboBox<>();
    private final JButton clearFiltersButton = new JButton("Clear");
    private final JScrollPane scrollPane;

    private final Map<GoalCategory, Boolean> collapsedCategories = new LinkedHashMap<>();
    private final AtomicInteger renderVersion = new AtomicInteger();
    private final AtomicBoolean renderQueued = new AtomicBoolean();
    private final Timer filterRenderTimer = new Timer(150, event -> render());

    private volatile GrinderProfile profile;
    private boolean updatingFilters;

    @Inject
    public GrinderPanel(Client client, ClientThread clientThread, GrinderConfig config)
    {
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
        styleScrollBar(scrollPane.getVerticalScrollBar());
        add(scrollPane, BorderLayout.CENTER);

        installListeners();
        render();
    }

    private void buildHeader()
    {
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(HEADER_COLOR);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel("Grinder Goals");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        summaryLabel.setForeground(MUTED_COLOR);
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.PLAIN, 11f));
        summaryLabel.setAlignmentX(LEFT_ALIGNMENT);

        overallProgress.setStringPainted(false);
        overallProgress.setBorderPainted(false);
        overallProgress.setOpaque(true);
        overallProgress.setBackground(INPUT_COLOR);
        overallProgress.setForeground(PROGRESS_COLOR);
        overallProgress.setPreferredSize(new Dimension(100, 8));
        overallProgress.setMinimumSize(new Dimension(0, 8));
        overallProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        overallProgress.setAlignmentX(LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(summaryLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(overallProgress);
    }

    private void buildControls()
    {
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setBackground(PANEL_COLOR);
        controlsPanel.setOpaque(true);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));

        styleTextField(searchField);
        searchField.setToolTipText("Search goals by name");
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setAlignmentX(LEFT_ALIGNMENT);

        categoryFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_CATEGORIES}));
        statusFilter.setModel(new DefaultComboBoxModel<>(new String[]{ALL_STATUSES, NOT_STARTED, IN_PROGRESS, COMPLETE}));
        sortFilter.setModel(new DefaultComboBoxModel<>(new String[]{SORT_NAME, SORT_PROGRESS_DESC, SORT_PROGRESS_ASC, SORT_STATUS}));

        styleComboBox(categoryFilter);
        styleComboBox(statusFilter);
        styleComboBox(sortFilter);

        categoryFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        statusFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        sortFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        categoryFilter.setAlignmentX(LEFT_ALIGNMENT);
        statusFilter.setAlignmentX(LEFT_ALIGNMENT);
        sortFilter.setAlignmentX(LEFT_ALIGNMENT);

        controlsPanel.add(searchField);
        controlsPanel.add(Box.createVerticalStrut(7));
        controlsPanel.add(categoryFilter);
        controlsPanel.add(Box.createVerticalStrut(7));
        controlsPanel.add(statusFilter);
        controlsPanel.add(Box.createVerticalStrut(7));
        controlsPanel.add(sortFilter);

        JPanel actionRow = new JPanel(new BorderLayout(8, 0));
        actionRow.setBackground(PANEL_COLOR);
        actionRow.setOpaque(true);
        actionRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        actionRow.setAlignmentX(LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        resultsLabel.setForeground(MUTED_COLOR);
        resultsLabel.setFont(resultsLabel.getFont().deriveFont(Font.PLAIN, 11f));

        styleButton(clearFiltersButton);
        clearFiltersButton.setToolTipText("Clear search and filters");

        actionRow.add(resultsLabel, BorderLayout.CENTER);
        actionRow.add(clearFiltersButton, BorderLayout.EAST);
        controlsPanel.add(actionRow);
    }

    private void styleTextField(JTextField field)
    {
        field.setBackground(INPUT_COLOR);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setSelectionColor(new Color(65, 100, 80));
        field.setSelectedTextColor(Color.WHITE);
        field.setDisabledTextColor(MUTED_COLOR);
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 12f));
        field.setBorder(INPUT_BORDER);
        field.setOpaque(true);
    }

    private void styleComboBox(JComboBox<String> comboBox)
    {
        comboBox.setBackground(INPUT_COLOR);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setFont(comboBox.getFont().deriveFont(Font.PLAIN, 12f));
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        comboBox.setOpaque(true);
        comboBox.setFocusable(true);
        comboBox.setMaximumRowCount(12);
        comboBox.setLightWeightPopupEnabled(true);

        comboBox.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setOpaque(true);
                label.setFont(comboBox.getFont());
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

                if (isSelected)
                {
                    label.setBackground(new Color(65, 85, 72));
                    label.setForeground(Color.WHITE);
                }
                else
                {
                    label.setBackground(INPUT_COLOR);
                    label.setForeground(TEXT_COLOR);
                }

                return label;
            }
        });

        comboBox.setUI(new javax.swing.plaf.basic.BasicComboBoxUI()
        {
            @Override
            protected JButton createArrowButton()
            {
                JButton button = new JButton("\u25BE");
                button.setFont(button.getFont().deriveFont(Font.PLAIN, 12f));
                button.setForeground(MUTED_COLOR);
                button.setBackground(INPUT_COLOR);
                button.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                button.setFocusable(false);
                button.setOpaque(true);
                button.setContentAreaFilled(true);
                button.setBorderPainted(false);
                return button;
            }
        });

        comboBox.setBackground(INPUT_COLOR);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
    }

    private void styleButton(JButton button)
    {
        button.setForeground(TEXT_COLOR);
        button.setBackground(INPUT_HOVER_COLOR);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 11f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 9, 4, 9)));
        button.setFocusable(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
    }

    private void styleScrollBar(JScrollBar scrollBar)
    {
        scrollBar.setBorder(BorderFactory.createEmptyBorder());
        scrollBar.setOpaque(true);
        scrollBar.setBackground(SCROLL_TRACK_COLOR);
        scrollBar.setPreferredSize(new Dimension(10, 0));

        scrollBar.setUI(new BasicScrollBarUI()
        {
            @Override
            protected void configureScrollBarColors()
            {
                thumbColor = SCROLL_THUMB_COLOR;
                thumbHighlightColor = SCROLL_THUMB_HOVER_COLOR;
                thumbDarkShadowColor = SCROLL_THUMB_COLOR;
                thumbLightShadowColor = SCROLL_THUMB_COLOR;
                trackColor = SCROLL_TRACK_COLOR;
            }

            @Override
            protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds)
            {
                graphics.setColor(SCROLL_TRACK_COLOR);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds)
            {
                if (bounds.isEmpty() || !scrollbar.isEnabled())
                {
                    return;
                }

                Graphics2D g = (Graphics2D) graphics.create();
                try
                {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(isDragging || isThumbRollover() ? SCROLL_THUMB_HOVER_COLOR : SCROLL_THUMB_COLOR);
                    g.fillRoundRect(bounds.x + 2, bounds.y + 2, Math.max(1, bounds.width - 4), Math.max(1, bounds.height - 4), 8, 8);
                }
                finally
                {
                    g.dispose();
                }
            }

            @Override
            public Dimension getPreferredSize(JComponent component)
            {
                return new Dimension(10, 10);
            }

            @Override
            protected JButton createDecreaseButton(int orientation)
            {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation)
            {
                return createZeroButton();
            }

            private JButton createZeroButton()
            {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                button.setVisible(false);
                return button;
            }
        });
    }

    private void installListeners()
    {
        searchField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent event)
            {
                requestFilteredRender();
            }

            @Override
            public void removeUpdate(DocumentEvent event)
            {
                requestFilteredRender();
            }

            @Override
            public void changedUpdate(DocumentEvent event)
            {
                requestFilteredRender();
            }
        });

        categoryFilter.addActionListener(this::onFilterChanged);
        statusFilter.addActionListener(this::onFilterChanged);
        sortFilter.addActionListener(this::onFilterChanged);

        clearFiltersButton.addActionListener(event ->
        {
            searchField.setText("");
            categoryFilter.setSelectedItem(ALL_CATEGORIES);
            statusFilter.setSelectedItem(ALL_STATUSES);
            sortFilter.setSelectedItem(SORT_NAME);
            render();
        });
    }

    private void onFilterChanged(ActionEvent event)
    {
        if (!updatingFilters)
        {
            requestFilteredRender();
        }
    }

    private void requestFilteredRender()
    {
        filterRenderTimer.restart();
    }

    public void load(GrinderProfile profile)
    {
        this.profile = profile;
        renderVersion.incrementAndGet();
        render();
    }

    public void unload()
    {
        profile = null;
        renderVersion.incrementAndGet();
        render();
    }

    /**
     * Requests goal preparation on RuneLite's client thread.
     * Swing component updates are then scheduled on the EDT.
     */
    public void render()
    {
        if (!renderQueued.compareAndSet(false, true))
        {
            return;
        }

        clientThread.invokeLater(() ->
        {
            renderQueued.set(false);
            int version = renderVersion.incrementAndGet();
            GrinderProfile currentProfile = profile;
            List<GoalView> allGoals = new ArrayList<>();

            if (currentProfile != null && config.enabled())
            {
                for (Goal goal : currentProfile.getGoals())
                {
                    if (goal == null || (!config.collectionLogEnabled() && isCollectionLogGoal(goal)))
                    {
                        continue;
                    }

                    GoalComponent component = goal.build(client, config);

                    allGoals.add(new GoalView(
                            goal,
                            component,
                            safeTitle(goal),
                            safeCategory(goal),
                            goal.getStatus(),
                            goal.hasProgress(),
                            goal.getProgressCurrent(),
                            goal.getProgressMaximum()));
                }
            }

            SwingUtilities.invokeLater(() ->
            {
                if (version != renderVersion.get())
                {
                    return;
                }

                renderPrepared(currentProfile, allGoals);
            });
        });
    }

    private void renderPrepared(GrinderProfile currentProfile, List<GoalView> allGoals)
    {
        main.removeAll();

        if (currentProfile == null)
        {
            updateSummary(new ArrayList<>());
            resultsLabel.setText("0 goals");
            addMessage("No profile loaded", "Load a profile to view your goals.");
            refreshPanel();
            return;
        }

        if (!config.enabled())
        {
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

        if (filteredGoals.isEmpty())
        {
            addMessage("No matching goals", "Try another search term or clear your filters.");
            refreshPanel();
            return;
        }

        Map<GoalCategory, List<GoalView>> groupedGoals = groupGoals(filteredGoals);

        for (Map.Entry<GoalCategory, List<GoalView>> entry : groupedGoals.entrySet())
        {
            addCategorySection(entry.getKey(), entry.getValue());
            main.add(Box.createVerticalStrut(10));
        }

        refreshPanel();
    }

    private void updateSummary(List<GoalView> goals)
    {
        int completed = 0;
        int tracked = 0;
        double currentTotal = 0;
        double maximumTotal = 0;

        for (GoalView view : goals)
        {
            if (isComplete(view.status))
            {
                completed++;
            }

            if (view.hasProgress)
            {
                long maximum = Math.max(0, view.progressMaximum);

                if (maximum > 0)
                {
                    long current = Math.max(0, Math.min(view.progressCurrent, maximum));
                    currentTotal += current;
                    maximumTotal += maximum;
                    tracked++;
                }
            }
        }

        int percentage = maximumTotal <= 0 ? 0 : (int) Math.round(currentTotal * 100.0 / maximumTotal);
        overallProgress.setValue(Math.max(0, Math.min(100, percentage)));
        summaryLabel.setText(completed + " completed  •  " + goals.size() + " goals  •  " +
                (tracked == 0 ? "No tracked progress" : percentage + "% across " + tracked + " tracked goals"));
    }

    private List<GoalView> filterGoals(List<GoalView> goals)
    {
        List<GoalView> filtered = new ArrayList<>();
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();

        for (GoalView view : goals)
        {
            if (!query.isEmpty() && !view.title.toLowerCase(Locale.ROOT).contains(query))
            {
                continue;
            }

            if (selectedCategory != null && !ALL_CATEGORIES.equals(selectedCategory) &&
                    !selectedCategory.equals(view.category.getDisplayName()))
            {
                continue;
            }

            if (!matchesStatus(view.status, selectedStatus))
            {
                continue;
            }

            filtered.add(view);
        }

        return filtered;
    }

    private boolean matchesStatus(GoalStatus status, String selectedStatus)
    {
        if (selectedStatus == null || ALL_STATUSES.equals(selectedStatus))
        {
            return true;
        }

        String value = status == null ? "" : status.name();

        if (COMPLETE.equals(selectedStatus))
        {
            return "COMPLETE".equalsIgnoreCase(value) || "COMPLETED".equalsIgnoreCase(value);
        }

        if (IN_PROGRESS.equals(selectedStatus))
        {
            return "IN_PROGRESS".equalsIgnoreCase(value);
        }

        if (NOT_STARTED.equals(selectedStatus))
        {
            return "NOT_STARTED".equalsIgnoreCase(value);
        }

        return true;
    }

    private void sortGoals(List<GoalView> goals)
    {
        String sort = (String) sortFilter.getSelectedItem();

        if (SORT_PROGRESS_DESC.equals(sort))
        {
            goals.sort(Comparator.comparingDouble(GrinderPanel::getProgressRatio).reversed()
                    .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
        else if (SORT_PROGRESS_ASC.equals(sort))
        {
            goals.sort(Comparator.comparingDouble(GrinderPanel::getProgressRatio)
                    .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
        else if (SORT_STATUS.equals(sort))
        {
            goals.sort(Comparator.comparingInt((GoalView view) -> getStatusOrder(view.status))
                    .thenComparing(view -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
        else
        {
            goals.sort(Comparator.comparing((GoalView view) -> view.title, String.CASE_INSENSITIVE_ORDER));
        }
    }

    private static double getProgressRatio(GoalView view)
    {
        if (!view.hasProgress || view.progressMaximum <= 0)
        {
            return isComplete(view.status) ? 1.0 : 0.0;
        }

        long current = Math.max(0, Math.min(view.progressCurrent, view.progressMaximum));
        return current / (double) view.progressMaximum;
    }

    private static int getStatusOrder(GoalStatus status)
    {
        if (isComplete(status))
        {
            return 0;
        }

        if (status == GoalStatus.IN_PROGRESS)
        {
            return 1;
        }

        return 2;
    }

    private Map<GoalCategory, List<GoalView>> groupGoals(List<GoalView> goals)
    {
        Map<GoalCategory, List<GoalView>> grouped = new LinkedHashMap<>();

        for (GoalView view : goals)
        {
            grouped.computeIfAbsent(view.category, key -> new ArrayList<>()).add(view);
        }

        return grouped;
    }

    private void updateCategoryOptions(List<GoalView> goals)
    {
        String previous = (String) categoryFilter.getSelectedItem();
        Set<String> categories = new LinkedHashSet<>();
        categories.add(ALL_CATEGORIES);

        for (GoalView view : goals)
        {
            categories.add(view.category.getDisplayName());
        }

        updatingFilters = true;
        try
        {
            categoryFilter.setModel(new DefaultComboBoxModel<>(categories.toArray(new String[0])));
            categoryFilter.setSelectedItem(previous != null && categories.contains(previous) ? previous : ALL_CATEGORIES);
        }
        finally
        {
            updatingFilters = false;
        }
    }

    private void addCategorySection(GoalCategory category, List<GoalView> goals)
    {
        boolean collapsed = Boolean.TRUE.equals(collapsedCategories.get(category));

        JPanel categorySection = new JPanel();
        categorySection.setLayout(new BoxLayout(categorySection, BoxLayout.Y_AXIS));
        categorySection.setBackground(CARD_COLOR);
        categorySection.setOpaque(true);
        categorySection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(0, 0, 4, 0)));
        categorySection.setAlignmentX(LEFT_ALIGNMENT);
        categorySection.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel categoryHeader = new JPanel(new BorderLayout(8, 0));
        categoryHeader.setBackground(HEADER_COLOR);
        categoryHeader.setOpaque(true);
        categoryHeader.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
        categoryHeader.setAlignmentX(LEFT_ALIGNMENT);
        categoryHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JButton toggleButton = new JButton(collapsed ? "\u25B6" : "\u25BC");
        toggleButton.setToolTipText(collapsed ? "Expand category" : "Collapse category");
        toggleButton.setForeground(MUTED_COLOR);
        toggleButton.setBackground(HEADER_COLOR);
        toggleButton.setFont(toggleButton.getFont().deriveFont(Font.PLAIN, 11f));
        toggleButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        toggleButton.setFocusable(false);
        toggleButton.setOpaque(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setBorderPainted(false);
        toggleButton.addActionListener(event ->
        {
            collapsedCategories.put(category, !Boolean.TRUE.equals(collapsedCategories.get(category)));
            render();
        });

        JLabel iconLabel = new JLabel();
        if (category.getIcon() != null && category.getIcon().getImage() != null)
        {
            iconLabel.setIcon(new ImageIcon(category.getIcon().getImage().getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH)));
        }

        JLabel titleLabel = new JLabel(category.getDisplayName());
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
        titlePanel.setBackground(HEADER_COLOR);
        titlePanel.setOpaque(false);
        titlePanel.add(iconLabel);
        titlePanel.add(Box.createHorizontalStrut(7));
        titlePanel.add(titleLabel);

        JLabel countLabel = new JLabel(String.valueOf(goals.size()), SwingConstants.RIGHT);
        countLabel.setForeground(MUTED_COLOR);
        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 11f));
        countLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        JPanel headerLeft = new JPanel();
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.X_AXIS));
        headerLeft.setBackground(HEADER_COLOR);
        headerLeft.setOpaque(false);
        headerLeft.add(toggleButton);
        headerLeft.add(Box.createHorizontalStrut(5));
        headerLeft.add(titlePanel);

        categoryHeader.add(headerLeft, BorderLayout.CENTER);
        categoryHeader.add(countLabel, BorderLayout.EAST);
        categorySection.add(categoryHeader);

        if (!collapsed)
        {
            JPanel goalsPanel = new JPanel();
            goalsPanel.setLayout(new BoxLayout(goalsPanel, BoxLayout.Y_AXIS));
            goalsPanel.setBackground(CARD_COLOR);
            goalsPanel.setOpaque(true);
            goalsPanel.setBorder(BorderFactory.createEmptyBorder(8, 7, 4, 7));
            goalsPanel.setAlignmentX(LEFT_ALIGNMENT);

            for (GoalView view : goals)
            {
                view.component.setAlignmentX(LEFT_ALIGNMENT);
                view.component.setMaximumSize(new Dimension(Integer.MAX_VALUE, view.component.getPreferredSize().height));
                goalsPanel.add(view.component);
                goalsPanel.add(Box.createVerticalStrut(8));
            }

            categorySection.add(goalsPanel);
        }

        main.add(categorySection);
    }

    private void addMessage(String title, String message)
    {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setBackground(PANEL_COLOR);
        emptyPanel.setOpaque(true);
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(18, 4, 18, 4));
        emptyPanel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));

        JLabel messageLabel = new JLabel("<html><div style='width: 220px'>" + escapeHtml(message) + "</div></html>");
        messageLabel.setForeground(MUTED_COLOR);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 11f));

        emptyPanel.add(titleLabel);
        emptyPanel.add(Box.createVerticalStrut(6));
        emptyPanel.add(messageLabel);
        main.add(emptyPanel);
    }

    private String safeTitle(Goal goal)
    {
        String title = goal.getTitle(client);
        return title == null ? "" : title;
    }

    private GoalCategory safeCategory(Goal goal)
    {
        GoalCategory category = goal.getCategory();
        return category == null ? GoalCategory.OTHER : category;
    }

    private static boolean isComplete(GoalStatus status)
    {
        return status != null && ("COMPLETE".equalsIgnoreCase(status.name()) ||
                "COMPLETED".equalsIgnoreCase(status.name()));
    }

    private boolean isCollectionLogGoal(Goal goal)
    {
        return goal instanceof CollectionLogEntryGoal || goal instanceof CollectionLogItemGoal;
    }

    private String escapeHtml(String value)
    {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void refreshPanel()
    {
        main.revalidate();
        main.repaint();
        scrollPane.getViewport().revalidate();
        scrollPane.revalidate();
        scrollPane.repaint();
        revalidate();
        repaint();
    }
    public void refreshGoal(Goal goal)
    {
        if (goal == null)
        {
            return;
        }

        // Collection-log updates can change many goals at once. Rebuilding one
        // consistent snapshot is both cheaper and avoids dropping sibling goals.
        render();
    }

    public void refreshCategory(GoalCategory category)
    {
        if (category == null)
        {
            return;
        }

        refresh();
    }

    public void refreshAll()
    {
        render();
    }

    private void refresh()
    {
        render();
    }

    private static final class GoalView
    {
        private final Goal goal;
        private final GoalComponent component;
        private final String title;
        private final GoalCategory category;
        private final GoalStatus status;
        private final boolean hasProgress;
        private final long progressCurrent;
        private final long progressMaximum;

        private GoalView(Goal goal, GoalComponent component, String title, GoalCategory category,
                         GoalStatus status, boolean hasProgress, long progressCurrent, long progressMaximum)
        {
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
