package com.grinder.repository;

import com.google.gson.*;
import com.grinder.content.goal.Goal;
import net.runelite.api.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        //NO-OP
    }

    public static synchronized void load() throws IOException {
        Files.createDirectories(DIRECTORY);

        try (java.util.stream.Stream<Path> files = Files.list(DIRECTORY)) {
            List<Path> paths = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .collect(Collectors.toList());

            for (Path path : paths) {
                try {
                    GrinderProfile profile = load(path);
                    profiles.put(profile.getHash(), profile);
                } catch (RuntimeException | IOException exception) {
                    System.err.println(
                            "Failed to load goal profile " + path + ": "
                                    + exception.getMessage()
                    );
                }
            }
        }
    }

    public static synchronized GrinderProfile load(Client client) throws IOException {
        GrinderProfile profile = profiles.get(client.getAccountHash());

        if (profile != null) {
            return profile;
        }

        Files.createDirectories(DIRECTORY);

        Path path = getPath(client.getAccountHash());
        profile = Files.exists(path) ? load(path) : new GrinderProfile(client.getAccountHash());
        profiles.put(client.getAccountHash(), profile);

        return profile;
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
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException exception) {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            profiles.put(profile.getHash(), profile);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static synchronized Map<Long, GrinderProfile> getProfiles() {
        return Map.copyOf(profiles);
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

        for (int i = 0; i < savedGoals.size(); i++) {
            try {
                JsonObject savedGoal = savedGoals.get(i).getAsJsonObject();
                String className = savedGoal.get("class").getAsString();

                Class<?> type = Class.forName(className);

                if (!Goal.class.isAssignableFrom(type)) {
                    continue;
                }

                Goal goal = (Goal) GSON.fromJson(savedGoal.get("data"), type);

                if (goal != null) {
                    goals.add(goal);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                System.err.println("Failed to restore a goal in " + path + ": " + exception.getMessage());
            }
        }

        profile.replace(goals);
        return profile;
    }

    private static Path getPath(long hash) {
        return DIRECTORY.resolve(hash + ".json");
    }
}