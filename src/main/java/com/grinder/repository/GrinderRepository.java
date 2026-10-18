
package com.grinder.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grinder.content.CollectionLogEntry;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.impl.TrackerGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogEntryGoal;
import com.grinder.content.goal.impl.colletion_log.CollectionLogItemGoal;
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
public final class GrinderRepository {
    private static final Path DIRECTORY = Paths.get("grinder", "profiles");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Long, GrinderProfile> profiles = new HashMap<>();

    private GrinderRepository() {
        // NO-OP
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

        JsonObject json = new JsonObject();
        json.addProperty("hash", profile.getHash());

        JsonArray savedGoals = new JsonArray();

        for (Goal goal : profile.getGoals()) {
            JsonObject savedGoal = new JsonObject();
            savedGoal.addProperty("class", goal.getClass().getName());
            savedGoal.add("data", GSON.toJsonTree(goal));
            savedGoals.add(savedGoal);
        }

        json.add("goals", savedGoals);

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

    public static synchronized Map<Long, GrinderProfile> getProfiles() {
        return Collections.unmodifiableMap(new HashMap<>(profiles));
    }

    private static GrinderProfile load(Path path) throws IOException {
        JsonObject json = new JsonParser().parse(Files.readString(path)).getAsJsonObject();
        long hash = json.get("hash").getAsLong();
        GrinderProfile profile = new GrinderProfile(hash);

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
                Goal goal;

                if (CollectionLogEntryGoal.class.getName().equals(className)) {
                    if (data == null || !data.isJsonObject()) {
                        continue;
                    }

                    JsonElement entryElement = data.getAsJsonObject().get("entry");

                    if (entryElement == null || entryElement.isJsonNull()) {
                        continue;
                    }

                    CollectionLogEntry entry = GSON.fromJson(entryElement, CollectionLogEntry.class);
                    goal = new CollectionLogEntryGoal(null, entry);
                } else if (CollectionLogItemGoal.class.getName().equals(className)) {
                    goal = GSON.fromJson(data, CollectionLogItemGoal.class);
                } else if (TrackerGoal.class.getName().equals(className)) {
                    goal = GSON.fromJson(data, TrackerGoal.class);
                } else {
                    // Profiles are intentionally limited to the built-in goal
                    // types. Never load a class selected by profile data.
                    continue;
                }

                if (goal != null) {
                    goals.add(goal);
                }
            } catch (RuntimeException exception) {
                System.err.println("Failed to restore a goal in " + path + ": " + exception.getMessage());
            }
        }

        profile.replace(goals);
        profile.ensureDefaultGoals(null);
        return profile;
    }

    private static Path getPath(long hash) {
        return DIRECTORY.resolve(hash + ".json");
    }
}
