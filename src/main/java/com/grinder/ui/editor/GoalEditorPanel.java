package com.grinder.ui.editor;

import com.grinder.content.collectionlog.CollectionLogEntry;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.ManualGoal;
import com.grinder.content.goal.impl.ChecklistGoal;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.experience.BaseExperienceGoal;
import com.grinder.content.goal.impl.experience.SessionExperienceGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.content.goal.impl.level.BaseLevelGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.tracker.TrackerEntry;
import com.grinder.repository.GrinderProfile;
import com.grinder.repository.RepeatSchedule;
import com.grinder.ui.components.SearchableList;
import com.grinder.ui.item.ItemLookup;
import com.grinder.ui.item.ItemPickerPanel;
import com.grinder.ui.util.UiTooltips;
import com.grinder.util.HtmlUtils;
import com.grinder.util.NumberFormats;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.*;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class GoalEditorPanel extends JPanel {
    private final JComboBox<String> type = new JComboBox<>(new String[]{ "Skill level", "Skill experience", "Combat level", "Total level", "Total experience", "Activity tracker", "Collection log", "Collection item", "Custom goal", "Quest", "Quest series", "Diary region & tier", "Diary tier (all regions)", "Combat achievements: boss", "Combat achievements: reward tier", "Combat achievements: difficulty tasks", "Owned item quantity", "Combat achievement task", "Base level (all skills)", "Base experience (all skills)", "Session experience", "Personal checklist / routine"});
    private final ChecklistGoalForm checklistForm = new ChecklistGoalForm();
    private final CustomGoalForm customForm = new CustomGoalForm();
    private final RepeatGoalForm repeatForm = new RepeatGoalForm();
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
            Supplier<Goal> factory = choices.get(value);
            return factory == null ? "" : factory.get().getCategory().getDisplayName();
        }
    };
    private final JSpinner target = new JSpinner();
    private final JPanel body = new JPanel();
    private final JLabel hint = new JLabel();
    private final JLabel preview = new JLabel();
    private final JPanel presets = new JPanel(new GridLayout(1, 3, 6, 0));
    private final Map<String, Supplier<Goal>> choices = new LinkedHashMap<>();
    private final ItemManager itemManager;
    private boolean updating;
    private Map<Goal, RepeatSchedule> existingGoals;
    private final Map<Integer, Map<String, Supplier<Goal>>> completionChoices;

    public GoalEditorPanel(ItemManager itemManager, ItemLookup searchService) {
        this(itemManager, searchService, CompletionGoalChoices.create(null));
    }

    public void setExistingGoals(Map<Goal, RepeatSchedule> existingGoals) {
        this.existingGoals = new LinkedHashMap<>(existingGoals);
    }

    public boolean isReviewEnabled() {
        return existingGoals != null;
    }

    public String reviewText(Goal goal) {
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
        boolean duplicate = existingGoals != null && existingGoals.entrySet().stream().anyMatch(entry -> entry.getValue() == getRepeatSchedule() && GrinderProfile.sameSelection(goal, entry.getKey()));
        text.append("\n\n").append(duplicate ? "A matching tracker and schedule existed when this editor opened. You will be asked before changing its target; no duplicate is added." : "This adds a new goal. Matching trackers are checked again before saving.");
        text.append("\n\n").append(goal instanceof ManualGoal ? "Manual progress only. Grinder never performs game actions." : "Client progress starts after saving. Some counters, storage and collection logs need their native interface opened. Unavailable readings are not confirmed zero.");
        if (!goal.getDescription().isEmpty()) {
            text.append("\n\n").append(goal.getDescription());
        }
        return text.toString();
    }

    public GoalEditorPanel(ItemManager itemManager, ItemLookup searchService, Map<Integer, Map<String, Supplier<Goal>>> completionChoices) {
        super(new BorderLayout(0, 12));
        this.completionChoices = completionChoices;
        this.itemManager = itemManager;
        itemPicker = new ItemPickerPanel(searchService, selection -> notifyReadiness());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        UiTooltips.set(type, "Choose a goal type. The form will show only the relevant fields.");
        UiTooltips.set(subject, "Choose the skill, activity or collection-log page to track.");
        add(field("Goal type", type), BorderLayout.NORTH);
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
        int kind = type.getSelectedIndex();
        repeatForm.setFixedTarget(null);
        if (kind == 21) {
            checklistForm.setAlignmentX(LEFT_ALIGNMENT);
            body.add(checklistForm);
            setHint("Check each step manually. Daily, weekly and monthly routines require every step, reset at the next UTC period and keep observed history. No game actions are performed.");
        } else if (kind == 8) {
            customForm.setRecurring(repeatForm.getSchedule() != RepeatSchedule.NONE);
            customForm.setAlignmentX(LEFT_ALIGNMENT);
            body.add(customForm);
            setHint("Write your own goal. You control its progress and can edit its details later.");
        } else if (kind == 7 || kind == 16) {
            itemPicker.setAlignmentX(LEFT_ALIGNMENT);
            body.add(itemPicker);
            itemPicker.resumeSearch();
            if (kind == 16) {
                body.add(Box.createVerticalStrut(12));
                target.setModel(GoalTargetModels.create(new PossessedItemGoal( ItemID.COINS)));
                target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
                GoalTargetModels.describe(target);
                JPanel quantity = field("Target quantity", target);
                presets.removeAll();
                for (long value : new long[]{1_000_000L, 100_000_000L, 1_500_000_000L}) {
                    JButton preset = new JButton(NumberFormats.target(value));
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
            if (kind < 2 || kind == 5 || kind == 6 || kind >= 9 && kind < 18 || kind == 20) {
                body.add(field(kind >= 9 ? "Track" : kind == 5 ? "Activity" : kind == 6 ? "Collection-log page" : "Skill", subject));
                body.add(Box.createVerticalStrut(12));
            }
            Goal sample = choices.isEmpty() ? null : choices.values().iterator().next().get();
            if (sample != null && sample.supportsTarget()) {
                configureTarget(false);
                JPanel targetField = field(kind == 1 || kind == 4 || kind == 19 || kind == 20 ? "Target experience" : kind == 5 ? "Target amount" : "Target level", target);
                targetField.add(presets, BorderLayout.SOUTH);
                presets.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
                body.add(targetField);
                body.add(Box.createVerticalStrut(12));
            }
            updatePreview();
            body.add(preview);
            setHint(kind >= 9 ? (choices.isEmpty() ? "Client data is not ready. Log in and reopen this dialog." : "Progress reads your logged-in character automatically. Completion goals have no manual target.") : kind == 6 ? "Open your in-game collection log to refresh unlocks. Each item keeps its first-seen date." : kind == 5 ? "Progress reads your character's activity count automatically." : kind == 1 || kind == 4 ? "Progress reads your character's real experience automatically." : "Uses real levels. Temporary boosts do not advance level goals.");
            if (kind == 18 || kind == 19) {
                setHint("Every skill must reach this target. The bar shows your lowest " + (kind == 18 ? "real skill level; temporary boosts do not count." : "skill XP, not total or average experience."));
            }
            if (kind == 20) {
                setHint("Counts observed XP gains this login, not existing XP. Relogging or reconnecting resets it. For daily or weekly gains, choose Skill experience with a repeat schedule instead.");
            }
        }
        repeatForm.setVisible(kind == 1 || kind == 4 || kind == 5 || kind == 8 || kind == 21);
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

    private void populateChoices(int kind) {
        if (kind == 20) {
            choices.put("All skills (total gains)", SessionExperienceGoal::new);
            for (Skill skill : Skill.values()) {
                choices.put(skill.getName(), () -> new SessionExperienceGoal(skill));
            }
            return;
        }
        if (kind == 6 && !completionChoices.getOrDefault(6, Collections.emptyMap()).isEmpty()) {
            choices.putAll(completionChoices.get(6));
            return;
        }
        if (kind == 18) {
            choices.put("Base level", BaseLevelGoal::new);
            return;
        }
        if (kind == 19) {
            choices.put("Base experience", BaseExperienceGoal::new);
            return;
        }
        if (kind >= 9) {
            choices.putAll(completionChoices.getOrDefault(kind, Collections.emptyMap()));
            return;
        }
        if (kind < 2) {
            for (Skill skill : Skill.values()) {
                choices.put(skill.getName(), kind == 0 ? () -> new SkillLevelGoal(skill) : () -> new SkillExperienceGoal(skill));
            }
        } else if (kind == 2) {
            choices.put("Combat level", CombatLevelGoal::new);
        } else if (kind == 3) {
            choices.put("Total level", TotalLevelGoal::new);
        } else if (kind == 4) {
            choices.put("Total experience", TotalExperienceGoal::new);
        } else if (kind == 5) {
            for (TrackerEntry entry : TrackerEntry.values()) {
                choices.put(entry.getDisplayName(), () -> new TrackerGoal(entry, entry.getDefaultTarget()));
            }
        } else {
            for (CollectionLogEntry entry : CollectionLogEntry.values()) {
                String name = entry.name().replace('_', ' ').toLowerCase(Locale.ROOT);
                choices.put(Character.toUpperCase(name.charAt(0)) + name.substring(1), () -> new CollectionLogEntryGoal(itemManager, entry));
            }
        }
    }

    private void configureTarget(boolean preserve) {
        Supplier<Goal> factory = choices.get(subject.getSelectedValue());
        if (factory == null) {
            return;
        }
        Goal goal = factory.get();
        if (!goal.supportsTarget()) {
            return;
        }
        long previous = target.getValue() instanceof Number ? ((Number) target.getValue()).longValue() : 0;
        SpinnerNumberModel model = GoalTargetModels.create(goal);
        if (preserve && previous >= ((Number) model.getMinimum()).longValue() && previous <= ((Number) model.getMaximum()).longValue()) {
            model.setValue(previous);
        }
        target.setModel(model);
        target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
        GoalTargetModels.describe(target);
        presets.removeAll();
        int kind = type.getSelectedIndex();
        long maximum = ((Number) model.getMaximum()).longValue();
        long[] values = kind == 0 || kind == 18 ? new long[]{50, 75, 99}
                : kind == 20 ? new long[]{10_000, 100_000, 1_000_000}
                : kind == 1 || kind == 19 ? new long[]{1_000_000, SkillExperienceGoal.LEVEL_99_EXPERIENCE, maximum}
                : kind == 2 ? new long[]{70, 100, 126}
                : kind == 3 ? new long[]{1000, 1500, maximum}
                : kind == 4 ? new long[]{100_000_000, goal.getProgressMaximum(), maximum}
                : new long[]{100, 500, 1000};
        for (long value : values) {
            JButton preset = new JButton(NumberFormats.target(value));
            preset.setToolTipText(String.format(Locale.ROOT, "Set target to %,d", value));
            preset.addActionListener(event -> target.setValue(value));
            presets.add(preset);
        }
    }

    private void onSubjectSelected(String value) {
        if (!updating) {
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
        Goal sample = choices.get(name).get();
        UiTooltips.set(subject, sample.getDescription().isEmpty() ? "Choose the goal to track on your character." : sample.getDescription());
        String detail = sample.supportsTarget() ? String.format(Locale.ROOT, "Target: %,d", ((Number) target.getValue()).longValue()) : sample.getTitle(null);
        preview.setText("<html><b>" + HtmlUtils.escape(name) + "</b><br>" + detail + "</html>");
    }

    private void setHint(String text) {
        hint.setText("<html><div style='width: 350px'>" + HtmlUtils.escape(text) + "</div></html>");
    }

    public boolean isReady() {
        return type.getSelectedIndex() == 7 || type.getSelectedIndex() == 16 ? itemPicker.hasSelection()
                : type.getSelectedIndex() == 8 || type.getSelectedIndex() == 21 || subject.getSelectedValue() != null;
    }

    private void notifyReadiness() {
        firePropertyChange("ready", !isReady(), isReady());
    }

    public Goal createGoal() throws ParseException {
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
            return new CollectionLogItemGoal(itemPicker.getSelectedItemId(), 1);
        }
        if (type.getSelectedIndex() == 16) {
            target.commitEdit();
            return new PossessedItemGoal( itemPicker.getSelectedItemId(), ((Number) target.getValue()).longValue());
        }
        Supplier<Goal> factory = choices.get(subject.getSelectedValue());
        if (factory == null) {
            throw new IllegalArgumentException("Choose a matching goal from the list first.");
        }
        Goal goal = factory.get();
        if (goal.supportsTarget()) {
            target.commitEdit();
            goal.setTarget(((Number) target.getValue()).longValue());
        }
        return goal;
    }

    public void stopSearch() {
        itemPicker.stopSearch();
    }
    public void loadGoal(Goal goal) {
        if (goal instanceof ChecklistGoal) {
            type.setSelectedIndex(21);
            checklistForm.load((ChecklistGoal) goal);
            repeatForm.setFixedTarget(goal.getProgressMaximum());
            return;
        }
        if (goal instanceof PossessedItemGoal) {
            type.setSelectedIndex(16);
            target.setValue(goal.getProgressMaximum());
            itemPicker.loadItem(((PossessedItemGoal) goal).getItem());
            return;
        }
        if (goal instanceof CollectionLogItemGoal) {
            type.setSelectedIndex(7);
            itemPicker.loadItem(((CollectionLogItemGoal) goal).getItem());
            return;
        }
        if (goal instanceof CustomGoal) {
            type.setSelectedIndex(8);
            customForm.load((CustomGoal) goal);
            return;
        }
        for (int kind = 0; kind < type.getItemCount(); kind++) {
            if (kind == 7 || kind == 8 || kind == 16 || kind == 21) {
                continue;
            }
            type.setSelectedIndex(kind);
            for (Map.Entry<String, Supplier<Goal>> choice : choices.entrySet()) {
                if (GrinderProfile.sameSelection(goal, choice.getValue().get())) {
                    subject.setSelectedValue(choice.getKey());
                    if (goal.supportsTarget()) {
                        target.setValue(goal.getProgressMaximum());
                    }
                    return;
                }
            }
        }
        throw new IllegalArgumentException("This goal's subject is not available in the current client data.");
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(380, size.height);
    }
}
