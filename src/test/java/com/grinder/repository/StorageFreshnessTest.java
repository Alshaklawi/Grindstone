package com.grinder.repository;
import com.grinder.content.storage.PlayerItemStorage;
import com.grinder.content.storage.PlayerStorageSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import net.runelite.api.Item;
import net.runelite.api.gameval.InventoryID;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class StorageFreshnessTest {
    @Test public void freshnessDistinguishesUnknownEmptyAndObservedBank() {
        Instant now = Instant.parse("2026-10-18T12:00:00Z");
        PlayerItemStorage storage = new PlayerItemStorage(Clock.fixed(now, ZoneOffset.UTC));
        assertTrue(storage.freshness().contains("not loaded"));
        storage.replace(InventoryID.BANK, new Item[0], id -> id);
        assertEquals(now, storage.lastSeen().get(PlayerStorageSource.BANK));
        assertTrue(storage.freshness().contains("snapshot"));
        storage.reset();
        assertTrue(storage.lastSeen().isEmpty());
        assertTrue(storage.freshness().contains("not loaded"));
    }
}
