package com.grindstone.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@Singleton
public final class MilestonePack {
    private static final Set<String> DEFINITION_FIELDS = Set.of("id", "title", "description", "category", "target", "tracker", "count", "skill", "total", "tabStruct", "tabName", "item", "amount", "entry", "pageStruct", "pageName", "mode", "sources", "targets", "required", "quest", "series", "area", "tier", "boss", "name", "task", "steps");
    private final GrindstoneRepository repository;

    @Inject
    public MilestonePack(GrindstoneRepository repository) {
        this.repository = repository;
    }

    public String exportPack(GrindstoneProfile profile) {
        JsonObject pack = new JsonObject();
        pack.addProperty("packVersion", 1);
        pack.add("goals", copyDefinitions(repository.toJson(profile).getAsJsonArray("goals")));
        return pack.toString();
    }

    public GrindstoneProfile importPack(String text, long accountHash) {
        if (text == null || text.length() > 5_000_000) {
            throw new IllegalArgumentException("Milestone pack must be smaller than 5 MB.");
        }
        JsonObject pack = new JsonParser().parse(text).getAsJsonObject();
        if (!pack.has("packVersion") || pack.get("packVersion").getAsInt() != 1 || !pack.has("goals") || !pack.get("goals").isJsonArray()) {
            throw new IllegalArgumentException("Not a supported Grindstone milestone pack.");
        }
        JsonObject profile = new JsonObject();
        profile.addProperty("hash", accountHash);
        profile.add("goals", copyDefinitions(pack.getAsJsonArray("goals")));
        return repository.importBackup(profile.toString(), accountHash);
    }

    private static JsonArray copyDefinitions(JsonArray entries) {
        if (entries.size() > GrindstoneRepository.MAX_MILESTONES) {
            throw new IllegalArgumentException("Too many milestones in this pack.");
        }
        Map<String, String> identities = new HashMap<>();
        for (JsonElement entry : entries) {
            String id = entry.getAsJsonObject().getAsJsonObject("metadata").get("id").getAsString();
            UUID.fromString(id);
            if (identities.put(id, UUID.randomUUID().toString()) != null) {
                throw new IllegalArgumentException("Duplicate milestone identity in this pack.");
            }
        }
        JsonArray definitions = new JsonArray();
        for (JsonElement entry : entries) {
            JsonObject saved = entry.getAsJsonObject();
            if (SavedMilestoneTypes.fromTag(saved) == null) {
                throw new IllegalArgumentException("Unsupported milestone type in this pack.");
            }
            String type = saved.get("type").getAsString();
            JsonObject data = copyFields(saved.getAsJsonObject("data"), DEFINITION_FIELDS);
            if (data.has("id")) {
                data.addProperty("id", UUID.randomUUID().toString());
            }
            if ("custom".equals(type)) {
                data.addProperty("current", 0);
            } else if ("manual-checklist".equals(type)) {
                data.add("checked", emptyChecks(data.getAsJsonArray("steps").size()));
            } else if ("linked-goal".equals(type)) {
                JsonArray sources = new JsonArray();
                for (JsonElement source : data.getAsJsonArray("sources")) {
                    String mapped = identities.get(source.getAsString());
                    if (mapped == null) {
                        throw new IllegalArgumentException("A linked milestone has a missing source. Repair its sources before sharing.");
                    }
                    sources.add(mapped);
                }
                data.add("sources", sources);
                data.add("achieved", emptyChecks(sources.size()));
                data.add("completedThrough", new JsonObject());
                data.addProperty("completedPeriods", 0);
            }
            JsonObject original = saved.getAsJsonObject("metadata");
            JsonObject metadata = copyFields(original, Set.of("pinned", "displayCategory"));
            metadata.addProperty("id", identities.get(original.get("id").getAsString()));
            if (original.has("recurring")) {
                metadata.add("recurring", copyFields(original.getAsJsonObject("recurring"), Set.of("schedule", "target")));
            }
            JsonObject definition = new JsonObject();
            definition.addProperty("type", type);
            definition.add("data", data);
            definition.add("metadata", metadata);
            definitions.add(definition);
        }
        return definitions;
    }

    private static JsonArray emptyChecks(int size) {
        JsonArray checks = new JsonArray();
        for (int i = 0; i < size; i++) {
            checks.add(false);
        }
        return checks;
    }

    private static JsonObject copyFields(JsonObject source, Set<String> fields) {
        JsonObject copy = new JsonObject();
        for (String key : fields) {
            if (source.has(key)) {
                copy.add(key, source.get(key).deepCopy());
            }
        }
        return copy;
    }
}
