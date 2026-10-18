package com.grinder.ui;

import com.grinder.content.CollectionLogEntry;
import com.grinder.content.TrackerEntry;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogItemGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.*;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class GoalEditorPanel extends JPanel
{
    private final JComboBox<String> type = new JComboBox<>(new String[]{
            "Skill level", "Skill experience", "Combat level", "Total level",
            "Total experience", "Activity tracker", "Collection log", "Collection item", "Custom goal",
            "Quest", "Quest series", "Diary region & tier", "Diary tier (all regions)",
            "Combat achievements: boss", "Combat achievements: reward tier", "Combat achievements: difficulty tasks",
            "Owned item quantity"});
    private final CustomGoalForm customForm = new CustomGoalForm();
    private final ItemPickerPanel itemPicker;
    private final JComboBox<String> subject = new JComboBox<>();
    private final JSpinner target = new JSpinner();
    private final JPanel body = new JPanel();
    private final JLabel hint = new JLabel();
    private final JLabel preview = new JLabel();
    private final JPanel presets = new JPanel(new GridLayout(1, 3, 6, 0));
    private final Map<String, Supplier<Goal>> choices = new LinkedHashMap<>();
    private final ItemManager itemManager;
    private boolean updating;
    private final Map<Integer, Map<String, Supplier<Goal>>> completionChoices;

    public GoalEditorPanel(ItemManager itemManager, ItemLookup searchService)
    {
        this(itemManager, searchService, com.grinder.content.CompletionGoalChoices.create(null));
    }

    public GoalEditorPanel(ItemManager itemManager, ItemLookup searchService,
            Map<Integer, Map<String, Supplier<Goal>>> completionChoices)
    {
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
        subject.addActionListener(event -> {
            if (!updating) { configureTarget(true); updatePreview(); }
        });
        target.addChangeListener(event -> updatePreview());
        rebuild();
    }

    private JPanel field(String title, JComponent control)
    {
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

    private void rebuild()
    {
        itemPicker.stopSearch();
        body.removeAll();
        choices.clear();
        int kind = type.getSelectedIndex();
        if (kind == 8)
        {
            customForm.setAlignmentX(LEFT_ALIGNMENT);
            body.add(customForm);
            setHint("Write your own goal. You control its progress and can edit its details later.");
        }
        else if (kind == 7 || kind == 16)
        {
            itemPicker.setAlignmentX(LEFT_ALIGNMENT);
            body.add(itemPicker);
            itemPicker.resumeSearch();
            if (kind == 16)
            {
                body.add(Box.createVerticalStrut(12));
                target.setModel(targetModel(new com.grinder.content.goal.impl.item.PossesedItemGoal(
                        net.runelite.api.gameval.ItemID.COINS)));
                target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
                describeTarget(target);
                JPanel quantity = field("Target quantity", target);
                presets.removeAll();
                for (long value : new long[]{1_000_000L, 100_000_000L, 1_500_000_000L})
                {
                    JButton preset = new JButton(shortNumber(value));
                    preset.setToolTipText(String.format(Locale.ROOT, "Set target quantity to %,d", value));
                    preset.addActionListener(event -> target.setValue(value));
                    presets.add(preset);
                }
                quantity.add(presets, BorderLayout.SOUTH);
                body.add(quantity);
            }
            setHint(kind == 16
                    ? "Counts the selected item across known personal storage, including coins in supported coffers. Open your bank and other storage to load them. This is item quantity, not bank value. Shared storage and unexposed locations are excluded."
                    : "Tracks collection-log unlocks and first-seen dates, not inventory or bank quantities. Choose an item that belongs to the collection log.");
        }
        else
        {
            populateChoices(kind);
            updating = true;
            subject.setModel(new DefaultComboBoxModel<>(choices.keySet().toArray(new String[0])));
            updating = false;
            if (kind < 2 || kind == 5 || kind == 6 || kind >= 9)
            {
                body.add(field(kind >= 9 ? "Track" : kind == 5 ? "Activity" : kind == 6 ? "Collection-log page" : "Skill", subject));
                body.add(Box.createVerticalStrut(12));
            }
            Goal sample = choices.isEmpty() ? null : choices.values().iterator().next().get();
            if (sample != null && sample.supportsTarget())
            {
                configureTarget(false);
                JPanel targetField = field(kind == 1 || kind == 4 ? "Target experience"
                        : kind == 5 ? "Target completions" : "Target level", target);
                targetField.add(presets, BorderLayout.SOUTH);
                presets.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
                body.add(targetField);
                body.add(Box.createVerticalStrut(12));
            }
            updatePreview();
            body.add(preview);
            setHint(kind >= 9 ? (choices.isEmpty() ? "Client data is not ready. Log in and reopen this dialog."
                    : "Progress reads your logged-in character automatically. Completion goals have no manual target.")
                    : kind == 6 ? "Open your in-game collection log to refresh unlocks. Each item keeps its first-seen date."
                    : kind == 5 ? "Progress reads your character's activity count automatically."
                    : kind == 1 || kind == 4 ? "Progress reads your character's real experience automatically."
                    : "Uses real levels. Temporary boosts do not advance level goals.");
        }
        body.add(Box.createVerticalStrut(10));
        body.add(hint);
        notifyReadiness();
        revalidate();
        repaint();
        firePropertyChange("formLayout", false, true);
    }

    private void populateChoices(int kind)
    {
        if (kind >= 9)
        {
            choices.putAll(completionChoices.getOrDefault(kind, java.util.Collections.emptyMap()));
            return;
        }
        if (kind < 2)
        {
            for (Skill skill : Skill.values())
            {
                choices.put(skill.getName(), kind == 0
                        ? () -> new SkillLevelGoal(skill) : () -> new SkillExperienceGoal(skill));
            }
        }
        else if (kind == 2) { choices.put("Combat level", CombatLevelGoal::new); }
        else if (kind == 3) { choices.put("Total level", TotalLevelGoal::new); }
        else if (kind == 4) { choices.put("Total experience", TotalExperienceGoal::new); }
        else if (kind == 5)
        {
            for (TrackerEntry entry : TrackerEntry.values())
            {
                choices.put(entry.getDisplayName(), () -> new TrackerGoal(entry, 5000));
            }
        }
        else
        {
            for (CollectionLogEntry entry : CollectionLogEntry.values())
            {
                String name = entry.name().replace('_', ' ').toLowerCase(Locale.ROOT);
                choices.put(Character.toUpperCase(name.charAt(0)) + name.substring(1),
                        () -> new CollectionLogEntryGoal(itemManager, entry));
            }
        }
    }

    private void configureTarget(boolean preserve)
    {
        Supplier<Goal> factory = choices.get(subject.getSelectedItem());
        if (factory == null) { return; }
        Goal goal = factory.get();
        if (!goal.supportsTarget()) { return; }
        long previous = target.getValue() instanceof Number ? ((Number) target.getValue()).longValue() : 0;
        SpinnerNumberModel model = targetModel(goal);
        if (preserve && previous >= ((Number) model.getMinimum()).longValue()
                && previous <= ((Number) model.getMaximum()).longValue())
        {
            model.setValue(previous);
        }
        target.setModel(model);
        target.setEditor(new JSpinner.NumberEditor(target, "#,##0"));
        describeTarget(target);
        presets.removeAll();
        int kind = type.getSelectedIndex();
        long maximum = ((Number) model.getMaximum()).longValue();
        long[] values = kind == 0 ? new long[]{50, 75, 99}
                : kind == 1 ? new long[]{1_000_000, SkillExperienceGoal.LEVEL_99_EXPERIENCE, maximum}
                : kind == 2 ? new long[]{70, 100, 126}
                : kind == 3 ? new long[]{1000, 1500, maximum}
                : kind == 4 ? new long[]{100_000_000, goal.getProgressMaximum(), maximum}
                : new long[]{100, 500, 1000};
        for (long value : values)
        {
            JButton preset = new JButton(shortNumber(value));
            preset.setToolTipText(String.format(Locale.ROOT, "Set target to %,d", value));
            preset.addActionListener(event -> target.setValue(value));
            presets.add(preset);
        }
    }

    private void updatePreview()
    {
        if (choices.isEmpty()) { preview.setText("No choices available"); return; }
        if (type.getSelectedIndex() == 7 || type.getSelectedIndex() == 8) { return; }
        String name = (String) subject.getSelectedItem();
        if (name == null) { return; }
        Goal sample = choices.get(name).get();
        UiTooltips.set(subject, sample.getDescription().isEmpty()
                ? "Choose the goal to track on your character." : sample.getDescription());
        String detail = sample.supportsTarget() ? String.format(Locale.ROOT,
                "Target: %,d", ((Number) target.getValue()).longValue()) : sample.getTitle(null);
        preview.setText("<html><b>" + escape(name) + "</b><br>" + detail + "</html>");
    }

    private void setHint(String text)
    {
        hint.setText("<html><div style='width: 350px'>" + escape(text) + "</div></html>");
    }

    public boolean isReady()
    {
        return type.getSelectedIndex() == 7 || type.getSelectedIndex() == 16 ? itemPicker.hasSelection()
                : type.getSelectedIndex() == 8 || !choices.isEmpty();
    }

    private void notifyReadiness() { firePropertyChange("ready", !isReady(), isReady()); }

    public Goal createGoal() throws java.text.ParseException
    {
        if (type.getSelectedIndex() == 8) { return customForm.read(); }
        if (type.getSelectedIndex() == 7) { return new CollectionLogItemGoal(itemPicker.getSelectedItemId(), 1); }
        if (type.getSelectedIndex() == 16)
        {
            target.commitEdit();
            return new com.grinder.content.goal.impl.item.PossesedItemGoal(
                    itemPicker.getSelectedItemId(), ((Number) target.getValue()).longValue());
        }
        Goal goal = choices.get(subject.getSelectedItem()).get();
        if (goal.supportsTarget())
        {
            target.commitEdit();
            goal.setTarget(((Number) target.getValue()).longValue());
        }
        return goal;
    }

    public void stopSearch() { itemPicker.stopSearch(); }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension size = super.getPreferredSize();
        return new Dimension(380, size.height);
    }

    public static SpinnerNumberModel targetModel(Goal goal)
    {
        long minimum = goal instanceof CombatLevelGoal ? 3 : 1;
        long maximum = goal instanceof SkillLevelGoal ? 99
                : goal instanceof CombatLevelGoal ? 126
                : goal instanceof TotalLevelGoal ? Skill.values().length * 99L
                : goal instanceof SkillExperienceGoal ? 200_000_000L
                : goal instanceof TotalExperienceGoal ? Skill.values().length * 200_000_000L
                : goal instanceof com.grinder.content.goal.impl.item.PossesedItemGoal
                        ? com.grinder.content.goal.impl.item.PossesedItemGoal.MAX_TARGET : Integer.MAX_VALUE;
        return new SpinnerNumberModel(Long.valueOf(goal.getProgressMaximum()),
                Long.valueOf(minimum), Long.valueOf(maximum), Long.valueOf(1));
    }

    public static void describeTarget(JSpinner spinner)
    {
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        UiTooltips.set(spinner, String.format(Locale.ROOT,
                "Target range: %,d to %,d. Type a number or use the arrows. This changes your goal, not your game stats.",
                model.getMinimum(), model.getMaximum()));
    }

    private static String shortNumber(long value)
    {
        return value >= 1_000_000_000L ? String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000.0)
                : value >= 1_000_000 ? String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0)
                : String.format(Locale.ROOT, "%,d", value);
    }

    private static String escape(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
