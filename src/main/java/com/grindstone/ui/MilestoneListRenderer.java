package com.grindstone.ui;

import static com.grindstone.ui.MilestoneFilters.isComplete;
import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.overlay.OverlayStyle;
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.repository.SidebarViewState;
import com.grindstone.ui.components.ContentHeightPanel;
import com.grindstone.ui.components.MilestoneComponent;
import com.grindstone.ui.components.MilestoneListPanel;
import com.grindstone.ui.components.SegmentedProgressBar;
import com.grindstone.ui.util.DialogPlacement;
import com.grindstone.ui.util.SkillMilestoneGrouping;
import com.grindstone.ui.util.UiTypography;
import com.grindstone.util.HtmlUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
final class MilestoneListRenderer extends JPanel {
    private static final Color PANEL_COLOR = ColorScheme.DARK_GRAY_COLOR;
    private static final Color HEADER_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color TEXT_COLOR = ColorScheme.TEXT_COLOR;
    private static final Color MUTED_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
    private final GrindstoneConfig config;
    private final Runnable requestRender;
    private final JPanel main = new MilestoneListPanel();
    private final JScrollPane scrollPane;
    private final Map<MilestoneCategory, Boolean> collapsedCategories;
    private final Map<RepeatSchedule, Boolean> collapsedRepeats;
    private List<MilestoneView> preparedGoals = new ArrayList<>();
    private List<?> layoutKey;
    private final Map<Milestone, MilestoneComponent> displayedCards = new IdentityHashMap<>();
    private final Map<RepeatSchedule, JButton> repeatHeadings = new LinkedHashMap<>();
    private JPanel routineOverview;
    private JLabel routineTitle;
    private JLabel routineSummary;
    private JLabel routineDue;
    private SegmentedProgressBar routineProgress;
    private final Component routineGap = Box.createVerticalStrut(12);
    private boolean todayMode;
    private String pendingReveal;
    private int displayedCount;

    MilestoneListRenderer(GrindstoneConfig config, Map<MilestoneCategory, Boolean> collapsedCategories, Map<RepeatSchedule, Boolean> collapsedRepeats, Runnable requestRender) {
        super(new BorderLayout());
        this.config = config;
        this.collapsedCategories = collapsedCategories;
        this.collapsedRepeats = collapsedRepeats;
        this.requestRender = requestRender;
        setBackground(PANEL_COLOR);
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
    }

    int getDisplayedCount() {
        return displayedCount;
    }

    void reveal(String id) {
        pendingReveal = id;
    }

    void clearPendingReveal() {
        pendingReveal = null;
    }

    public void render(GrindstoneProfile currentProfile, List<MilestoneView> allGoals, SidebarViewState viewState, String query) {
        todayMode = viewState.isRoutines();
        displayedCount = 0;
        if (currentProfile == null) {
            main.removeAll();
            layoutKey = null;

            addMessage("Log in to get started", "Your milestones load automatically for your logged-in character.");
            refreshPanel();
            return;
        }

        preparedGoals = allGoals;

        List<MilestoneView> filteredGoals = MilestoneFilters.filter(allGoals, viewState, config.hideCompletedGoals(), query);
        if (todayMode) {
            filteredGoals.removeIf(view -> view.getComponent().getClientProperty("grindstone.repeat") == RepeatSchedule.NONE);
        }
        MilestoneFilters.sort(filteredGoals, viewState.getFilters().get("sort"));
        if (todayMode) {
            filteredGoals.sort(Comparator.comparingLong((MilestoneView view) -> getNextReset(view)).thenComparing(view -> isComplete(view.getStatus())));
        }

        displayedCount = filteredGoals.size();
        List<Object> rows = new ArrayList<>();
        for (MilestoneView view : filteredGoals) {
            rows.add(Arrays.asList(view.getGoal(), view.getCategory(), view.getComponent().getClientProperty("grindstone.repeat"), view.getComponent().getClientProperty("grindstone.pinned")));
        }
        List<?> nextLayout = Arrays.asList(currentProfile, todayMode, config.showTodayOverview(), config.categorizedGoals(), config.groupRecurringGoals(), config.groupSkillGoals(), config.showPinnedSection(), new LinkedHashMap<>(collapsedCategories), new LinkedHashMap<>(collapsedRepeats), rows, allGoals.size());
        if (nextLayout.equals(layoutKey)) {
            for (MilestoneView view : filteredGoals) {
                MilestoneComponent previous = displayedCards.put(view.getGoal(), view.getComponent());
                if (previous != null && previous != view.getComponent() && previous.getParent() != null) {
                    Container parent = previous.getParent();
                    int index = Arrays.asList(parent.getComponents()).indexOf(previous);
                    view.getComponent().setAlignmentX(previous.getAlignmentX());
                    parent.remove(previous);
                    parent.add(view.getComponent(), index);
                    parent.revalidate();
                    parent.repaint();
                }
            }
            for (Map.Entry<RepeatSchedule, JButton> entry : repeatHeadings.entrySet()) {
                List<MilestoneView> repeats = new ArrayList<>();
                for (MilestoneView view : filteredGoals) {
                    if (view.getComponent().getClientProperty("grindstone.repeat") == entry.getKey() && !(config.showPinnedSection() && Boolean.TRUE.equals(view.getComponent().getClientProperty("grindstone.pinned")))) {
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
            displayedCards.put(view.getGoal(), view.getComponent());
        }
        if (todayMode || config.showTodayOverview()) {
            addTodayOverview(allGoals);
        }

        if (filteredGoals.isEmpty()) {
            if (currentProfile.getGoals().isEmpty()) {
                addMessage("No milestones yet", "Select Add milestone to start tracking.");
            } else if (!todayMode && !allGoals.isEmpty() && allGoals.stream().allMatch(view -> isComplete(view.getStatus()))) {
                addMessage("All milestones complete", "Add another milestone, or select Done to see completed ones.");
            } else {
                addMessage(todayMode ? "No matching routines" : "No matching milestones", todayMode ? "Clear filters or choose Make routine from a milestone's actions." : "Try another search term or clear your filters.");
            }
            refreshPanel();
            return;
        }

        List<MilestoneView> pinnedGoals = new ArrayList<>();
        List<MilestoneView> remainingGoals = new ArrayList<>();
        for (MilestoneView view : filteredGoals) {
            if (config.showPinnedSection() && Boolean.TRUE.equals(view.getComponent().getClientProperty("grindstone.pinned"))) {
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
                view.getComponent().setAlignmentX(LEFT_ALIGNMENT);
                main.add(view.getComponent());
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
                    if (view.getComponent().getClientProperty("grindstone.repeat") == schedule) {
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
                view.getComponent().setAlignmentX(LEFT_ALIGNMENT);
                main.add(view.getComponent());
                main.add(Box.createVerticalStrut(12));
            }
            refreshPanel();
            return;
        }
        Map<MilestoneCategory, List<MilestoneView>> groupedGoals = groupMilestones(remainingGoals);

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
            if (view.getComponent().getClientProperty("grindstone.repeat") != RepeatSchedule.NONE) {
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
        long completed = recurring.stream().filter(view -> isComplete(view.getStatus())).count();
        long waiting = recurring.stream().filter(view -> !Boolean.TRUE.equals(view.getComponent().getClientProperty("grindstone.ready"))).count();
        double ratio = recurring.stream().filter(view -> Boolean.TRUE.equals(view.getComponent().getClientProperty("grindstone.ready"))).mapToDouble(MilestoneFilters::getProgressRatio).average().orElse(0);
        if (routineOverview == null) {
            routineOverview = new ContentHeightPanel();
            routineOverview.setLayout(new BoxLayout(routineOverview, BoxLayout.Y_AXIS));
            routineOverview.setBackground(HEADER_COLOR);
            routineOverview.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            routineOverview.setAlignmentX(LEFT_ALIGNMENT);
            routineTitle = new JLabel();
            routineTitle.setFont(UiTypography.TITLE);
            routineTitle.setAlignmentX(LEFT_ALIGNMENT);
            routineOverview.add(routineTitle);
            routineOverview.add(Box.createVerticalStrut(6));
            routineSummary = new JLabel();
            routineSummary.setFont(UiTypography.BODY);
            routineSummary.setAlignmentX(LEFT_ALIGNMENT);
            routineOverview.add(routineSummary);
            routineOverview.add(Box.createVerticalStrut(4));
            routineProgress = new SegmentedProgressBar();
            routineProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
            routineProgress.setAlignmentX(LEFT_ALIGNMENT);
            routineOverview.add(routineProgress);
            routineDue = new JLabel();
            routineDue.setFont(UiTypography.BODY);
            routineDue.setForeground(MUTED_COLOR);
            routineDue.setAlignmentX(LEFT_ALIGNMENT);
            routineOverview.add(Box.createVerticalStrut(6));
            routineOverview.add(routineDue);
        }
        routineTitle.setText("Routines · " + completed + "/" + recurring.size() + " complete");
        routineSummary.setText(waiting == recurring.size() ? "Waiting for game data" : "<html>" + Math.round(ratio * 100) + (recurring.size() == 1 ? "% toward target" : "% average progress") + (waiting > 0 ? "<br>" + waiting + " waiting for data" : "") + "</html>");
        String progressTip = "Average target progress for routines with loaded data.";
        routineSummary.setToolTipText(waiting == recurring.size() ? null : progressTip);
        routineProgress.setToolTipText(progressTip);
        routineProgress.setProgress(Math.round(ratio * 100), 100);
        routineProgress.setStatus(completed == recurring.size() ? MilestoneStatus.COMPLETE : ratio > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED);
        routineProgress.setVisible(waiting < recurring.size());
        long next = recurring.stream().mapToLong(this::getNextReset).min().orElse(0);
        routineDue.setText("Next reset in " + OverlayStyle.formatCountdown(next, System.currentTimeMillis()));
        routineDue.setToolTipText(DateTimeFormatter.ofPattern("EEE d MMM HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(next)));
        if (routineOverview.getParent() != main) {
            main.add(routineOverview, 0);
            main.add(routineGap, 1);
        }
    }

    private long getNextReset(MilestoneView view) {
        RepeatSchedule schedule = (RepeatSchedule) view.getComponent().getClientProperty("grindstone.repeat");
        return schedule == RepeatSchedule.NONE ? Long.MAX_VALUE : schedule.getNextReset(schedule.getPeriodStart(System.currentTimeMillis()));
    }

    private Map<MilestoneCategory, List<MilestoneView>> groupMilestones(List<MilestoneView> goals) {
        Map<MilestoneCategory, List<MilestoneView>> grouped = new LinkedHashMap<>();

        for (MilestoneView view : goals) {
            grouped.computeIfAbsent(view.getCategory(), key -> new ArrayList<>()).add(view);
        }

        return grouped;
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
        heading.setToolTipText("Click to expand or collapse. UTC resets: daily at midnight, weekly on Monday, monthly on the 1st.");
        heading.addActionListener(event -> {
            collapsedRepeats.put(schedule, !Boolean.TRUE.equals(collapsedRepeats.get(schedule)));
            requestRender.run();
        });
        main.add(heading);
        main.add(Box.createVerticalStrut(8));
        if (!collapsed) {
            for (MilestoneView view : goals) {
                view.getComponent().setAlignmentX(LEFT_ALIGNMENT);
                main.add(view.getComponent());
                main.add(Box.createVerticalStrut(12));
            }
        }
    }

    private void updateRepeatHeading(RepeatSchedule schedule, List<MilestoneView> goals, JButton heading) {
        boolean collapsed = Boolean.TRUE.equals(collapsedRepeats.get(schedule));
        long now = System.currentTimeMillis();
        long minutes = Math.max(1, (schedule.getNextReset(schedule.getPeriodStart(now)) - now + 59_999) / 60_000);
        String countdown = minutes >= 1440 ? minutes / 1440 + "d " + minutes % 1440 / 60 + "h" : minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
        long completed = goals.stream().filter(view -> isComplete(view.getStatus())).count();
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
        categoryHeader.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR), BorderFactory.createEmptyBorder(4, 2, 4, 4)));
        categoryHeader.setAlignmentX(LEFT_ALIGNMENT);
        JButton toggleButton = new JButton(collapsed ? "\u25B6" : "\u25BC");
        toggleButton.setToolTipText(collapsed ? "Expand category" : "Collapse category");
        styleButton(toggleButton);
        toggleButton.setMargin(new Insets(2, 3, 2, 3));
        toggleButton.putClientProperty("JButton.buttonType", "toolBarButton");
        toggleButton.addActionListener(event -> {
            collapsedCategories.put(category, !Boolean.TRUE.equals(collapsedCategories.get(category)));
            requestRender.run();
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

            for (List<MilestoneView> group : config.groupSkillGoals() ? SkillMilestoneGrouping.group(goals, view -> view.getGoal()) : goals.stream().map(Collections::singletonList).collect(Collectors.toList())) {
                if (goalsPanel.getComponentCount() > 0) {
                    goalsPanel.add(Box.createVerticalStrut(12));
                }
                if (group.size() == 1) {
                    group.get(0).getComponent().setAlignmentX(LEFT_ALIGNMENT);
                    goalsPanel.add(group.get(0).getComponent());
                    continue;
                }
                JPanel skillGroup = new ContentHeightPanel();
                skillGroup.setLayout(new BoxLayout(skillGroup, BoxLayout.Y_AXIS));
                skillGroup.setBackground(PANEL_COLOR);
                skillGroup.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR), BorderFactory.createEmptyBorder(0, 6, 0, 0)));
                skillGroup.setAlignmentX(LEFT_ALIGNMENT);
                JLabel heading = new JLabel(SkillMilestoneGrouping.getSkill(group.get(0).getGoal()).getName());
                heading.setFont(UiTypography.META);
                heading.setForeground(MUTED_COLOR);
                heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
                heading.setToolTipText("Level and XP milestones for this skill.");
                skillGroup.add(heading);
                for (MilestoneView view : group) {
                    view.getComponent().setAlignmentX(LEFT_ALIGNMENT);
                    skillGroup.add(view.getComponent());
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

    private void revealPrepared(List<MilestoneView> goals) {
        if (pendingReveal == null) {
            return;
        }
        String id = pendingReveal;
        pendingReveal = null;
        for (MilestoneView view : goals) {
            if (id.equals(view.getComponent().getClientProperty("grindstone.id")) && main.isAncestorOf(view.getComponent())) {
                SwingUtilities.invokeLater(() -> view.getComponent().scrollRectToVisible(new Rectangle(0, 0, view.getComponent().getWidth(), view.getComponent().getHeight())));
                return;
            }
        }
        JOptionPane.showMessageDialog(DialogPlacement.getOwner(this), "Enable this milestone type in Grindstone settings to show it.");
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

    private void styleButton(JButton button) {
        button.setFont(UiTypography.BODY);
        button.setMargin(new Insets(4, 7, 4, 7));
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
