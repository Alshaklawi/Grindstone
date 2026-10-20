
package com.grindstone.repository;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grindstone.content.collectionlog.CollectionLogEntry;
import com.grindstone.content.milestone.ClientCompletionMilestone;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import com.grindstone.content.milestone.impl.TrackerMilestone;
import com.grindstone.content.milestone.impl.collectionlog.CollectionLogEntryMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.milestone.impl.level.SkillLevelMilestone;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.util.Filepath;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@lombok.extern.slf4j.Slf4j
@Singleton
public final class GrindstoneRepository {
    public static final int MAX_PROFILE_BYTES = 20_000_000;
    private final Gson gson;

    @Inject
    public GrindstoneRepository(Gson gson) {
        this.gson = gson.newBuilder().setPrettyPrinting().create();
    }

    public GrindstoneProfile load(Filepath directory, long hash) throws IOException {
        requireHash(hash);
        Filepath file = directory.joinSegment(hash + ".json");
        Filepath recovery = directory.joinSegment(hash + ".json.bak");
        if (!file.exists() && !recovery.exists()) {
            return new GrindstoneProfile(hash);
        }
        try {
            return readProfile(file, hash);
        } catch (IOException | RuntimeException failure) {
            if (!recovery.exists()) {
                throw new IOException("The saved profile could not be read. It has not been replaced.", failure);
            }
            GrindstoneProfile restored = readProfile(recovery, hash);
            log.warn("Recovered a Grindstone profile from its previous valid save");
            return restored;
        }
    }

    public GrindstoneProfile readProfile(Filepath file, long hash) throws IOException {
        requireHash(hash);
        JsonObject json = new JsonParser().parse(readText(file, MAX_PROFILE_BYTES)).getAsJsonObject();
        if (!json.has("hash") || json.get("hash").getAsLong() != hash || !json.has("goals") || !json.get("goals").isJsonArray() || json.getAsJsonArray("goals").size() > 5000) {
            throw new IOException("This profile does not belong to the current account or has invalid milestone data.");
        }
        GrindstoneProfile profile = decode(json, true);
        if (profile.getGoals().size() != json.getAsJsonArray("goals").size()) {
            throw new IOException("The profile contains unsupported or invalid milestones. The original file has not been changed.");
        }
        return profile;
    }

    public String readText(Filepath file, int maximum) throws IOException {
        if (!file.isFile() || file.size() > maximum) {
            throw new IOException("The selected file is missing or exceeds the size limit.");
        }
        StringBuilder text = new StringBuilder();
        try (Reader reader = file.openReader()) {
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                text.append(buffer, 0, count);
                if (text.length() > maximum) {
                    throw new IOException("The file exceeds the size limit.");
                }
            }
        }
        return text.toString();
    }

    public void save(Filepath directory, JsonObject snapshot) throws IOException {
        long hash = snapshot.get("hash").getAsLong();
        requireHash(hash);
        String text = gson.toJson(snapshot);
        if (text.length() > MAX_PROFILE_BYTES || text.getBytes(StandardCharsets.UTF_8).length > MAX_PROFILE_BYTES) {
            throw new IOException("The profile exceeds the save size limit. The previous save has not been changed.");
        }
        directory.createDirectories();
        Filepath destination = directory.joinSegment(hash + ".json");
        Filepath recovery = directory.joinSegment(hash + ".json.bak");
        Filepath temporary = directory.createTempFile("profile-", ".tmp");
        try {
            temporary.write(text);
            if (destination.exists()) {
                boolean valid = true;
                try {
                    readProfile(destination, hash);
                } catch (IOException | RuntimeException failure) {
                    valid = false;
                }
                if (valid) {
                    destination.copyTo(recovery, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    destination.copyTo(directory.joinSegment(hash + ".corrupt-" + System.currentTimeMillis() + ".json"));
                }
            }
            try {
                temporary.moveTo(destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                temporary.moveTo(destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                temporary.deleteIfExists();
            } catch (IOException cleanupFailure) {
                log.debug("Unable to remove an unused Grindstone save temporary file", cleanupFailure);
            }
        }
    }

    private static void requireHash(long hash) {
        if (hash == 0) {
            throw new IllegalArgumentException("A logged-in account is required.");
        }
    }

    public JsonObject toJson(GrindstoneProfile profile) {
        JsonObject json = new JsonObject();
        json.addProperty("hash", profile.getHash());

        JsonArray savedGoals = new JsonArray();

        for (Milestone goal : profile.getGoals()) {
            JsonObject savedGoal = new JsonObject();
            savedGoal.addProperty("type", SavedMilestoneTypes.nameOf(goal));
            savedGoal.add("data", gson.toJsonTree(goal));
            savedGoal.add("metadata", gson.toJsonTree(profile.getMetadata(goal)));
            savedGoals.add(savedGoal);
        }

        json.add("goals", savedGoals);
        json.add("archivedHistory", gson.toJsonTree(profile.getArchivedHistory()));
        JsonObject observed = new JsonObject();
        profile.getCollectionLogHistory().getSnapshot().forEach((id, time) -> observed.addProperty(id.toString(), time));
        json.add("collectionLogFirstSeen", observed);
        json.addProperty("collectionLogSnapshotAt", profile.getCollectionLogHistory().getSnapshotAt());
        json.addProperty("collectionLogSnapshotCount", profile.getCollectionLogHistory().getSnapshotCount());

        return json;
    }

    public GrindstoneProfile fromJson(JsonObject json) {
        return decode(json, false);
    }

    private GrindstoneProfile decode(JsonObject json, boolean strict) {
        long hash = json.get("hash").getAsLong();
        GrindstoneProfile profile = new GrindstoneProfile(hash);
        if (json.has("collectionLogFirstSeen") && json.get("collectionLogFirstSeen").isJsonObject()) {
            for (Map.Entry<String, JsonElement> observation : json.getAsJsonObject("collectionLogFirstSeen").entrySet()) {
                try {
                    profile.getCollectionLogHistory().record(Integer.parseInt(observation.getKey()), observation.getValue().getAsLong());
                } catch (RuntimeException exception) {
                    if (strict) {
                        throw new IllegalArgumentException("Invalid collection-log observation", exception);
                    }
                    log.debug("Skipping an invalid collection-log observation", exception);
                }
            }
        }

        if (json.has("collectionLogSnapshotAt")) {
            try {
                long snapshotAt = json.get("collectionLogSnapshotAt").getAsLong();
                if (snapshotAt != 0) {
                    profile.getCollectionLogHistory().restoreSnapshot(snapshotAt, json.get("collectionLogSnapshotCount").getAsInt());
                }
            } catch (RuntimeException exception) {
                if (strict) {
                    throw new IllegalArgumentException("Invalid collection-log snapshot", exception);
                }
                log.debug("Ignoring an invalid collection-log snapshot", exception);
            }
        }

        if (!json.has("goals") || !json.get("goals").isJsonArray()) {
            return profile;
        }

        List<Milestone> goals = new ArrayList<>();
        JsonArray savedGoals = json.getAsJsonArray("goals");

        for (JsonElement element : savedGoals) {
            try {
                JsonObject savedGoal = element.getAsJsonObject();
                JsonElement data = savedGoal.get("data");
                Class<? extends Milestone> type = SavedMilestoneTypes.fromTag(savedGoal);
                if (type == null || data == null || !data.isJsonObject()) {
                    continue;
                }
                Milestone goal;
                if (type == CollectionLogEntryMilestone.class) {
                    JsonObject page = data.getAsJsonObject();
                    if (page.has("pageStruct")) {
                        goal = new CollectionLogEntryMilestone(null, page.get("pageStruct").getAsInt(), page.get("pageName").getAsString());
                    } else {
                        JsonElement entryElement = data.getAsJsonObject().get("entry");
                        if (entryElement == null || entryElement.isJsonNull()) {
                            continue;
                        }
                        CollectionLogEntry entry = gson.fromJson(entryElement, CollectionLogEntry.class);
                        if (entry == null) {
                            continue;
                        }
                        goal = new CollectionLogEntryMilestone(null, entry);
                    }
                } else if (type == CustomMilestone.class) {
                    CustomMilestone saved = gson.fromJson(data, CustomMilestone.class);
                    goal = new CustomMilestone(saved.getId(), saved.getTitle(null), saved.getDescription(), saved.getCategory(), saved.getProgressCurrent(), saved.getProgressMaximum());
                } else if (type == PossessedItemMilestone.class) {
                    PossessedItemMilestone saved = gson.fromJson(data, PossessedItemMilestone.class);
                    goal = new PossessedItemMilestone(saved.getItem(), saved.getAmount());
                } else {
                    goal = gson.fromJson(data, type);
                }

                if (goal != null) {
                    if (goal instanceof ChecklistMilestone) {
                        ((ChecklistMilestone) goal).validate();
                    }
                    if (goal instanceof SessionExperienceMilestone) {
                        ((SessionExperienceMilestone) goal).validate();
                    }
                    if (goal instanceof LinkedMilestone) {
                        ((LinkedMilestone) goal).validate();
                    }
                    if (goal instanceof ClientCompletionMilestone) {
                        ((ClientCompletionMilestone) goal).selectionKey();
                    }
                    if (goal instanceof SkillLevelMilestone && ((SkillLevelMilestone) goal).getSkill() == null || goal instanceof SkillExperienceMilestone && ((SkillExperienceMilestone) goal).getSkill() == null || goal instanceof TrackerMilestone && ((TrackerMilestone) goal).getTracker() == null) {
                        continue;
                    }
                    if (goal.supportsTarget()) {
                        goal.setTarget(goal.getProgressMaximum());
                    }
                    goals.add(goal);
                    if (savedGoal.has("metadata")) {
                        try {
                            profile.restoreMetadata(goal, gson.fromJson(savedGoal.get("metadata"), MilestoneMetadata.class));
                        } catch (RuntimeException exception) {
                            if (strict) {
                                throw new IllegalArgumentException("Invalid milestone metadata", exception);
                            }
                            log.debug("Skipping invalid milestone metadata", exception);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                if (strict) {
                    throw new IllegalArgumentException("Invalid saved milestone", exception);
                }
                log.debug("Skipping an invalid saved Grindstone milestone", exception);
            }
        }

        profile.replace(goals);
        if (json.has("archivedHistory") && json.get("archivedHistory").isJsonArray()) {
            if (json.getAsJsonArray("archivedHistory").size() > 500) {
                throw new IllegalArgumentException("Too many archived milestone histories");
            }
            for (JsonElement saved : json.getAsJsonArray("archivedHistory")) {
                try {
                    profile.restoreArchive(gson.fromJson(saved, GrindstoneProfile.ArchivedHistory.class));
                } catch (RuntimeException exception) {
                    if (strict) {
                        throw new IllegalArgumentException("Invalid archived milestone history", exception);
                    }
                    log.debug("Skipping invalid archived milestone history", exception);
                }
            }
        }
        if (strict) {
            profile.validateReferences();
        }
        return profile;
    }

    public String exportBackup(GrindstoneProfile profile) {
        JsonObject snapshot = toJson(profile);
        snapshot.addProperty("backupVersion", 1);
        return gson.toJson(snapshot);
    }
    public GrindstoneProfile importBackup(String text, long targetHash) {
        if (text == null || text.length() > 5_000_000) {
            throw new IllegalArgumentException("Backup must be smaller than 5 MB.");
        }
        JsonObject json = new JsonParser().parse(text).getAsJsonObject();
        if (!json.has("hash") || !json.has("goals") || !json.get("goals").isJsonArray() || json.getAsJsonArray("goals").size() > 5000) {
            throw new IllegalArgumentException("Not a valid Grindstone profile backup.");
        }
        if (json.has("backupVersion") && json.get("backupVersion").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported backup version.");
        }
        boolean sameAccount = json.get("hash").getAsLong() == targetHash;
        json.addProperty("hash", targetHash);
        if (!sameAccount) {
            json.remove("archivedHistory");
            json.remove("collectionLogFirstSeen");
            json.remove("collectionLogSnapshotAt");
            json.remove("collectionLogSnapshotCount");
            for (JsonElement element : json.getAsJsonArray("goals")) {
                JsonObject saved = element.getAsJsonObject();
                if (saved.has("metadata") && saved.get("metadata").isJsonObject()) {
                    JsonObject metadata = saved.getAsJsonObject("metadata");
                    metadata.addProperty("lastObservedAt", 0);
                    metadata.addProperty("hasLastReading", false);
                    metadata.addProperty("lastObservedCurrent", 0);
                    metadata.addProperty("lastObservedTarget", 0);
                    metadata.add("completions", new JsonArray());
                    if (metadata.has("recurring") && metadata.get("recurring").isJsonObject()) {
                        JsonObject recurring = metadata.getAsJsonObject("recurring");
                        for (String key : new String[]{"start", "baselineAt", "lastValue", "progress", "completedAt", "lastObservedAt"}) {
                            recurring.addProperty(key, 0);
                        }
                        recurring.add("history", new JsonArray());
                    }
                }
                if (SavedMilestoneTypes.fromTag(saved) == CustomMilestone.class) {
                    saved.getAsJsonObject("data").addProperty("current", 0);
                }
                if (SavedMilestoneTypes.fromTag(saved) == ChecklistMilestone.class) {
                    JsonObject data = saved.getAsJsonObject("data");
                    JsonArray cleared = new JsonArray();
                    for (JsonElement ignored : data.getAsJsonArray("checked")) {
                        cleared.add(false);
                    }
                    data.add("checked", cleared);
                }
                if (SavedMilestoneTypes.fromTag(saved) == LinkedMilestone.class) {
                    JsonObject data = saved.getAsJsonObject("data");
                    if (data.has("achieved")) {
                        JsonArray cleared = new JsonArray();
                        for (JsonElement ignored : data.getAsJsonArray("achieved")) {
                            cleared.add(false);
                        }
                        data.add("achieved", cleared);
                    }
                    data.add("completedThrough", new JsonObject());
                    data.addProperty("completedPeriods", 0);
                }
            }
        }
        GrindstoneProfile restored = decode(json, true);
        if (restored.getGoals().size() != json.getAsJsonArray("goals").size()) {
            throw new IllegalArgumentException("Backup contains invalid or unsupported milestones. No milestones were imported.");
        }
        return restored;
    }
}
