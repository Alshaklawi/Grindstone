package com.grindstone.ui.editor;

import com.grindstone.content.collectionlog.CollectionLogEntry;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.milestone.ManualMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
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
import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.RepeatSchedule;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.SearchableList;
import com.grindstone.ui.item.ItemLookup;
import com.grindstone.ui.item.ItemPickerPanel;
import com.grindstone.ui.util.UiTooltips;
import com.grindstone.util.HtmlUtils;
import com.grindstone.util.NumberFormats;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class MilestoneEditorPanel extends JPanel {
    private final JComboBox<String> type = new JComboBox<>(new String[]{ "Skill level", "Skill experience", "Combat level", "Total level", "Total experience", "Boss kills & completions", "Page", "Item unlock", "Custom milestone", "Quest", "Quest series", "Achievement diary region & tier", "Achievement diary tier (all regions)", "Combat achievements: boss", "Combat achievements: reward tier", "Combat achievements: difficulty tasks", "Owned item quantity", "Combat achievement task", "Base level (all skills)", "Base experience (all skills)", "Session experience", "Personal checklist / routine", "Total unlocks", "Raid completions", "Slayer counters", "Clue scroll completions", "Minigame counters", "Skilling activities", "Quest speedrun completions", "Tab"});
    private final ChecklistMilestoneForm checklistForm = new ChecklistMilestoneForm();
    private final CustomMilestoneForm customForm = new CustomMilestoneForm();
    private final RepeatMilestoneForm repeatForm = new RepeatMilestoneForm();
    public RepeatSchedule getRepeatSchedule() {
        return repeatForm.isVisible() ? repeatForm.getSchedule() : RepeatSchedule.NONE;
    }
    public long getRepeatTarget() {
        if (type.getSelectedIndex() == 21) {
            return checklistForm.read().getProgressMaximum();
        }
        return repeatForm.getTarget();
    }
    public void loadRepeat(RepeatSchedule schedule, long target) {
        repeatForm.load(schedule, target);
    }
    private final ItemPickerPanel itemPicker;
    private final SearchableList<String> subject = new SearchableList<String>(this::onSubjectSelected) {
        @Override
        protected String getText(String value) {
            return value;
        }

        @Override
        protected String getCategory(String value) {
            Supplier<Milestone> factory = choices.get(value);
            return factory == null ? "" : factory.get().getCategory().getDisplayName();
        }
    };
    private final SkillIconManager skillIcons = new SkillIconManager();
    private final Map<String, JToggleButton> skillButtons = new LinkedHashMap<>();
    private final JLabel selectedSkill = new JLabel();
    private final JSpinner target = new JSpinner();
    private final JPanel body = new JPanel();
    private final JLabel hint = new JLabel();
    private final JLabel preview = new JLabel();
    private final JPanel presets = new JPanel(new GridLayout(1, 3, 6, 0));
    private final Map<String, Supplier<Milestone>> choices = new LinkedHashMap<>();
    private final ItemManager itemManager;
    private final boolean editing;
    private boolean updating;
    private boolean collectionSnapshotAvailable = true;

    private int collectionKnownUnlocks;
    private long collectionSnapshotAt;

    public void setCollectionSnapshotStatus(boolean available, int knownUnlocks, long capturedAt) {
        collectionSnapshotAvailable = available;
        collectionKnownUnlocks = knownUnlocks;
        collectionSnapshotAt = capturedAt;
        rebuild();
    }

    private JPanel createCollectionSnapshotInfo() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel status = new JLabel(collectionSnapshotAvailable ? "Snapshot ready · " + collectionKnownUnlocks + " unlocks" : "Snapshot needed · " + collectionKnownUnlocks + " known unlocks");
        status.setIcon(new ImageIcon(Icon.COLLECTION_LOG.getImage()));
        row.add(status, BorderLayout.CENTER);
        JButton info = new JButton("i");
        info.setMargin(new Insets(1, 6, 1, 6));
        String captured = collectionSnapshotAt > 0 ? "<br>Last full capture: " + DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(collectionSnapshotAt)) + "." : "<br>No verified full snapshot yet.";
        String tip = "<html>Open your in-game collection log to capture available unlocks.<br>A full snapshot must match the game's total before adding collection milestones." + captured + "<br>See dashboard Snapshots for incomplete data and refresh tips.</html>";
        info.setToolTipText(tip);
        info.getAccessibleContext().setAccessibleName("Collection log snapshot information");
        status.setToolTipText(tip);
        row.add(info, BorderLayout.EAST);
        return row;
    }

    private boolean needsCollectionSnapshot() {
        int kind = type.getSelectedIndex();
        return (kind == 6 || kind == 7 || kind == 22 || kind == 29) && !collectionSnapshotAvailable;
    }
    private Map<Milestone, RepeatSchedule> existingGoals;
    private final Map<Integer, Map<String, Supplier<Milestone>>> completionChoices;

    public MilestoneEditorPanel(ItemManager itemManager, ItemLookup searchService) {
        this(itemManager, searchService, CompletionMilestoneChoices.create(null));
    }

    public void hideObtainedCollectionItems(Set<Integer> obtained) {
        Set<Integer> snapshot = new HashSet<>(obtained);
        itemPicker.setAvailableItems(id -> type.getSelectedIndex() != 7 || !snapshot.contains(CollectionLogPages.getCanonicalItemId(id)));
    }

    public void setExistingMilestones(Map<Milestone, RepeatSchedule> existingGoals) {
        this.existingGoals = new LinkedHashMap<>(existingGoals);
    }

    public boolean isReviewEnabled() {
        return existingGoals != null;
    }

    public String getReviewText(Milestone goal) {
        String name = goal.getTitle(null);
        if ((type.getSelectedIndex() == 7 || type.getSelectedIndex() == 16) && itemPicker.hasSelection()) {
            name = itemPicker.getSelectedItemName() + " · item " + itemPicker.getSelectedItemId();
        }
        StringBuilder text = new StringBuilder(name).append('\n');
        text.append("Type: ").append(type.getSelectedItem()).append('\n');
        text.append("Target: ").append(goal.getProgressMaximum() > 0 ? String.format(Locale.ROOT, "%,d", goal.getProgressMaximum()) : "native completion; reading loads after saving").append('\n');
        text.append("Repeat: ").append(getRepeatSchedule());
        if (getRepeatSchedule() != RepeatSchedule.NONE) {
            text.append(" · ").append(String.format(Locale.ROOT, "%,d per period", getRepeatTarget()));
        }
        boolean duplicate = existingGoals != null && existingGoals.entrySet().stream().anyMatch(entry -> entry.getValue() == getRepeatSchedule() && GrindstoneProfile.sameSelection(goal, entry.getKey()));
        text.append("\n\n").append(duplicate ? "A matching tracker and schedule existed when this editor opened. You will be asked before changing its target; no duplicate is added." : "This adds a new milestone. Matching trackers are checked again before saving.");
        text.append("\n\n").append(goal instanceof ManualMilestone ? "Manual progress only. Grindstone never performs game actions." : "Client progress starts after saving. Some counters, storage and collection logs need their native interface opened. Unavailable readings are not confirmed zero.");
        if (!goal.getDescription().isEmpty()) {
            text.append("\n\n").append(goal.getDescription());
        }
        return text.toString();
    }

    public MilestoneEditorPanel(ItemManager itemManager, ItemLookup searchService, Map<Integer, Map<String, Supplier<Milestone>>> completionChoices) {
        this(itemManager, searchService, completionChoices, false);
    }

    public MilestoneEditorPanel(ItemManager itemManager, ItemLookup searchService, Map<Integer, Map<String, Supplier<Milestone>>> completionChoices, boolean editing) {
        super(new BorderLayout(0, 12));
        this.completionChoices = completionChoices;
        this.editing = editing;
        this.itemManager = itemManager;
        itemPicker = new ItemPickerPanel(searchService, selection -> notifyReadiness());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        UiTooltips.set(type, "Choose a milestone type. The form will show only the relevant fields.");
        UiTooltips.set(subject, "Choose the skill, activity or collection-log page to track.");
        add(new MilestoneTypePicker(type, kind -> !completionChoices.containsKey(kind) || !completionChoices.get(kind).isEmpty()), BorderLayout.NORTH);
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        add(body, BorderLayout.CENTER);
        preview.setOpaque(true);
        preview.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        preview.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        preview.setFont(FontManager.getRunescapeSmallFont());
        preview.setAlignmentX(LEFT_ALIGNMENT);
        hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        hint.setFont(FontManager.getRunescapeSmallFont());
        hint.setAlignmentX(LEFT_ALIGNMENT);
        presets.setOpaque(false);
        type.addActionListener(event -> rebuild());
        target.addChangeListener(event -> updatePreview());
        checklistForm.addStepsListener(() -> {
            if (type.getSelectedIndex() == 21) {
                repeatForm.setFixedTarget(checklistForm.getStepCount());
            }
        });
        repeatForm.addScheduleListener(() -> {
            customForm.setRecurring(type.getSelectedIndex() == 8 && repeatForm.getSchedule() != RepeatSchedule.NONE);
            firePropertyChange("formLayout", false, true);
        });
        rebuild();
    }

    private JPanel field(String title, JComponent control) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 5));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(LEFT_ALIGNMENT);
        JLabel label = new JLabel(title);
        label.setLabelFor(control);
        label.setToolTipText(control.getToolTipText());
        wrapper.add(label, BorderLayout.NORTH);
        wrapper.add(control, BorderLayout.CENTER);
        return wrapper;
    }

    private void rebuild() {
        itemPicker.stopSearch();
        body.removeAll();
        choices.clear();
        skillButtons.clear();
        int kind = type.getSelectedIndex();
        repeatForm.setFixedTarget(null);
        if (kind == 6 || kind == 7 || kind == 22 || kind == 29) {
            body.add(createCollectionSnapshotInfo());
            body.add(Box.createVerticalStrut(8));
        }
        if (needsCollectionSnapshot()) {
            repeatForm.setVisible(false);
            setHint("Open your in-game collection log, then reopen Add milestone after a full snapshot is captured. Check Snapshots on the dashboard for capture progress.");
            body.add(hint);
            notifyReadiness();
            revalidate();
            repaint();
            firePropertyChange("formLayout", false, true);
            return;
        }
        if (kind == 21) {
            checklistForm.setAlignmentX(LEFT_ALIGNMENT);
            body.add(checklistForm);
            setHint("Check each step manually. Daily, weekly and monthly routines require every step, reset at the next UTC period and keep observed history. No game actions are performed.");
        } else if (kind == 8) {
            customForm.setRecurring(repeatForm.getSchedule() != RepeatSchedule.NONE);
            customForm.setAlignmentX(LEFT_ALIGNMENT);
            body.add(customForm);
            setHint("Write your own milestone. You control its progress and can edit its details later.");
        } else if (kind == 7 || kind == 16) {
            itemPicker.setAlignmentX(LEFT_ALIGNMENT);
            body.add(itemPicker);
            itemPicker.resumeSearch();
            if (kind == 16) {
                body.add(Box.createVerticalStrut(12));
                target.setModel(MilestoneTargetModels.create(new PossessedItemMilestone( ItemID.COINS)));
                target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
                MilestoneTargetModels.describe(target);
                JPanel quantity = field("Target quantity", target);
                presets.removeAll();
                presets.setVisible(true);
                for (long value : new long[]{1_000_000L, 100_000_000L, 1_500_000_000L}) {
                    JButton preset = new JButton(NumberFormats.formatTarget(value));
                    preset.setToolTipText(String.format(Locale.ROOT, "Set target quantity to %,d", value));
                    preset.addActionListener(event -> target.setValue(value));
                    presets.add(preset);
                }
                quantity.add(presets, BorderLayout.SOUTH);
                body.add(quantity);
            }
            setHint(kind == 16 ? "Counts the selected item across known personal storage, including coins in supported coffers. Open your bank and other storage to load them. This is item quantity, not bank value. Shared storage and unexposed locations are excluded." : "Tracks collection-log unlocks and first-seen dates, not inventory or bank quantities. Choose an item that belongs to the collection log.");
        } else {
            populateChoices(kind);
            updating = true;
            subject.setItems(choices.keySet());
            updating = false;
            if (kind < 2 || CompletionMilestoneChoices.isActivityType(kind) || kind == 6 || kind == 29 || kind >= 9 && kind < 18 || kind == 20) {
                boolean skillSelection = kind == 0 || kind == 1 || kind == 20;
                body.add(field(skillSelection ? "Skill" : CompletionMilestoneChoices.isActivityType(kind) ? "Activity" : kind == 29 ? "Tab" : kind >= 9 ? "Track" : "Page", skillSelection ? createSkillPicker() : subject));
                body.add(Box.createVerticalStrut(12));
            }
            Milestone sample = choices.isEmpty() ? null : choices.values().iterator().next().get();
            if (sample != null && sample.supportsTarget()) {
                configureTarget(false);
                JPanel targetField = field(kind == 1 || kind == 4 || kind == 19 || kind == 20 ? "Target experience" : CompletionMilestoneChoices.isActivityType(kind) ? "Target amount" : kind == 22 ? "Target unlocks" : "Target level", target);
                targetField.add(presets, BorderLayout.SOUTH);
                presets.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
                body.add(targetField);
                body.add(Box.createVerticalStrut(12));
            }
            updatePreview();
            body.add(preview);
            setHint(kind >= 9 ? (choices.isEmpty() ? "Client data is not ready. Log in and reopen this dialog." : "Progress reads your logged-in character automatically. Completion milestones have no manual target.") : kind == 6 ? "Open your in-game collection log to refresh unlocks. Each item keeps its first-seen date." : kind == 5 ? "Progress reads your character's activity count automatically." : kind == 1 || kind == 4 ? "Progress reads your character's real experience automatically." : "Uses real levels. Temporary boosts do not advance level milestones.");
            if (CompletionMilestoneChoices.isActivityType(kind)) {
                setHint("Uses your character's client-reported activity counter. Open the relevant in-game interface if the reading has not loaded. Repeating routines count gains from their captured baseline.");
            }
            if (kind == 29) {
                setHint("Complete a whole collection-log tab. Each distinct unlock counts once across its pages; duplicate drops do not add progress.");
            }
            if (kind == 22) {
                setHint("Counts total collection-log unlocks across all pages. Open your collection log if the total has not loaded. Each unlock counts once; duplicate item quantities do not add progress.");
            }
            if (kind == 18 || kind == 19) {
                setHint("Every skill must reach this target. The bar shows your lowest " + (kind == 18 ? "real skill level; temporary boosts do not count." : "skill XP, not total or average experience."));
            }
            if (kind == 20) {
                setHint("Counts observed XP gains this login, not existing XP. Relogging or reconnecting resets it. For daily or weekly gains, choose Skill experience with a repeat schedule instead.");
            }
        }
        repeatForm.setVisible(kind == 1 || kind == 4 || CompletionMilestoneChoices.isActivityType(kind) || kind == 8 || kind == 21);
        if (kind == 21) {
            repeatForm.setFixedTarget(checklistForm.getStepCount());
        }
        if (repeatForm.isVisible()) {
            repeatForm.setAlignmentX(LEFT_ALIGNMENT);
            body.add(repeatForm);
        }
        body.add(Box.createVerticalStrut(10));
        body.add(hint);
        notifyReadiness();
        revalidate();
        repaint();
        firePropertyChange("formLayout", false, true);
    }

    private JPanel createSkillPicker() {
        JPanel grid = new JPanel(new GridLayout(0, 6, 6, 6));
        grid.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (String name : choices.keySet()) {
            Skill skill = null;
            for (Skill candidate : Skill.values()) {
                if (candidate.getName().equals(name)) {
                    skill = candidate;
                    break;
                }
            }
            JToggleButton button = new JToggleButton(new ImageIcon(skill == null ? Icon.SKILLING.getImage() : skillIcons.getSkillImage(skill)));
            if (skill == null) {
                button.setText("All");
            }
            button.setPreferredSize(new Dimension(44, 38));
            button.setMargin(new Insets(4, 4, 4, 4));
            button.setToolTipText(name);
            button.getAccessibleContext().setAccessibleName(name);
            button.addActionListener(event -> subject.setSelectedValue(name));
            group.add(button);
            skillButtons.put(name, button);
            grid.add(button);
        }
        syncSkillSelection();
        JPanel picker = new JPanel(new BorderLayout(0, 6));
        picker.setOpaque(false);
        selectedSkill.setFont(FontManager.getRunescapeBoldFont());
        selectedSkill.setForeground(ColorScheme.BRAND_ORANGE);
        picker.add(selectedSkill, BorderLayout.NORTH);
        picker.add(grid, BorderLayout.CENTER);
        return picker;
    }

    private void syncSkillSelection() {
        String selected = subject.getSelectedValue();
        selectedSkill.setText(selected == null ? "Choose a skill" : selected);
        skillButtons.forEach((name, button) -> {
            boolean active = name.equals(selected);
            button.setSelected(active);
            button.setBackground(active ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
            button.setBorder(BorderFactory.createLineBorder(active ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR, 2));
        });
    }

    private void populateChoices(int kind) {
        if (completionChoices.containsKey(kind) && (kind != 6 || !completionChoices.get(kind).isEmpty() || completionChoices.containsKey(0))) {
            choices.putAll(completionChoices.get(kind));
            return;
        }
        if (kind == 20) {
            choices.put("All skills (total gains)", SessionExperienceMilestone::new);
            for (Skill skill : Skill.values()) {
                choices.put(skill.getName(), () -> new SessionExperienceMilestone(skill));
            }
            return;
        }
        if (kind == 6 && !completionChoices.getOrDefault(6, Collections.emptyMap()).isEmpty()) {
            choices.putAll(completionChoices.get(6));
            return;
        }
        if (kind == 18) {
            choices.put("Base level", BaseLevelMilestone::new);
            return;
        }
        if (kind == 19) {
            choices.put("Base experience", BaseExperienceMilestone::new);
            return;
        }
        if (kind >= 9) {
            choices.putAll(completionChoices.getOrDefault(kind, Collections.emptyMap()));
            return;
        }
        if (kind < 2) {
            for (Skill skill : Skill.values()) {
                choices.put(skill.getName(), kind == 0 ? () -> new SkillLevelMilestone(skill) : () -> new SkillExperienceMilestone(skill));
            }
        } else if (kind == 2) {
            choices.put("Combat level", CombatLevelMilestone::new);
        } else if (kind == 3) {
            choices.put("Total level", TotalLevelMilestone::new);
        } else if (kind == 4) {
            choices.put("Total experience", TotalExperienceMilestone::new);
        } else {
            for (CollectionLogEntry entry : CollectionLogEntry.values()) {
                String name = entry.name().replace('_', ' ').toLowerCase(Locale.ROOT);
                choices.put(Character.toUpperCase(name.charAt(0)) + name.substring(1), () -> new CollectionLogEntryMilestone(itemManager, entry));
            }
        }
    }

    private void configureTarget(boolean preserve) {
        Supplier<Milestone> factory = choices.get(subject.getSelectedValue());
        if (factory == null) {
            return;
        }
        Milestone goal = factory.get();
        if (!goal.supportsTarget()) {
            return;
        }
        long previous = target.getValue() instanceof Number ? ((Number) target.getValue()).longValue() : 0;
        SpinnerNumberModel model = !editing && completionChoices.containsKey(type.getSelectedIndex())
                ? MilestoneTargetModels.createNextTarget(goal) : MilestoneTargetModels.create(goal);
        if (preserve && previous >= ((Number) model.getMinimum()).longValue() && previous <= ((Number) model.getMaximum()).longValue()) {
            model.setValue(previous);
        }
        target.setModel(model);
        target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
        MilestoneTargetModels.describe(target);
        presets.removeAll();
        int kind = type.getSelectedIndex();
        long maximum = ((Number) model.getMaximum()).longValue();
        long[] values = kind == 0 || kind == 18 ? new long[]{50, 75, 99}
                : kind == 22 ? new long[]{100, 500, 1000}
                : kind == 20 ? new long[]{10_000, 100_000, 1_000_000}
                : kind == 1 || kind == 19 ? new long[]{1_000_000, SkillExperienceMilestone.LEVEL_99_EXPERIENCE, maximum}
                : kind == 2 ? new long[]{70, 100, 126}
                : kind == 3 ? new long[]{1000, 1500, maximum}
                : kind == 4 ? new long[]{100_000_000, goal.getProgressMaximum(), maximum}
                : new long[]{100, 500, 1000};
        long minimum = Math.max(((Number) model.getMinimum()).longValue(), goal.getProgressCurrent() + 1);
        Set<Long> suggestions = new TreeSet<>();
        for (long value : values) {
            if (value >= minimum && value <= maximum) {
                suggestions.add(value);
            }
        }
        // Fill gaps with nearby round thresholds instead of leaving only distant targets.
        long step = kind == 0 || kind == 18 ? 5 : kind == 2 ? 10 : 100;
        while (step <= minimum / 100 && step <= Long.MAX_VALUE / 10) {
            step *= 10;
        }
        long next = ((minimum - 1) / step + 1) * step;
        if (suggestions.size() < 3) {
            for (int index = 0; index < 3 && next <= maximum; index++, next += step) {
                suggestions.add(next);
            }
        }
        int shown = 0;
        for (long value : suggestions) {
            if (shown++ == 3) {
                break;
            }
            JButton preset = new JButton(NumberFormats.formatTarget(value));
            preset.setToolTipText(String.format(Locale.ROOT, "Set target to %,d", value));
            preset.addActionListener(event -> target.setValue(value));
            presets.add(preset);
        }
        presets.setVisible(presets.getComponentCount() > 0);
    }

    private void onSubjectSelected(String value) {
        if (!updating) {
            syncSkillSelection();
            configureTarget(true);
            updatePreview();
            notifyReadiness();
        }
    }

    private void updatePreview() {
        if (choices.isEmpty()) {
            preview.setText("No choices available");
            return;
        }
        if (type.getSelectedIndex() == 7 || type.getSelectedIndex() == 8) {
            return;
        }
        String name = subject.getSelectedValue();
        if (name == null) {
            preview.setText("No matching choices");
            return;
        }
        Milestone sample = choices.get(name).get();
        UiTooltips.set(subject, sample.getDescription().isEmpty() ? "Choose the milestone to track on your character." : sample.getDescription());
        String detail = sample.supportsTarget() ? String.format(Locale.ROOT, "Target: %,d", ((Number) target.getValue()).longValue()) : sample.getTitle(null);
        preview.setText("<html><b>" + HtmlUtils.escape(name) + "</b><br>" + detail + "</html>");
    }

    private void setHint(String text) {
        hint.setText("<html><div style='width: 350px'>" + HtmlUtils.escape(text) + "</div></html>");
    }

    public boolean isReady() {
        if (needsCollectionSnapshot()) {
            return false;
        }
        return type.getSelectedIndex() == 7 || type.getSelectedIndex() == 16 ? itemPicker.hasSelection()
                : type.getSelectedIndex() == 8 || type.getSelectedIndex() == 21 || subject.getSelectedValue() != null;
    }

    private void notifyReadiness() {
        firePropertyChange("ready", !isReady(), isReady());
    }

    public Milestone createMilestone() throws ParseException {
        if (needsCollectionSnapshot()) {
            throw new IllegalArgumentException("Capture a full collection-log snapshot before adding collection milestones.");
        }
        if (type.getSelectedIndex() == 21) {
            repeatForm.commit();
            return checklistForm.read();
        }
        if (repeatForm.isVisible()) {
            repeatForm.commit();
        }
        if (type.getSelectedIndex() == 8) {
            return customForm.read(getRepeatSchedule() == RepeatSchedule.NONE ? 0 : getRepeatTarget());
        }
        if (type.getSelectedIndex() == 7) {
            return new CollectionLogItemMilestone(itemPicker.getSelectedItemId(), 1);
        }
        if (type.getSelectedIndex() == 16) {
            target.commitEdit();
            return new PossessedItemMilestone( itemPicker.getSelectedItemId(), ((Number) target.getValue()).longValue());
        }
        Supplier<Milestone> factory = choices.get(subject.getSelectedValue());
        if (factory == null) {
            throw new IllegalArgumentException("Choose a matching milestone from the list first.");
        }
        Milestone goal = factory.get();
        if (goal.supportsTarget()) {
            target.commitEdit();
            goal.setTarget(((Number) target.getValue()).longValue());
        }
        return goal;
    }

    public void stopSearch() {
        itemPicker.stopSearch();
    }
    public void loadMilestone(Milestone goal) {
        if (goal instanceof ChecklistMilestone) {
            type.setSelectedIndex(21);
            checklistForm.load((ChecklistMilestone) goal);
            repeatForm.setFixedTarget(goal.getProgressMaximum());
            return;
        }
        if (goal instanceof PossessedItemMilestone) {
            type.setSelectedIndex(16);
            target.setValue(goal.getProgressMaximum());
            itemPicker.loadItem(((PossessedItemMilestone) goal).getItem());
            return;
        }
        if (goal instanceof CollectionLogItemMilestone) {
            type.setSelectedIndex(7);
            itemPicker.loadItem(((CollectionLogItemMilestone) goal).getItem());
            return;
        }
        if (goal instanceof CustomMilestone) {
            type.setSelectedIndex(8);
            customForm.load((CustomMilestone) goal);
            return;
        }
        for (int kind = 0; kind < type.getItemCount(); kind++) {
            if (kind == 7 || kind == 8 || kind == 16 || kind == 21) {
                continue;
            }
            type.setSelectedIndex(kind);
            for (Map.Entry<String, Supplier<Milestone>> choice : choices.entrySet()) {
                if (GrindstoneProfile.sameSelection(goal, choice.getValue().get())) {
                    subject.setSelectedValue(choice.getKey());
                    if (goal.supportsTarget()) {
                        target.setValue(goal.getProgressMaximum());
                    }
                    return;
                }
            }
        }
        throw new IllegalArgumentException("This milestone's subject is not available in the current client data.");
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(380, size.height);
    }
}
