package com.grinder.repository;

import com.grinder.content.collectionlog.CollectionLogEntry;
import com.grinder.content.collectionlog.CollectionLogHistory;
import com.grinder.content.tracker.TrackerEntry;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementDifficultyGoal;
import com.grinder.content.combatachievement.CombatAchievementTier;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import lombok.Getter;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@Getter
public class GrinderProfile {
    private final long hash;
    private final List<Goal> goals = new ArrayList<>();
    private final transient java.util.Map<Goal, GoalMetadata> metadata = new java.util.IdentityHashMap<>();
    private transient long revision;
    private final java.util.Map<String, ArchivedHistory> archivedHistory = new java.util.LinkedHashMap<>();
    public java.util.Collection<ArchivedHistory> archivedHistory() {
        return Collections.unmodifiableCollection(archivedHistory.values());
    }
    public void restoreArchive(ArchivedHistory history) {
        if (history == null || history.title == null || history.title.length() > 300) {
            throw new IllegalArgumentException("Invalid archived history");
        }
        GoalMetadata.validated(history.metadata);
        if (goals.stream().noneMatch(goal -> metadata(goal).getId().equals(history.metadata.getId()))) {
            archivedHistory.putIfAbsent(history.metadata.getId(), history);
        }
        while (archivedHistory.size() > 500) {
            archivedHistory.remove(archivedHistory.keySet().iterator().next());
        }
    }
    private void archive(Goal goal) {
        GoalMetadata saved = metadata(goal).copy();
        if (saved.getCompletions().isEmpty() && saved.recurring().getHistory().isEmpty() && saved.recurring().getBaselineAt() == 0) {
            return;
        }
        saved.recurring().stop(System.currentTimeMillis(), "Goal archived · last observed");
        archivedHistory.put(saved.getId(), new ArchivedHistory(goal.getTitle(null), saved));
        while (archivedHistory.size() > 500) {
            archivedHistory.remove(archivedHistory.keySet().iterator().next());
        }
    }
    public long revision() {
        return revision;
    }
    public void touch() {
        revision++;
    }
    public java.util.function.Consumer<Goal> snapshotEdit(Goal original) {
        GoalMetadata saved = metadata(original).copy();
        RepeatSchedule previousSchedule = saved.recurring().getSchedule();
        long undoWindow = previousSchedule.start(System.currentTimeMillis());
        long target = original.getProgressMaximum();
        CustomGoal custom = original instanceof CustomGoal ? (CustomGoal) original : null;
        CustomGoal manual = custom == null ? null : new CustomGoal(custom.getId(), custom.getTitle(null), custom.getDescription(), custom.getCategory(), custom.getProgressCurrent(), target);
        com.grinder.content.goal.impl.ChecklistGoal checklist = original instanceof com.grinder.content.goal.impl.ChecklistGoal ? (com.grinder.content.goal.impl.ChecklistGoal) original : null;
        boolean[] savedChecks = checklist == null ? null : checklist.getChecks();
        return replacement -> {
            if (previousSchedule != RepeatSchedule.NONE && previousSchedule.start(System.currentTimeMillis()) != undoWindow) {
                throw new IllegalStateException("The previous period has reset.");
            }
            int index = goals.indexOf(replacement);
            if (index < 0) {
                throw new IllegalStateException("The edited goal no longer exists.");
            }
            if (custom != null) {
                custom.update(manual.getTitle(null), manual.getDescription(), manual.getCategory(), manual.getProgressCurrent(), manual.getProgressMaximum());
            } else if (original.supportsTarget()) {
                original.setTarget(target);
            }
            if (checklist != null) {
                checklist.restoreChecks(savedChecks);
            }
            goals.set(index, original);
            if (original != replacement) {
                metadata.remove(replacement);
            }
            metadata.put(original, saved);
            archivedHistory.remove(saved.getId());
            attachCollectionData(original);
            touch();
        };
    }
    public GoalMetadata metadata(Goal goal) {
        return metadata.computeIfAbsent(goal, ignored -> new GoalMetadata());
    }
    public void restoreMetadata(Goal goal, GoalMetadata saved) {
        metadata.put(goal, GoalMetadata.validated(saved));
    }
    public void observeRecurring(net.runelite.api.Client client, long now) {
        for (Goal goal : goals) {
            RecurringGoal recurring = metadata(goal).recurring();
            if (recurring.isActive() && RecurringGoal.supports(goal)) {
                goal.refresh(client);
                recurring.observe(goal, now);
            }
        }
        updateLinkedGoals(client != null && client.getGameState() == net.runelite.api.GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == hash);
    }
    public void updateLinkedGoals(boolean loggedIn) {
        java.util.Map<String, Goal> sources = new java.util.HashMap<>();
        for (Goal goal : goals) {
            if (!(goal instanceof com.grinder.content.goal.impl.LinkedGoal)) {
                sources.put(metadata(goal).getId(), goal);
            }
        }
        for (Goal goal : goals) {
            if (goal instanceof com.grinder.content.goal.impl.LinkedGoal) {
                ((com.grinder.content.goal.impl.LinkedGoal) goal).update(this, loggedIn, sources);
            }
        }
    }
    private final List<Item> items = new ArrayList<>();
    private final CollectionLogHistory collectionLogHistory = new CollectionLogHistory();
    private transient ItemManager itemManager;
    private final transient com.grinder.content.storage.PlayerItemStorage playerStorage = new com.grinder.content.storage.PlayerItemStorage();

    public com.grinder.content.storage.PlayerItemStorage getPlayerStorage() {
        return playerStorage;
    }

    public GrinderProfile(long hash) {
        this(hash, null);
    }

    public GrinderProfile(long hash, ItemManager itemManager) {
        this.hash = hash;
        initializeGoals(itemManager);
    }

    private void initializeGoals(ItemManager itemManager) {
        ensureDefaultGoals(itemManager);
    }

    public void ensureDefaultGoals(ItemManager itemManager) {
        for (CollectionLogEntry entry : CollectionLogEntry.values()) {
            if (!hasCollectionEntry(entry)) {
                goals.add(new CollectionLogEntryGoal(itemManager, entry));
            }
        }

        for (TrackerEntry entry : TrackerEntry.values()) {
            if (!hasTracker(entry)) {
                goals.add(new TrackerGoal(entry, 5000));
            }
        }

        for (Skill skill : Skill.values()) {
            if (!hasSkillGoal(skill)) {
                goals.add(new SkillLevelGoal(skill));
            }

            if (!hasExperienceGoal(skill)) {
                goals.add(new SkillExperienceGoal(skill));
            }
        }

        if (goals.stream().noneMatch(goal -> goal instanceof CombatLevelGoal)) {
            goals.add(new CombatLevelGoal());
        }

        if (goals.stream().noneMatch(goal -> goal instanceof TotalLevelGoal)) {
            goals.add(new TotalLevelGoal());
        }

        if (goals.stream().noneMatch(goal -> goal instanceof TotalExperienceGoal && metadata(goal).recurring().getSchedule() == RepeatSchedule.NONE)) {
            goals.add(new TotalExperienceGoal());
        }

        addDefaultCollectionItem(ItemID.DRAGON_PICKAXE);
        add(new com.grinder.content.goal.impl.level.BaseLevelGoal());
        add(new com.grinder.content.goal.impl.experience.BaseExperienceGoal());
        add(new com.grinder.content.goal.impl.experience.SessionExperienceGoal(Skill.WOODCUTTING));
        Goal sampleSource = findMatchingGoal(new SkillLevelGoal(Skill.ATTACK));
        String linkedExampleId = java.util.UUID.nameUUIDFromBytes("grinder-default-linked-goal".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        add(new com.grinder.content.goal.impl.LinkedGoal(linkedExampleId, "Example: Attack milestones", "A ladder example. Edit its source and milestones, or delete it.", com.grinder.content.goal.impl.LinkedGoal.Mode.MILESTONES, java.util.Collections.singletonList(metadata(sampleSource).getId()), new long[]{50, 75, 99}, 1));
        addDefaultCollectionItem(ItemID.TWISTED_BOW);
        addDefaultCollectionItem(ItemID.JAR_OF_SPIRITS);
        add(new PossessedItemGoal(ItemID.COINS, 1_500_000_000L));
        add(new PossessedItemGoal(ItemID.DRAGON_PICKAXE, 1));
        add(new CombatAchievementBossGoal("Barrows"));
        add(new CombatAchievementDifficultyGoal(CombatAchievementTier.EASY));
        add(new com.grinder.content.goal.impl.combatachievement.CombatAchievementTaskGoal(0, "Combat task 0", "The native task name, requirements and completion state load when logged in."));
        String customExampleId = java.util.UUID.nameUUIDFromBytes("grinder-default-custom-goal".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        add(new CustomGoal(customExampleId, "Example: complete 10 personal challenges", "A manual tracker example. Edit its title, description, target and progress to suit your own goal.", GoalCategory.OTHER, 0, 10));
        String checklistExampleId = java.util.UUID.nameUUIDFromBytes("grinder-default-checklist".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        add(new com.grinder.content.goal.impl.ChecklistGoal(checklistExampleId, "Example: personal routine", "Check steps manually. Add a repeat schedule to reset and archive this routine each period.", java.util.Arrays.asList("Choose today's activity", "Prepare supplies", "Finish the activity"), new boolean[3]));
        add(new com.grinder.content.goal.impl.quest.QuestGoal(net.runelite.api.Quest.DRAGON_SLAYER_I));
        add(new com.grinder.content.goal.impl.quest.QuestSeriesGoal(com.grinder.content.quest.QuestSeries.ELF));
        add(new com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierAreaGoal( com.grinder.content.diary.AchievementDiaryArea.LUMBRIDGE, com.grinder.content.diary.DiaryTier.EASY));
        add(new com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierGoal(com.grinder.content.diary.DiaryTier.EASY));
        add(new com.grinder.content.goal.impl.combatachievement.CombatAchievementTierGoal( com.grinder.content.combatachievement.CombatAchievementTier.EASY));
        setItemManager(itemManager);
    }

    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
        for (Goal goal : goals) {
            attachCollectionData(goal);
        }
    }

    private void attachCollectionData(Goal goal) {
        if (goal instanceof TrackerGoal) {
            ((TrackerGoal) goal).setItemManager(itemManager);
        }
        if (goal instanceof com.grinder.content.goal.impl.item.PossessedItemGoal) {
            ((com.grinder.content.goal.impl.item.PossessedItemGoal) goal).attach(playerStorage, itemManager);
        }
        if (goal instanceof CollectionLogEntryGoal) {
            CollectionLogEntryGoal entry = (CollectionLogEntryGoal) goal;
            entry.setItemManager(itemManager);
            entry.setHistory(collectionLogHistory);
        } else if (goal instanceof CollectionLogItemGoal) {
            CollectionLogItemGoal item = (CollectionLogItemGoal) goal;
            item.setItemManager(itemManager);
            item.setHistory(collectionLogHistory);
        }
    }

    public boolean add(Goal goal) {
        return add(goal, new GoalMetadata());
    }

    public boolean add(Goal goal, GoalMetadata details) {
        Objects.requireNonNull(goal, "goal");
        GoalMetadata.validated(details);
        if (findMatchingGoal(goal, details.recurring().getSchedule()) != null) {
            return false;
        }
        attachCollectionData(goal);
        metadata.put(goal, details);
        archivedHistory.remove(details.getId());
        touch();
        return goals.add(goal);
    }

    public Goal findMatchingGoal(Goal candidate) {
        return findMatchingGoal(candidate, RepeatSchedule.NONE);
    }

    public Goal findMatchingGoal(Goal candidate, RepeatSchedule schedule) {
        return goals.stream().filter(goal -> sameGoal(goal, candidate) && metadata(goal).recurring().getSchedule() == schedule).findFirst().orElse(null);
    }

    private static boolean sameGoal(Goal left, Goal right) {
        if (left.getClass() != right.getClass()) {
            return false;
        }
        if (left instanceof CombatAchievementBossGoal) {
            return ((CombatAchievementBossGoal) left).matches((CombatAchievementBossGoal) right);
        }
        if (left instanceof com.grinder.content.goal.impl.item.PossessedItemGoal) {
            return ((com.grinder.content.goal.impl.item.PossessedItemGoal) left).getItem()
            == ((com.grinder.content.goal.impl.item.PossessedItemGoal) right).getItem();
        }
        if (left instanceof com.grinder.content.goal.ClientCompletionGoal) {
            return ((com.grinder.content.goal.ClientCompletionGoal) left).selectionKey()
                    .equals(((com.grinder.content.goal.ClientCompletionGoal) right).selectionKey());
        }
        if (left instanceof CustomGoal) {
            return ((CustomGoal) left).getId().equals(((CustomGoal) right).getId());
        }
        if (left instanceof com.grinder.content.goal.impl.ChecklistGoal) {
            return ((com.grinder.content.goal.impl.ChecklistGoal) left).getId().equals(((com.grinder.content.goal.impl.ChecklistGoal) right).getId());
        }
        if (left instanceof com.grinder.content.goal.impl.LinkedGoal) {
            return ((com.grinder.content.goal.impl.LinkedGoal) left).getId().equals(((com.grinder.content.goal.impl.LinkedGoal) right).getId());
        }
        if (left instanceof SkillLevelGoal) {
            return ((SkillLevelGoal) left).getSkill() == ((SkillLevelGoal) right).getSkill();
        }
        if (left instanceof SkillExperienceGoal) {
            return ((SkillExperienceGoal) left).getSkill() == ((SkillExperienceGoal) right).getSkill();
        }
        if (left instanceof com.grinder.content.goal.impl.experience.SessionExperienceGoal) {
            com.grinder.content.goal.impl.experience.SessionExperienceGoal first = (com.grinder.content.goal.impl.experience.SessionExperienceGoal) left;
            com.grinder.content.goal.impl.experience.SessionExperienceGoal second = (com.grinder.content.goal.impl.experience.SessionExperienceGoal) right;
            return first.isTotal() == second.isTotal() && first.getSkill() == second.getSkill();
        }
        if (left instanceof TrackerGoal) {
            return ((TrackerGoal) left).getTracker() == ((TrackerGoal) right).getTracker();
        }
        if (left instanceof CollectionLogEntryGoal) {
            return ((CollectionLogEntryGoal) left).getPageStruct() == ((CollectionLogEntryGoal) right).getPageStruct();
        }
        if (left instanceof CollectionLogItemGoal) {
            return ((CollectionLogItemGoal) left).getItem() == ((CollectionLogItemGoal) right).getItem();
        }
        return true;
    }
    public static boolean sameSelection(Goal left, Goal right) {
        return sameGoal(left, right);
    }
    public boolean replaceGoal(Goal original, Goal replacement) {
        return replaceGoal(original, replacement, metadata(original).recurring().getSchedule());
    }
    public boolean replaceGoal(Goal original, Goal replacement, RepeatSchedule schedule) {
        if (original == replacement) {
            return goals.contains(original);
        }
        int index = goals.indexOf(original);
        if (index < 0) {
            return false;
        }
        for (Goal goal : goals) {
            if (goal != original && sameGoal(goal, replacement) && metadata(goal).recurring().getSchedule() == schedule) {
                throw new IllegalArgumentException("That tracker and schedule are already tracked. Edit the existing goal instead.");
            }
        }
        GoalMetadata old = metadata(original);
        if (!sameGoal(original, replacement)) {
            archive(original);
        }
        GoalMetadata next = sameGoal(original, replacement) ? old : new GoalMetadata();
        next.setPinned(old.isPinned());
        attachCollectionData(replacement);
        goals.set(index, replacement);
        metadata.put(replacement, next);
        metadata.remove(original);
        touch();
        return true;
    }

    public boolean remove(Goal goal) {
        if (goals.contains(goal)) {
            archive(goal);
        }
        boolean removed = goals.remove(goal);
        if (removed) {
            touch();
        }
        return removed;
    }
    public int mergeBackup(GrinderProfile backup) {
        if (backup.hash != hash) {
            throw new IllegalArgumentException("Backup must be mapped to this account before merging.");
        }
        int added = 0;
        java.util.Map<String, String> mapping = new java.util.HashMap<>();
        for (Goal goal : backup.getGoals()) {
            Goal existing = findMatchingGoal(goal, backup.metadata(goal).recurring().getSchedule());
            mapping.put(backup.metadata(goal).getId(), existing == null ? backup.metadata(goal).getId() : metadata(existing).getId());
        }
        for (Goal goal : backup.getGoals()) {
            if (goal instanceof com.grinder.content.goal.impl.LinkedGoal && findMatchingGoal(goal) == null) {
                ((com.grinder.content.goal.impl.LinkedGoal) goal).remapSources(mapping);
            }
            if (add(goal, backup.metadata(goal))) {
                added++;
            }
        }
        backup.collectionLogHistory.snapshot().forEach(collectionLogHistory::record);
        backup.archivedHistory().forEach(this::restoreArchive);
        setItemManager(itemManager);
        return added;
    }

    public List<Goal> getGoals() {
        return Collections.unmodifiableList(goals);
    }

    public void replace(List<Goal> goals) {
        this.goals.clear();
        this.goals.addAll(goals);
        this.goals.forEach(this::attachCollectionData);
    }

    private boolean hasCollectionEntry(CollectionLogEntry entry) {
        return goals.stream().anyMatch(goal -> goal instanceof CollectionLogEntryGoal && ((CollectionLogEntryGoal) goal).getEntry() == entry);
    }

    private boolean hasTracker(TrackerEntry entry) {
        return goals.stream().anyMatch(goal -> goal instanceof TrackerGoal && ((TrackerGoal) goal).getTracker() == entry && metadata(goal).recurring().getSchedule() == RepeatSchedule.NONE);
    }

    private boolean hasSkillGoal(Skill skill) {
        return goals.stream().anyMatch(goal -> goal instanceof SkillLevelGoal && ((SkillLevelGoal) goal).getSkill() == skill);
    }

    private boolean hasExperienceGoal(Skill skill) {
        return goals.stream().anyMatch(goal -> goal instanceof SkillExperienceGoal && ((SkillExperienceGoal) goal).getSkill() == skill && metadata(goal).recurring().getSchedule() == RepeatSchedule.NONE);
    }

    private void addDefaultCollectionItem(int itemId) {
        boolean exists = goals.stream().anyMatch(goal -> goal instanceof CollectionLogItemGoal && ((CollectionLogItemGoal) goal).getItem() == itemId);
        if (!exists) {
            goals.add(new CollectionLogItemGoal(itemId, 1));
        }
    }

    public void onContainerOpen(ItemContainer container) {
        items.clear();

        if (container == null) {
            return;
        }

        for (Item item : container.getItems()) {
            if (item != null && item.getId() > 0 && item.getQuantity() > 0) {
                items.add(item);
            }
        }
    }
    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    public static final class ArchivedHistory {
        private final String title;
        private final GoalMetadata metadata;
        private ArchivedHistory(String title, GoalMetadata metadata) {
            this.title = title;
            this.metadata = metadata;
        }
        public String getTitle() {
            return title;
        }
        public GoalMetadata getMetadata() {
            return metadata;
        }
    }
}
