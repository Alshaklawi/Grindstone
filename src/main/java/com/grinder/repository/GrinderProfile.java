package com.grinder.repository;

import com.grinder.content.collectionlog.CollectionLogEntry;
import com.grinder.content.collectionlog.CollectionLogHistory;
import com.grinder.content.combatachievement.CombatAchievementTier;
import com.grinder.content.diary.AchievementDiaryArea;
import com.grinder.content.diary.DiaryTier;
import com.grinder.content.goal.ClientCompletionGoal;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.impl.ChecklistGoal;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.LinkedGoal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierAreaGoal;
import com.grinder.content.goal.impl.achievementdiary.AchievementDiaryTierGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementBossGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementDifficultyGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTaskGoal;
import com.grinder.content.goal.impl.combatachievement.CombatAchievementTierGoal;
import com.grinder.content.goal.impl.experience.BaseExperienceGoal;
import com.grinder.content.goal.impl.experience.SessionExperienceGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.item.PossessedItemGoal;
import com.grinder.content.goal.impl.level.BaseLevelGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.goal.impl.quest.QuestGoal;
import com.grinder.content.goal.impl.quest.QuestSeriesGoal;
import com.grinder.content.quest.QuestSeries;
import com.grinder.content.storage.PlayerItemStorage;
import com.grinder.content.tracker.TrackerEntry;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
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
    private final transient Map<Goal, GoalMetadata> metadata = new IdentityHashMap<>();
    private transient long revision;
    private final Map<String, ArchivedHistory> archivedHistory = new LinkedHashMap<>();
    public Collection<ArchivedHistory> archivedHistory() {
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
    public void restoreArchiveSnapshot(Collection<ArchivedHistory> histories) {
        if (histories.size() > 500) {
            throw new IllegalArgumentException("Too many archived histories.");
        }
        for (ArchivedHistory history : histories) {
            if (history == null || history.title == null || history.title.length() > 300) {
                throw new IllegalArgumentException("Invalid archived history.");
            }
            GoalMetadata.validated(history.metadata);
        }
        archivedHistory.clear();
        histories.forEach(this::restoreArchive);
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
    public Consumer<Goal> snapshotEdit(Goal original) {
        GoalMetadata saved = metadata(original).copy();
        RepeatSchedule previousSchedule = saved.recurring().getSchedule();
        long undoWindow = previousSchedule.start(System.currentTimeMillis());
        long target = original.getProgressMaximum();
        CustomGoal custom = original instanceof CustomGoal ? (CustomGoal) original : null;
        CustomGoal manual = custom == null ? null : new CustomGoal(custom.getId(), custom.getTitle(null), custom.getDescription(), custom.getCategory(), custom.getProgressCurrent(), target);
        ChecklistGoal checklist = original instanceof ChecklistGoal ? (ChecklistGoal) original : null;
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
    public void observeRecurring(Client client, long now) {
        for (Goal goal : goals) {
            RecurringGoal recurring = metadata(goal).recurring();
            if (recurring.isActive() && RecurringGoal.supports(goal)) {
                goal.refresh(client);
                recurring.observe(goal, now);
            }
        }
        updateLinkedGoals(client != null && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == hash);
    }
    public void updateLinkedGoals(boolean loggedIn) {
        Map<String, Goal> sources = new HashMap<>();
        for (Goal goal : goals) {
            if (!(goal instanceof LinkedGoal)) {
                sources.put(metadata(goal).getId(), goal);
            }
        }
        for (Goal goal : goals) {
            if (goal instanceof LinkedGoal) {
                ((LinkedGoal) goal).update(this, loggedIn, sources);
            }
        }
    }
    private final List<Item> items = new ArrayList<>();
    private final CollectionLogHistory collectionLogHistory = new CollectionLogHistory();
    private transient ItemManager itemManager;
    private final transient PlayerItemStorage playerStorage = new PlayerItemStorage();

    public PlayerItemStorage getPlayerStorage() {
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
        Set<Goal> existingDefaults = new HashSet<>(goals);
        for (CollectionLogEntry entry : CollectionLogEntry.values()) {
            if (!hasCollectionEntry(entry)) {
                goals.add(new CollectionLogEntryGoal(itemManager, entry));
            }
        }

        for (TrackerEntry entry : TrackerEntry.values()) {
            if (!hasTracker(entry)) {
                goals.add(new TrackerGoal(entry, entry.getDefaultTarget()));
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
        add(new BaseLevelGoal());
        add(new BaseExperienceGoal());
        add(new SessionExperienceGoal(Skill.WOODCUTTING));
        Goal sampleSource = findMatchingGoal(new SkillLevelGoal(Skill.ATTACK));
        String linkedExampleId = UUID.nameUUIDFromBytes("grinder-default-linked-goal".getBytes(StandardCharsets.UTF_8)).toString();
        add(new LinkedGoal(linkedExampleId, "Example: Attack milestones", "A ladder example. Edit its source and milestones, or delete it.", LinkedGoal.Mode.MILESTONES, Collections.singletonList(metadata(sampleSource).getId()), new long[]{50, 75, 99}, 1));
        addDefaultCollectionItem(ItemID.TWISTED_BOW);
        addDefaultCollectionItem(ItemID.JAR_OF_SPIRITS);
        add(new PossessedItemGoal(ItemID.COINS, 1_500_000_000L));
        add(new PossessedItemGoal(ItemID.DRAGON_PICKAXE, 1));
        add(new CombatAchievementBossGoal("Barrows"));
        add(new CombatAchievementDifficultyGoal(CombatAchievementTier.EASY));
        add(new CombatAchievementTaskGoal(0, "Combat task 0", "The native task name, requirements and completion state load when logged in."));
        String customExampleId = UUID.nameUUIDFromBytes("grinder-default-custom-goal".getBytes(StandardCharsets.UTF_8)).toString();
        add(new CustomGoal(customExampleId, "Example: complete 10 personal challenges", "A manual tracker example. Edit its title, description, target and progress to suit your own goal.", GoalCategory.OTHER, 0, 10));
        String checklistExampleId = UUID.nameUUIDFromBytes("grinder-default-checklist".getBytes(StandardCharsets.UTF_8)).toString();
        add(new ChecklistGoal(checklistExampleId, "Example: personal routine", "Check steps manually. Add a repeat schedule to reset and archive this routine each period.", Arrays.asList("Choose today's activity", "Prepare supplies", "Finish the activity"), new boolean[3]));
        add(new QuestGoal(Quest.DRAGON_SLAYER_I));
        add(new QuestSeriesGoal(QuestSeries.ELF));
        add(new AchievementDiaryTierAreaGoal( AchievementDiaryArea.LUMBRIDGE, DiaryTier.EASY));
        add(new AchievementDiaryTierGoal(DiaryTier.EASY));
        add(new CombatAchievementTierGoal( CombatAchievementTier.EASY));
        GoalExamples.addLinkedExamples(this);
        for (Goal goal : goals) {
            if (!existingDefaults.contains(goal) && (goal.getTitle(null).startsWith("Example:") || goal.getTitle(null).equals("Combat task 0"))) {
                metadata(goal).setSample(true);
            }
        }
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
        if (goal instanceof PossessedItemGoal) {
            ((PossessedItemGoal) goal).attach(playerStorage, itemManager);
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
        if (left instanceof PossessedItemGoal) {
            return ((PossessedItemGoal) left).getItem()
            == ((PossessedItemGoal) right).getItem();
        }
        if (left instanceof ClientCompletionGoal) {
            return ((ClientCompletionGoal) left).selectionKey()
                    .equals(((ClientCompletionGoal) right).selectionKey());
        }
        if (left instanceof CustomGoal) {
            return ((CustomGoal) left).getId().equals(((CustomGoal) right).getId());
        }
        if (left instanceof ChecklistGoal) {
            return ((ChecklistGoal) left).getId().equals(((ChecklistGoal) right).getId());
        }
        if (left instanceof LinkedGoal) {
            return ((LinkedGoal) left).getId().equals(((LinkedGoal) right).getId());
        }
        if (left instanceof SkillLevelGoal) {
            return ((SkillLevelGoal) left).getSkill() == ((SkillLevelGoal) right).getSkill();
        }
        if (left instanceof SkillExperienceGoal) {
            return ((SkillExperienceGoal) left).getSkill() == ((SkillExperienceGoal) right).getSkill();
        }
        if (left instanceof SessionExperienceGoal) {
            SessionExperienceGoal first = (SessionExperienceGoal) left;
            SessionExperienceGoal second = (SessionExperienceGoal) right;
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
        if (sameGoal(original, replacement) && original instanceof SessionExperienceGoal) {
            ((SessionExperienceGoal) replacement).copyReadingsFrom((SessionExperienceGoal) original);
        }
        next.setPinned(old.isPinned());
        attachCollectionData(replacement);
        goals.set(index, replacement);
        metadata.put(replacement, next);
        metadata.remove(original);
        touch();
        return true;
    }

    public void validateReferences() {
        Map<String, Goal> byId = new HashMap<>();
        for (Goal goal : goals) {
            if (byId.put(metadata(goal).getId(), goal) != null) {
                throw new IllegalArgumentException("Duplicate goal metadata ID.");
            }
        }
        for (Goal goal : goals) {
            if (goal instanceof LinkedGoal) {
                for (String source : ((LinkedGoal) goal).getSources()) {
                    if (byId.get(source) instanceof LinkedGoal) {
                        throw new IllegalArgumentException("Linked goals cannot reference other linked goals.");
                    }
                }
            }
        }
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
        Map<String, String> mapping = new HashMap<>();
        for (Goal goal : backup.getGoals()) {
            Goal existing = findMatchingGoal(goal, backup.metadata(goal).recurring().getSchedule());
            mapping.put(backup.metadata(goal).getId(), existing == null ? backup.metadata(goal).getId() : metadata(existing).getId());
        }
        for (Goal goal : backup.getGoals()) {
            if (goal instanceof LinkedGoal && findMatchingGoal(goal) == null) {
                ((LinkedGoal) goal).remapSources(mapping);
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
