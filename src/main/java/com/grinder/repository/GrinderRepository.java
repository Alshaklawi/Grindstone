
package com.grinder.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grinder.content.collectionlog.CollectionLogEntry;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.CustomGoal;
import com.grinder.content.goal.impl.level.CombatLevelGoal;
import com.grinder.content.goal.impl.level.SkillLevelGoal;
import com.grinder.content.goal.impl.level.TotalLevelGoal;
import com.grinder.content.goal.impl.experience.SkillExperienceGoal;
import com.grinder.content.goal.impl.experience.TotalExperienceGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.collectionlog.CollectionLogItemGoal;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@lombok.extern.slf4j.Slf4j
public final class GrinderRepository {
    private static final Path DIRECTORY = Paths.get("grinder", "profiles");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Long, GrinderProfile> profiles = new HashMap<>();

    private GrinderRepository() {
    }

    public static synchronized void load() throws IOException {
        Files.createDirectories(DIRECTORY);
        profiles.clear();

        List<Path> paths;

        try (Stream<Path> files = Files.list(DIRECTORY)) {
            paths = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .collect(Collectors.toList());
        }

        for (Path path : paths) {
            try {
                GrinderProfile profile = load(path);
                profiles.put(profile.getHash(), profile);
            } catch (RuntimeException | IOException exception) {
                System.err.println("Failed to load goal profile " + path + ": " + exception.getMessage());
            }
        }
    }

    public static synchronized GrinderProfile load(Client client, ItemManager itemManager) throws IOException {
        long hash = client.getAccountHash();
        if (client.getLocalPlayer() == null || hash == 0) {
            throw new IllegalStateException("A logged-in player is required to load Grinder goals");
        }
        GrinderProfile profile = profiles.get(hash);

        if (profile == null) {
            Files.createDirectories(DIRECTORY);
            Path path = getPath(hash);
            profile = Files.exists(path) ? load(path) : new GrinderProfile(hash);
            profiles.put(hash, profile);
        }

        profile.setItemManager(itemManager);
        return profile;
    }

    public static synchronized GrinderProfile load(Client client) throws IOException {
        return load(client, null);
    }

    public static synchronized void save(GrinderProfile profile) throws IOException {
        Files.createDirectories(DIRECTORY);

        JsonObject json = toJson(profile);

        Path destination = getPath(profile.getHash());
        Path temporary = Files.createTempFile(DIRECTORY, "profile-", ".tmp");

        try {
            Files.writeString(temporary, GSON.toJson(json));

            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            profiles.put(profile.getHash(), profile);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static JsonObject toJson(GrinderProfile profile) {
        JsonObject json = new JsonObject();
        json.addProperty("hash", profile.getHash());

        JsonArray savedGoals = new JsonArray();

        for (Goal goal : profile.getGoals()) {
            JsonObject savedGoal = new JsonObject();
            savedGoal.addProperty("class", SavedGoalTypes.nameOf(goal));
            savedGoal.add("data", GSON.toJsonTree(goal));
            savedGoals.add(savedGoal);
        }

        json.add("goals", savedGoals);
        JsonObject observed = new JsonObject();
        profile.getCollectionLogHistory().snapshot().forEach((id, time) -> observed.addProperty(id.toString(), time));
        json.add("collectionLogFirstSeen", observed);

        return json;
    }

    public static synchronized Map<Long, GrinderProfile> getProfiles() {
        return Collections.unmodifiableMap(new HashMap<>(profiles));
    }

    private static GrinderProfile load(Path path) throws IOException {
        JsonObject json = new JsonParser().parse(Files.readString(path)).getAsJsonObject();
        return fromJson(json);
    }

    static GrinderProfile fromJson(JsonObject json) {
        long hash = json.get("hash").getAsLong();
        GrinderProfile profile = new GrinderProfile(hash);
        if (json.has("collectionLogFirstSeen") && json.get("collectionLogFirstSeen").isJsonObject()) {
            for (Map.Entry<String, JsonElement> observation : json.getAsJsonObject("collectionLogFirstSeen").entrySet()) {
                try {
                    profile.getCollectionLogHistory().record(Integer.parseInt(observation.getKey()), observation.getValue().getAsLong());
                } catch (RuntimeException exception) {
                    log.debug("Skipping an invalid collection-log observation", exception);
                }
            }
        }

        if (!json.has("goals") || !json.get("goals").isJsonArray()) {
            return profile;
        }

        List<Goal> goals = new ArrayList<>();
        JsonArray savedGoals = json.getAsJsonArray("goals");

        for (JsonElement element : savedGoals) {
            try {
                JsonObject savedGoal = element.getAsJsonObject();
                String className = savedGoal.get("class").getAsString();
                JsonElement data = savedGoal.get("data");
                Class<? extends Goal> type = SavedGoalTypes.resolve(className);
                if (type == null || data == null || !data.isJsonObject()) { continue; }
                Goal goal;
                if (type == CollectionLogEntryGoal.class) {
                    JsonElement entryElement = data.getAsJsonObject().get("entry");
                    if (entryElement == null || entryElement.isJsonNull()) { continue; }
                    CollectionLogEntry entry = GSON.fromJson(entryElement, CollectionLogEntry.class);
                    if (entry == null) { continue; }
                    goal = new CollectionLogEntryGoal(null, entry);
                } else if (type == CustomGoal.class) {
                    CustomGoal saved = GSON.fromJson(data, CustomGoal.class);
                    goal = new CustomGoal(saved.getId(), saved.getTitle(null), saved.getDescription(), saved.getCategory(), saved.getProgressCurrent(), saved.getProgressMaximum());
                } else if (type == com.grinder.content.goal.impl.item.PossessedItemGoal.class) {
                    com.grinder.content.goal.impl.item.PossessedItemGoal saved = GSON.fromJson(data, com.grinder.content.goal.impl.item.PossessedItemGoal.class);
                    goal = new com.grinder.content.goal.impl.item.PossessedItemGoal(saved.getItem(), saved.getAmount());
                } else {
                    goal = GSON.fromJson(data, type);
                }

                if (goal != null) {
                    if (goal instanceof com.grinder.content.goal.ClientCompletionGoal) {
                        ((com.grinder.content.goal.ClientCompletionGoal) goal).selectionKey();
                    }
                    if (goal instanceof SkillLevelGoal && ((SkillLevelGoal) goal).getSkill() == null
                            || goal instanceof SkillExperienceGoal && ((SkillExperienceGoal) goal).getSkill() == null
                            || goal instanceof TrackerGoal && ((TrackerGoal) goal).getTracker() == null) {
                        continue;
                    }
                    if (goal.supportsTarget()) {
                        goal.setTarget(goal.getProgressMaximum());
                    }
                    goals.add(goal);
                }
            } catch (RuntimeException exception) {
                log.debug("Skipping an invalid saved Grinder goal", exception);
            }
        }

        profile.replace(goals);
        return profile;
    }

    private static Path getPath(long hash) {
        return DIRECTORY.resolve(hash + ".json");
    }
}
