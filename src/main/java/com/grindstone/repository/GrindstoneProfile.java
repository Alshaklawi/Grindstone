package com.grindstone.repository;

import com.grindstone.content.collectionlog.CollectionLogHistory;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogTabMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogItemMilestone;
import com.grindstone.content.milestone.impl.combatachievement.CombatAchievementBossMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import com.grindstone.content.storage.PlayerItemStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.game.ItemManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GrindstoneProfile {
    @Getter
    private final long hash;
    private final List<Milestone> goals = new ArrayList<>();
    private final transient Map<Milestone, MilestoneMetadata> metadata = new IdentityHashMap<>();
    private transient long revision;
    private final Map<String, ArchivedHistory> archivedHistory = new LinkedHashMap<>();
    public Collection<ArchivedHistory> getArchivedHistory() {
        return Collections.unmodifiableCollection(archivedHistory.values());
    }
    public void restoreArchive(ArchivedHistory history) {
        if (history == null || history.title == null || history.title.length() > 300) {
            throw new IllegalArgumentException("Invalid archived history");
        }
        MilestoneMetadata.validated(history.metadata);
        if (goals.stream().noneMatch(goal -> getMetadata(goal).getId().equals(history.metadata.getId()))) {
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
            MilestoneMetadata.validated(history.metadata);
        }
        archivedHistory.clear();
        histories.forEach(this::restoreArchive);
    }
    private void archive(Milestone goal) {
        MilestoneMetadata saved = getMetadata(goal).copy();
        if (saved.getCompletions().isEmpty() && saved.getRecurring().getHistory().isEmpty() && saved.getRecurring().getBaselineAt() == 0) {
            return;
        }
        saved.getRecurring().stop(System.currentTimeMillis(), "Milestone archived · last observed");
        archivedHistory.put(saved.getId(), new ArchivedHistory(goal.getTitle(null), saved));
        while (archivedHistory.size() > 500) {
            archivedHistory.remove(archivedHistory.keySet().iterator().next());
        }
    }
    public long getRevision() {
        return revision;
    }
    public void touch() {
        revision++;
    }
    public Consumer<Milestone> snapshotEdit(Milestone original) {
        MilestoneMetadata saved = getMetadata(original).copy();
        RepeatSchedule previousSchedule = saved.getRecurring().getSchedule();
        long undoWindow = previousSchedule.getPeriodStart(System.currentTimeMillis());
        long target = original.getProgressMaximum();
        CustomMilestone custom = original instanceof CustomMilestone ? (CustomMilestone) original : null;
        CustomMilestone manual = custom == null ? null : new CustomMilestone(custom.getId(), custom.getTitle(null), custom.getDescription(), custom.getCategory(), custom.getProgressCurrent(), target);
        ChecklistMilestone checklist = original instanceof ChecklistMilestone ? (ChecklistMilestone) original : null;
        boolean[] savedChecks = checklist == null ? null : checklist.getChecks();
        return replacement -> {
            if (previousSchedule != RepeatSchedule.NONE && previousSchedule.getPeriodStart(System.currentTimeMillis()) != undoWindow) {
                throw new IllegalStateException("The previous period has reset.");
            }
            int index = goals.indexOf(replacement);
            if (index < 0) {
                throw new IllegalStateException("The edited milestone no longer exists.");
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
    public MilestoneMetadata getMetadata(Milestone goal) {
        return metadata.computeIfAbsent(goal, ignored -> new MilestoneMetadata());
    }
    public void restoreMetadata(Milestone goal, MilestoneMetadata saved) {
        metadata.put(goal, MilestoneMetadata.validated(saved));
    }
    public void observeRecurring(Client client, long now) {
        for (Milestone goal : goals) {
            RecurringMilestone recurring = getMetadata(goal).getRecurring();
            if (recurring.isActive() && RecurringMilestone.supports(goal)) {
                goal.refresh(client);
                recurring.observe(goal, now);
            }
        }
        updateLinkedGoals(client != null && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getAccountHash() == hash);
    }
    public void updateLinkedGoals(boolean loggedIn) {
        Map<String, Milestone> sources = new HashMap<>();
        for (Milestone goal : goals) {
            if (!(goal instanceof LinkedMilestone)) {
                sources.put(getMetadata(goal).getId(), goal);
            }
        }
        for (Milestone goal : goals) {
            if (goal instanceof LinkedMilestone) {
                ((LinkedMilestone) goal).update(this, loggedIn, sources);
            }
        }
    }
    @Getter
    private final CollectionLogHistory collectionLogHistory = new CollectionLogHistory();
    private transient ItemManager itemManager;
    @Getter
    private final transient PlayerItemStorage playerStorage = new PlayerItemStorage();

    public GrindstoneProfile(long hash) {
        this(hash, null);
    }

    public GrindstoneProfile(long hash, ItemManager itemManager) {
        this.hash = hash;
        setItemManager(itemManager);
    }

    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
        for (Milestone goal : goals) {
            attachCollectionData(goal);
        }
    }

    private void attachCollectionData(Milestone goal) {
        if (goal instanceof TrackerMilestone) {
            ((TrackerMilestone) goal).setItemManager(itemManager);
        }
        if (goal instanceof PossessedItemMilestone) {
            ((PossessedItemMilestone) goal).attach(playerStorage, itemManager);
        }
        if (goal instanceof CollectionLogTabMilestone) {
            ((CollectionLogTabMilestone) goal).setHistory(collectionLogHistory);
        }
        if (goal instanceof CollectionLogEntryMilestone) {
            CollectionLogEntryMilestone entry = (CollectionLogEntryMilestone) goal;
            entry.setItemManager(itemManager);
            entry.setHistory(collectionLogHistory);
        } else if (goal instanceof CollectionLogItemMilestone) {
            CollectionLogItemMilestone item = (CollectionLogItemMilestone) goal;
            item.setItemManager(itemManager);
            item.setHistory(collectionLogHistory);
        }
    }

    public boolean add(Milestone goal) {
        return add(goal, new MilestoneMetadata());
    }

    public boolean add(Milestone goal, MilestoneMetadata details) {
        Objects.requireNonNull(goal, "goal");
        MilestoneMetadata.validated(details);
        if (findMatchingGoal(goal, details.getRecurring().getSchedule()) != null) {
            return false;
        }
        attachCollectionData(goal);
        metadata.put(goal, details);
        archivedHistory.remove(details.getId());
        touch();
        return goals.add(goal);
    }

    public Milestone findMatchingGoal(Milestone candidate) {
        return findMatchingGoal(candidate, RepeatSchedule.NONE);
    }

    public Milestone findMatchingGoal(Milestone candidate, RepeatSchedule schedule) {
        return goals.stream().filter(goal -> sameGoal(goal, candidate) && getMetadata(goal).getRecurring().getSchedule() == schedule).findFirst().orElse(null);
    }

    private static boolean sameGoal(Milestone left, Milestone right) {
        if (left.getClass() != right.getClass()) {
            return false;
        }
        if (left instanceof CombatAchievementBossMilestone) {
            return ((CombatAchievementBossMilestone) left).matches((CombatAchievementBossMilestone) right);
        }
        if (left instanceof PossessedItemMilestone) {
            return ((PossessedItemMilestone) left).getItem()
            == ((PossessedItemMilestone) right).getItem();
        }
        if (left instanceof ClientCompletionMilestone) {
            return ((ClientCompletionMilestone) left).selectionKey()
                    .equals(((ClientCompletionMilestone) right).selectionKey());
        }
        if (left instanceof CustomMilestone) {
            return ((CustomMilestone) left).getId().equals(((CustomMilestone) right).getId());
        }
        if (left instanceof ChecklistMilestone) {
            return ((ChecklistMilestone) left).getId().equals(((ChecklistMilestone) right).getId());
        }
        if (left instanceof LinkedMilestone) {
            return ((LinkedMilestone) left).getId().equals(((LinkedMilestone) right).getId());
        }
        if (left instanceof SkillLevelMilestone) {
            return ((SkillLevelMilestone) left).getSkill() == ((SkillLevelMilestone) right).getSkill();
        }
        if (left instanceof SkillExperienceMilestone) {
            return ((SkillExperienceMilestone) left).getSkill() == ((SkillExperienceMilestone) right).getSkill();
        }
        if (left instanceof SessionExperienceMilestone) {
            SessionExperienceMilestone first = (SessionExperienceMilestone) left;
            SessionExperienceMilestone second = (SessionExperienceMilestone) right;
            return first.isTotal() == second.isTotal() && first.getSkill() == second.getSkill();
        }
        if (left instanceof TrackerMilestone) {
            return ((TrackerMilestone) left).getTracker() == ((TrackerMilestone) right).getTracker();
        }
        if (left instanceof CollectionLogTabMilestone) {
            return ((CollectionLogTabMilestone) left).getTabStruct() == ((CollectionLogTabMilestone) right).getTabStruct();
        }
        if (left instanceof CollectionLogEntryMilestone) {
            return ((CollectionLogEntryMilestone) left).getPageStruct() == ((CollectionLogEntryMilestone) right).getPageStruct();
        }
        if (left instanceof CollectionLogItemMilestone) {
            return ((CollectionLogItemMilestone) left).getItem() == ((CollectionLogItemMilestone) right).getItem();
        }
        return true;
    }
    public static boolean sameSelection(Milestone left, Milestone right) {
        return sameGoal(left, right);
    }
    public boolean replaceGoal(Milestone original, Milestone replacement) {
        return replaceGoal(original, replacement, getMetadata(original).getRecurring().getSchedule());
    }
    public boolean replaceGoal(Milestone original, Milestone replacement, RepeatSchedule schedule) {
        if (original == replacement) {
            return goals.contains(original);
        }
        int index = goals.indexOf(original);
        if (index < 0) {
            return false;
        }
        for (Milestone goal : goals) {
            if (goal != original && sameGoal(goal, replacement) && getMetadata(goal).getRecurring().getSchedule() == schedule) {
                throw new IllegalArgumentException("That tracker and schedule are already tracked. Edit the existing milestone instead.");
            }
        }
        MilestoneMetadata old = getMetadata(original);
        if (!sameGoal(original, replacement)) {
            archive(original);
        }
        MilestoneMetadata next = sameGoal(original, replacement) ? old : new MilestoneMetadata();
        if (sameGoal(original, replacement) && original instanceof SessionExperienceMilestone) {
            ((SessionExperienceMilestone) replacement).copyReadingsFrom((SessionExperienceMilestone) original);
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
        Map<String, Milestone> byId = new HashMap<>();
        for (Milestone goal : goals) {
            if (byId.put(getMetadata(goal).getId(), goal) != null) {
                throw new IllegalArgumentException("Duplicate milestone metadata ID.");
            }
        }
        for (Milestone goal : goals) {
            if (goal instanceof LinkedMilestone) {
                for (String source : ((LinkedMilestone) goal).getSources()) {
                    if (byId.get(source) instanceof LinkedMilestone) {
                        throw new IllegalArgumentException("Linked milestones cannot reference other linked milestones.");
                    }
                }
            }
        }
    }

    public boolean remove(Milestone goal) {
        if (goals.contains(goal)) {
            archive(goal);
        }
        boolean removed = goals.remove(goal);
        if (removed) {
            touch();
        }
        return removed;
    }
    public int mergeBackup(GrindstoneProfile backup) {
        if (backup.hash != hash) {
            throw new IllegalArgumentException("Backup must be mapped to this account before merging.");
        }
        int added = 0;
        Map<String, String> mapping = new HashMap<>();
        for (Milestone goal : backup.getGoals()) {
            Milestone existing = findMatchingGoal(goal, backup.getMetadata(goal).getRecurring().getSchedule());
            mapping.put(backup.getMetadata(goal).getId(), existing == null ? backup.getMetadata(goal).getId() : getMetadata(existing).getId());
        }
        for (Milestone goal : backup.getGoals()) {
            if (goal instanceof LinkedMilestone && findMatchingGoal(goal) == null) {
                ((LinkedMilestone) goal).remapSources(mapping);
            }
            if (add(goal, backup.getMetadata(goal))) {
                added++;
            }
        }
        backup.collectionLogHistory.getSnapshot().forEach(collectionLogHistory::record);
        backup.getArchivedHistory().forEach(this::restoreArchive);
        setItemManager(itemManager);
        return added;
    }

    public List<Milestone> getGoals() {
        return Collections.unmodifiableList(goals);
    }

    public void replace(List<Milestone> goals) {
        this.goals.clear();
        this.goals.addAll(goals);
        this.goals.forEach(this::attachCollectionData);
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/19/2026
     */
    public static final class ArchivedHistory {
        private final String title;
        private final MilestoneMetadata metadata;
        private ArchivedHistory(String title, MilestoneMetadata metadata) {
            this.title = title;
            this.metadata = metadata;
        }
        public String getTitle() {
            return title;
        }
        public MilestoneMetadata getMetadata() {
            return metadata;
        }
    }
}
