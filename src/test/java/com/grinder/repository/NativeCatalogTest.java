package com.grinder.repository;
import com.google.gson.JsonObject;
import com.grinder.content.collectionlog.CollectionLogEntry;
import com.grinder.content.collectionlog.CollectionLogHistory;
import com.grinder.content.collectionlog.CollectionLogPages;
import com.grinder.content.goal.impl.collectionlog.CollectionLogEntryGoal;
import com.grinder.content.tracker.TrackerEntry;
import java.util.Collections;
import java.util.HashSet;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class NativeCatalogTest {
    @Test
    public void allActivityCountersUseDistinctNamedClientVariables() {
        HashSet<String> variables = new HashSet<>();
        for (TrackerEntry entry : TrackerEntry.values()) {
            assertTrue(variables.add(entry.isVarbit() + ":" + entry.getId()));
            assertFalse(entry.getDisplayName().isEmpty());
            assertNotNull(entry.getIcon());
        }
        assertEquals(90, variables.size());
        assertEquals(VarPlayerID.TOTAL_WINTERTODT_KILLS, TrackerEntry.WINTERTODT.getId());
        assertEquals(VarPlayerID.TOTAL_CORP_KILLS, TrackerEntry.CORPOREAL_BEAST.getId());
    }
    @Test
    public void dynamicCollectionPagesHaveIndependentIdentityAndSurviveReload() {
        GrinderProfile profile = TestProfiles.populated(42);
        profile.replace(Collections.emptyList());
        assertTrue(profile.add(new CollectionLogEntryGoal(null, 10_000, "Test page A")));
        assertTrue(profile.add(new CollectionLogEntryGoal(null, 10_001, "Test page B")));
        assertFalse(profile.add(new CollectionLogEntryGoal(null, 10_000, "Renamed test page A")));
        GrinderProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        assertEquals(2, restored.getGoals().size());
        assertEquals(10_001, ((CollectionLogEntryGoal) restored.getGoals().get(1)).getPageStruct());
    }
    @Test
    public void legacyEnumPagesStillLoadWithoutStructFields() {
        GrinderProfile profile = TestProfiles.populated(42);
        profile.replace(Collections.singletonList(new CollectionLogEntryGoal(null, CollectionLogEntry.CHAMBERS_OF_XERIC)));
        JsonObject json = TestProfiles.REPOSITORY.toJson(profile);
        JsonObject data = json.getAsJsonArray("goals").get(0).getAsJsonObject().getAsJsonObject("data");
        data.remove("pageStruct");
        data.remove("pageName");
        assertEquals(507, ((CollectionLogEntryGoal) TestProfiles.REPOSITORY.fromJson(json).getGoals().get(0)).getPageStruct());
    }
    @Test
    public void collectionLogAliasesRetainEarliestUnlockObservation() {
        CollectionLogHistory history = new CollectionLogHistory();
        history.record(ItemID.COAL_BAG, 1000);
        history.record(ItemID.COAL_BAG_DUMMY, 2000);
        assertEquals(ItemID.COAL_BAG_DUMMY, CollectionLogPages.canonicalItemId(ItemID.COAL_BAG));
        assertEquals(Long.valueOf(1000), history.firstSeenAt(ItemID.COAL_BAG_DUMMY));
        assertTrue(history.contains(ItemID.COAL_BAG));
        assertEquals(1, history.snapshot().size());
    }
}
