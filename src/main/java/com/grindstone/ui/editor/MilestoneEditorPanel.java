package com.grindstone.ui.editor;

import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogItemMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
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
    private final JComboBox<MilestoneType> type = new JComboBox<>(MilestoneType.values());
    private final ChecklistMilestoneForm checklistForm = new ChecklistMilestoneForm();
    private final CustomMilestoneForm customForm = new CustomMilestoneForm();
    private final RepeatMilestoneForm repeatForm = new RepeatMilestoneForm();
    public RepeatSchedule getRepeatSchedule() {
        return repeatForm.isVisible() ? repeatForm.getSchedule() : RepeatSchedule.NONE;
    }
    public long getRepeatTarget() {
        if (getSelectedType() == MilestoneType.CHECKLIST) {
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
        String captured = collectionSnapshotAt > 0 ? "<br>Last checked " + DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(collectionSnapshotAt)) + "." : "";
        String tip = "<html>Open your collection log to load all unlocks before adding milestones." + captured + "<br>See Game data for loading progress.</html>";
        info.setToolTipText(tip);
        info.getAccessibleContext().setAccessibleName("Collection log snapshot information");
        status.setToolTipText(tip);
        row.add(info, BorderLayout.EAST);
        return row;
    }

    private boolean needsCollectionSnapshot() {
        MilestoneType kind = getSelectedType();
        return kind.isCollectionSnapshotRequired() && !collectionSnapshotAvailable;
    }
    private Map<Milestone, RepeatSchedule> existingGoals;
    private final Map<MilestoneType, Map<String, Supplier<Milestone>>> completionChoices;

    public MilestoneEditorPanel(ItemManager itemManager, ItemLookup searchService) {
        this(itemManager, searchService, CompletionMilestoneChoices.create(null));
    }

    public void hideObtainedCollectionItems(Set<Integer> obtained) {
        Set<Integer> snapshot = new HashSet<>(obtained);
        itemPicker.setAvailableItems(id -> getSelectedType() != MilestoneType.COLLECTION_ITEM || !snapshot.contains(CollectionLogPages.getCanonicalItemId(id)));
    }

    public void setExistingMilestones(Map<Milestone, RepeatSchedule> existingGoals) {
        this.existingGoals = new LinkedHashMap<>(existingGoals);
    }

    public boolean isReviewEnabled() {
        return existingGoals != null;
    }

    public String getReviewText(Milestone goal) {
        String name = goal.getTitle(null);
        if ((getSelectedType() == MilestoneType.COLLECTION_ITEM || getSelectedType() == MilestoneType.OWNED_ITEM) && itemPicker.hasSelection()) {
            name = itemPicker.getSelectedItemName() + " · item " + itemPicker.getSelectedItemId();
        }
        StringBuilder text = new StringBuilder(name).append('\n');
        text.append("Type: ").append(type.getSelectedItem()).append('\n');
        text.append("Target: ").append(goal.getProgressMaximum() > 0 ? String.format(Locale.ROOT, "%,d", goal.getProgressMaximum()) : "Complete in game").append('\n');
        text.append("Repeat: ").append(getRepeatSchedule());
        if (getRepeatSchedule() != RepeatSchedule.NONE) {
            text.append(" · ").append(String.format(Locale.ROOT, "%,d per period", getRepeatTarget()));
        }
        boolean duplicate = existingGoals != null && existingGoals.entrySet().stream().anyMatch(entry -> entry.getValue() == getRepeatSchedule() && GrindstoneProfile.sameSelection(goal, entry.getKey()));
        if (duplicate) {
            text.append("\n\nThis milestone already exists. You can update its target.");
        }
        if (!goal.getDescription().isEmpty()) {
            text.append("\n\n").append(goal.getDescription());
        }
        return text.toString();
    }

    public MilestoneEditorPanel(ItemManager itemManager, ItemLookup searchService, Map<MilestoneType, Map<String, Supplier<Milestone>>> completionChoices) {
        this(itemManager, searchService, completionChoices, false);
    }

    public MilestoneEditorPanel(ItemManager itemManager, ItemLookup searchService, Map<MilestoneType, Map<String, Supplier<Milestone>>> completionChoices, boolean editing) {
        super(new BorderLayout(0, 12));
        this.completionChoices = completionChoices;
        this.editing = editing;
        this.itemManager = itemManager;
        itemPicker = new ItemPickerPanel(searchService, selection -> notifyReadiness());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        UiTooltips.set(type, "Choose what to track.");
        UiTooltips.set(subject, "Choose a skill, activity or collection-log page.");
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
            if (getSelectedType() == MilestoneType.CHECKLIST) {
                repeatForm.setFixedTarget(checklistForm.getStepCount());
            }
        });
        repeatForm.addScheduleListener(() -> {
            customForm.setRecurring(getSelectedType() == MilestoneType.CUSTOM && repeatForm.getSchedule() != RepeatSchedule.NONE);
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
        MilestoneType kind = getSelectedType();
        repeatForm.setFixedTarget(null);
        if (kind.isCollectionSnapshotRequired()) {
            body.add(createCollectionSnapshotInfo());
            body.add(Box.createVerticalStrut(8));
        }
        if (needsCollectionSnapshot()) {
            repeatForm.setVisible(false);
            setHint("Open your collection log, then reopen Add milestone. Check Game data for loading progress.");
            body.add(hint);
            notifyReadiness();
            revalidate();
            repaint();
            firePropertyChange("formLayout", false, true);
            return;
        }
        if (kind == MilestoneType.CHECKLIST) {
            checklistForm.setAlignmentX(LEFT_ALIGNMENT);
            body.add(checklistForm);
            setHint("");
        } else if (kind == MilestoneType.CUSTOM) {
            customForm.setRecurring(repeatForm.getSchedule() != RepeatSchedule.NONE);
            customForm.setAlignmentX(LEFT_ALIGNMENT);
            body.add(customForm);
            setHint("");
        } else if (kind == MilestoneType.COLLECTION_ITEM || kind == MilestoneType.OWNED_ITEM) {
            itemPicker.setAlignmentX(LEFT_ALIGNMENT);
            body.add(itemPicker);
            itemPicker.resumeSearch();
            if (kind == MilestoneType.OWNED_ITEM) {
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
            setHint(kind == MilestoneType.OWNED_ITEM ? "Counts items in known storage. Open your bank and other storage to refresh." : "Choose a collection-log item. Open your collection log to refresh unlocks.");
        } else {
            populateChoices(kind);
            updating = true;
            subject.setItems(choices.keySet());
            updating = false;
            if (kind.isSkill() || CompletionMilestoneChoices.isActivityType(kind) || kind == MilestoneType.COLLECTION_PAGE || kind == MilestoneType.COLLECTION_TAB || kind.isCompletion() || kind == MilestoneType.SESSION_EXPERIENCE) {
                boolean skillSelection = kind == MilestoneType.SKILL_LEVEL || kind == MilestoneType.SKILL_EXPERIENCE || kind == MilestoneType.SESSION_EXPERIENCE;
                body.add(field(skillSelection ? "Skill" : CompletionMilestoneChoices.isActivityType(kind) ? "Activity" : kind == MilestoneType.COLLECTION_TAB ? "Tab" : kind.isClientType() ? "Track" : "Page", skillSelection ? createSkillPicker() : subject));
                body.add(Box.createVerticalStrut(12));
            }
            Milestone sample = choices.isEmpty() ? null : choices.values().iterator().next().get();
            if (sample != null && sample.supportsTarget()) {
                configureTarget(false);
                JPanel targetField = field(kind == MilestoneType.SKILL_EXPERIENCE || kind == MilestoneType.TOTAL_EXPERIENCE || kind == MilestoneType.BASE_EXPERIENCE || kind == MilestoneType.SESSION_EXPERIENCE ? "Target experience" : CompletionMilestoneChoices.isActivityType(kind) ? "Target amount" : kind == MilestoneType.COLLECTION_TOTAL ? "Target unlocks" : "Target level", target);
                targetField.add(presets, BorderLayout.SOUTH);
                presets.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
                body.add(targetField);
                body.add(Box.createVerticalStrut(12));
            }
            updatePreview();
            body.add(preview);
            setHint(kind.isClientType() ? (choices.isEmpty() ? "Client data is not ready. Log in and reopen this dialog." : "Progress updates automatically. Completion targets are set by the game.") : kind == MilestoneType.COLLECTION_PAGE ? "Open your collection log to refresh unlocks." : kind == MilestoneType.BOSS_KILLS ? "Progress reads your character's activity count automatically." : kind == MilestoneType.SKILL_EXPERIENCE || kind == MilestoneType.TOTAL_EXPERIENCE ? "Progress reads your character's real experience automatically." : "Uses real levels. Temporary boosts do not advance level milestones.");
            if (CompletionMilestoneChoices.isActivityType(kind)) {
                setHint("Tracks your activity total. Open its in-game screen to refresh. Routines count new completions each period.");
            }
            if (kind == MilestoneType.COLLECTION_TAB) {
                setHint("Complete all unlocks in a collection-log tab. Each item counts once.");
            }
            if (kind == MilestoneType.COLLECTION_TOTAL) {
                setHint("Tracks total collection-log unlocks. Open your collection log to refresh.");
            }
            if (kind == MilestoneType.BASE_LEVEL || kind == MilestoneType.BASE_EXPERIENCE) {
                setHint("Every skill must reach this target. The bar shows your lowest " + (kind == MilestoneType.BASE_LEVEL ? "real skill level; temporary boosts do not count." : "skill XP, not total or average experience."));
            }
            if (kind == MilestoneType.SESSION_EXPERIENCE) {
                setHint("Tracks XP gained this login. Relogging or reconnecting clears progress.");
            }
        }
        repeatForm.setVisible(kind == MilestoneType.SKILL_EXPERIENCE || kind == MilestoneType.TOTAL_EXPERIENCE || CompletionMilestoneChoices.isActivityType(kind) || kind == MilestoneType.CUSTOM || kind == MilestoneType.CHECKLIST);
        if (kind == MilestoneType.CHECKLIST) {
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

    private MilestoneType getSelectedType() {
        return (MilestoneType) type.getSelectedItem();
    }

    private void populateChoices(MilestoneType kind) {
        choices.putAll(completionChoices.getOrDefault(kind, kind.createDefaultChoices()));
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
        SpinnerNumberModel model = !editing && completionChoices.containsKey(getSelectedType())
                ? MilestoneTargetModels.createNextTarget(goal) : MilestoneTargetModels.create(goal);
        if (preserve && previous >= ((Number) model.getMinimum()).longValue() && previous <= ((Number) model.getMaximum()).longValue()) {
            model.setValue(previous);
        }
        target.setModel(model);
        target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
        MilestoneTargetModels.describe(target);
        presets.removeAll();
        MilestoneType kind = getSelectedType();
        long maximum = ((Number) model.getMaximum()).longValue();
        long[] values = kind == MilestoneType.SKILL_LEVEL || kind == MilestoneType.BASE_LEVEL ? new long[]{50, 75, 99}
                : kind == MilestoneType.COLLECTION_TOTAL ? new long[]{100, 500, 1000}
                : kind == MilestoneType.SESSION_EXPERIENCE ? new long[]{10_000, 100_000, 1_000_000}
                : kind == MilestoneType.SKILL_EXPERIENCE || kind == MilestoneType.BASE_EXPERIENCE ? new long[]{1_000_000, SkillExperienceMilestone.LEVEL_99_EXPERIENCE, maximum}
                : kind == MilestoneType.COMBAT_LEVEL ? new long[]{70, 100, 126}
                : kind == MilestoneType.TOTAL_LEVEL ? new long[]{1000, 1500, maximum}
                : kind == MilestoneType.TOTAL_EXPERIENCE ? new long[]{100_000_000, goal.getProgressMaximum(), maximum}
                : new long[]{100, 500, 1000};
        long minimum = Math.max(((Number) model.getMinimum()).longValue(), goal.getProgressCurrent() + 1);
        Set<Long> suggestions = new TreeSet<>();
        for (long value : values) {
            if (value >= minimum && value <= maximum) {
                suggestions.add(value);
            }
        }
        // Fill gaps with nearby round thresholds instead of leaving only distant targets.
        long step = kind == MilestoneType.SKILL_LEVEL || kind == MilestoneType.BASE_LEVEL ? 5 : kind == MilestoneType.COMBAT_LEVEL ? 10 : 100;
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
        if (getSelectedType() == MilestoneType.COLLECTION_ITEM || getSelectedType() == MilestoneType.CUSTOM) {
            return;
        }
        String name = subject.getSelectedValue();
        if (name == null) {
            preview.setText("No matching choices");
            return;
        }
        Milestone sample = choices.get(name).get();
        UiTooltips.set(subject, sample.getDescription().isEmpty() ? "Choose a milestone." : sample.getDescription());
        String detail = sample.supportsTarget() ? String.format(Locale.ROOT, "Target: %,d", ((Number) target.getValue()).longValue()) : sample.getTitle(null);
        preview.setText("<html><b>" + HtmlUtils.escape(name) + "</b><br>" + detail + "</html>");
    }

    private void setHint(String text) {
        hint.setVisible(!text.isEmpty());
        hint.setText("<html><div style='width: 350px'>" + HtmlUtils.escape(text) + "</div></html>");
    }

    public boolean isReady() {
        if (needsCollectionSnapshot()) {
            return false;
        }
        return getSelectedType() == MilestoneType.COLLECTION_ITEM || getSelectedType() == MilestoneType.OWNED_ITEM ? itemPicker.hasSelection()
                : getSelectedType() == MilestoneType.CUSTOM || getSelectedType() == MilestoneType.CHECKLIST || subject.getSelectedValue() != null;
    }

    private void notifyReadiness() {
        firePropertyChange("ready", !isReady(), isReady());
    }

    public Milestone createMilestone() throws ParseException {
        if (needsCollectionSnapshot()) {
            throw new IllegalArgumentException("Open your collection log to load all unlocks first.");
        }
        if (getSelectedType() == MilestoneType.CHECKLIST) {
            repeatForm.commit();
            return checklistForm.read();
        }
        if (repeatForm.isVisible()) {
            repeatForm.commit();
        }
        if (getSelectedType() == MilestoneType.CUSTOM) {
            return customForm.read(getRepeatSchedule() == RepeatSchedule.NONE ? 0 : getRepeatTarget());
        }
        if (getSelectedType() == MilestoneType.COLLECTION_ITEM) {
            return new CollectionLogItemMilestone(itemPicker.getSelectedItemId(), 1);
        }
        if (getSelectedType() == MilestoneType.OWNED_ITEM) {
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
            type.setSelectedItem(MilestoneType.CHECKLIST);
            checklistForm.load((ChecklistMilestone) goal);
            repeatForm.setFixedTarget(goal.getProgressMaximum());
            return;
        }
        if (goal instanceof PossessedItemMilestone) {
            type.setSelectedItem(MilestoneType.OWNED_ITEM);
            target.setValue(goal.getProgressMaximum());
            itemPicker.loadItem(((PossessedItemMilestone) goal).getItem());
            return;
        }
        if (goal instanceof CollectionLogItemMilestone) {
            type.setSelectedItem(MilestoneType.COLLECTION_ITEM);
            itemPicker.loadItem(((CollectionLogItemMilestone) goal).getItem());
            return;
        }
        if (goal instanceof CustomMilestone) {
            type.setSelectedItem(MilestoneType.CUSTOM);
            customForm.load((CustomMilestone) goal);
            return;
        }
        for (MilestoneType kind : MilestoneType.values()) {
            if (kind == MilestoneType.COLLECTION_ITEM || kind == MilestoneType.CUSTOM || kind == MilestoneType.OWNED_ITEM || kind == MilestoneType.CHECKLIST) {
                continue;
            }
            type.setSelectedItem(kind);
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
