package com.grindstone.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.impl.ChecklistMilestone;
import com.grindstone.content.milestone.impl.CustomMilestone;
import com.grindstone.content.milestone.impl.LinkedMilestone;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public class MilestonePackTest {
    @Test
    public void packsExcludeAccountStateAndRemapLinkedDefinitions() {
        GrindstoneProfile source = new GrindstoneProfile(987654321);
        CustomMilestone manual = new CustomMilestone("Personal target", "Definition notes", MilestoneCategory.OTHER, 7, 10);
        ChecklistMilestone checklist = new ChecklistMilestone("Routine", "", List.of("First", "Second"));
        checklist.setChecked(0, true);
        source.add(manual);
        source.add(checklist);
        String sourceId = source.getMetadata(manual).getId();
        LinkedMilestone linked = new LinkedMilestone("Ladder", "", LinkedMilestone.Mode.MILESTONES, List.of(sourceId), new long[]{5, 10}, 1);
        source.add(linked);
        linked.update(source, true);
        source.getMetadata(manual).getRecurring().configure(RepeatSchedule.DAILY, 10, System.currentTimeMillis());
        source.getMetadata(manual).getRecurring().observe(manual, System.currentTimeMillis());
        source.getCollectionLogHistory().record(4151, 123456789);

        MilestonePack packs = new MilestonePack(TestProfiles.REPOSITORY);
        String text = packs.exportPack(source);
        JsonObject pack = new JsonParser().parse(text).getAsJsonObject();
        assertEquals(2, pack.size());
        assertFalse(text.contains("987654321"));
        assertFalse(text.contains("123456789"));
        assertFalse(text.contains(sourceId));
        assertFalse(text.contains(manual.getId()));
        assertFalse(text.contains("lastObserved"));
        assertFalse(text.contains("completions"));
        assertFalse(text.contains("history"));
        assertFalse(text.contains("storage"));
        assertTrue(text.contains("Definition notes"));

        // Imported packs cannot smuggle personal observations back into the profile.
        pack.addProperty("hash", source.getHash());
        pack.add("storageSnapshots", TestProfiles.REPOSITORY.toJson(source));
        JsonObject first = pack.getAsJsonArray("goals").get(0).getAsJsonObject();
        first.getAsJsonObject("data").addProperty("current", 999);
        first.getAsJsonObject("metadata").addProperty("lastObservedAt", 999);
        GrindstoneProfile restored = packs.importPack(pack.toString(), 42);
        assertEquals(42, restored.getHash());
        assertEquals(3, restored.getGoals().size());
        assertEquals(0, restored.getGoals().get(0).getProgressCurrent());
        assertEquals(0, restored.getGoals().get(1).getProgressCurrent());
        assertTrue(restored.getCollectionLogHistory().getSnapshot().isEmpty());
        assertEquals(RepeatSchedule.DAILY, restored.getMetadata(restored.getGoals().get(0)).getRecurring().getSchedule());
        assertEquals(0, restored.getMetadata(restored.getGoals().get(0)).getRecurring().getProgress());
        String importedId = restored.getMetadata(restored.getGoals().get(0)).getId();
        assertNotEquals(first.getAsJsonObject("metadata").get("id").getAsString(), importedId);
        LinkedMilestone importedLink = (LinkedMilestone) restored.getGoals().get(2);
        assertEquals(List.of(importedId), importedLink.getSources());
        importedLink.update(restored, true);
        assertEquals(0, importedLink.getProgressCurrent());
    }
}
