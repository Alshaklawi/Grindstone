package com.grindstone.repository;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.storage.PlayerItemStorage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Item;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class PossessedItemMilestoneTest {
    @Test
    public void snapshotHeaderUpdatesWithoutOpeningTheBankAndResetsOnLogout() {
        Instant now = Instant.parse("2026-10-19T12:34:56Z");
        PlayerItemStorage storage = new PlayerItemStorage(Clock.fixed(now, ZoneOffset.UTC));
        assertEquals("Last snapshot: —", storage.snapshotLabel());
        put(storage, InventoryID.INV, ItemID.COINS, 10);
        String time = DateTimeFormatter.ofPattern("HH:mm:ss z", Locale.ROOT).withZone(ZoneId.systemDefault()).format(now);
        assertEquals("Last snapshot: " + time, storage.snapshotLabel());
        assertFalse(storage.hasBankSnapshot());
        storage.reset();
        assertEquals("Last snapshot: —", storage.snapshotLabel());
    }

    @Test
    public void combinesPersonalContainersWithLongArithmetic() {
        PlayerItemStorage storage = new PlayerItemStorage();
        put(storage, InventoryID.BANK, ItemID.COINS, 2_000_000_000);
        put(storage, InventoryID.INV, ItemID.COINS, 1_000_000_000);
        put(storage, InventoryID.LOOTING_BAG, ItemID.COINS, 500_000_000);
        assertEquals(3_500_000_000L, storage.quantity(ItemID.COINS));
        assertEquals(3, storage.breakdown(ItemID.COINS).size());
    }
    @Test
    public void equipmentAndSpecializedStorageAreIncluded() {
        PlayerItemStorage storage = new PlayerItemStorage();
        put(storage, InventoryID.WORN, ItemID.DRAGON_PICKAXE, 1);
        put(storage, InventoryID.TACKLE_BOX, ItemID.DRAGON_PICKAXE, 2);
        put(storage, InventoryID.SEED_BOX, ItemID.DRAGON_PICKAXE, 3);
        put(storage, InventoryID.SEED_VAULT, ItemID.DRAGON_PICKAXE, 4);
        put(storage, InventoryID.HUNTSMANS_KIT, ItemID.DRAGON_PICKAXE, 5);
        put(storage, InventoryID.FORESTRY_KIT, ItemID.DRAGON_PICKAXE, 6);
        put(storage, InventoryID.PREPOT_DEVICE_INV, ItemID.DRAGON_PICKAXE, 7);
        put(storage, InventoryID.DEATH_PERMANENT, ItemID.DRAGON_PICKAXE, 8);
        put(storage, InventoryID.SAILING_BOAT_1_CARGOHOLD, ItemID.DRAGON_PICKAXE, 9);
        assertEquals(45, storage.quantity(ItemID.DRAGON_PICKAXE));
    }
    @Test
    public void snapshotsReplaceRatherThanAccumulateAndTransfersKeepTheTotal() {
        PlayerItemStorage storage = new PlayerItemStorage();
        put(storage, InventoryID.BANK, ItemID.COINS, 100);
        put(storage, InventoryID.INV, ItemID.COINS, 10);
        put(storage, InventoryID.BANK, ItemID.COINS, 90);
        put(storage, InventoryID.INV, ItemID.COINS, 20);
        assertEquals(110, storage.quantity(ItemID.COINS));
        storage.replace(InventoryID.BANK, new Item[0], id -> id);
        assertEquals(20, storage.quantity(ItemID.COINS));
    }
    @Test
    public void notesAreCombinedAndPlaceholdersOrInvalidSlotsAreNotCounted() {
        PlayerItemStorage storage = new PlayerItemStorage();
        int note = 100_001;
        int placeholder = 100_002;
        storage.replace(InventoryID.BANK, new Item[]{ new Item(ItemID.DRAGON_PICKAXE, 2), new Item(note, 3), new Item(placeholder, 5), new Item(-1, 10), new Item(ItemID.COINS, 0), null}, id -> id == note ? ItemID.DRAGON_PICKAXE : id == placeholder ? -1 : id);
        assertEquals(5, storage.quantity(ItemID.DRAGON_PICKAXE));
        assertEquals(0, storage.quantity(note));
        assertEquals(0, storage.quantity(placeholder));
    }
    @Test
    public void collectionLogAndSharedStorageDoNotInventOwnedItems() {
        PlayerItemStorage storage = new PlayerItemStorage();
        put(storage, InventoryID.COLLECTION_TRANSMIT, ItemID.COINS, 1000);
        put(storage, InventoryID.INV_GROUP_TEMP, ItemID.COINS, 1000);
        put(storage, InventoryID.RAIDS_SHAREDSTORAGE, ItemID.COINS, 1000);
        put(storage, InventoryID.INV_PLAYER_SNAPSHOT, ItemID.COINS, 1000);
        assertEquals(0, storage.quantity(ItemID.COINS));
    }
    @Test
    public void coinCoffersUseCorrectUnitsAndAreNotAccumulatedTwice() {
        PlayerItemStorage storage = new PlayerItemStorage();
        IntUnaryOperator varbits = id -> id == VarbitID.NZONE_CASH ? 12
                : id == VarbitID.BR_COFFER ? 3
                : id == VarbitID.BLAST_FURNACE_COFFER ? 100
                : id == VarbitID.MISC_COFFERS ? 200 : 0;
        IntUnaryOperator varps = id -> id == VarPlayerID.SCAR_ESSENCEMINE_COFFER ? 300 : 0;
        storage.refreshCoinValues(varbits, varps);
        storage.refreshCoinValues(varbits, varps);
        assertEquals(15_600, storage.quantity(ItemID.COINS));
        storage.refreshCoinValues(id -> -1, id -> -1);
        assertEquals(0, storage.quantity(ItemID.COINS));
    }
    @Test
    public void goalCanCompleteThenRegressWhenThePlayerSpendsItems() {
        PlayerItemStorage storage = new PlayerItemStorage();
        PossessedItemMilestone goal = new PossessedItemMilestone(ItemID.COINS, 1_500_000_000L);
        goal.attach(storage, null);
        goal.refresh(null);
        assertEquals(MilestoneStatus.NOT_STARTED, goal.getStatus());
        put(storage, InventoryID.BANK, ItemID.COINS, 1_000_000_000);
        goal.refresh(null);
        assertEquals(MilestoneStatus.IN_PROGRESS, goal.getStatus());
        put(storage, InventoryID.INV, ItemID.COINS, 500_000_000);
        goal.refresh(null);
        assertEquals(MilestoneStatus.COMPLETE, goal.getStatus());
        put(storage, InventoryID.INV, ItemID.COINS, 100);
        assertTrue(goal.refresh(null));
        assertEquals(MilestoneStatus.IN_PROGRESS, goal.getStatus());
        assertFalse(goal.refresh(null));
    }
    @Test
    public void pouchContentsRequireAnOwnedPouchAndUseLiveQuantities() {
        PlayerItemStorage storage = new PlayerItemStorage();
        IntUnaryOperator values = id -> id == VarbitID.RUNE_POUCH_TYPE_1 ? 1
                : id == VarbitID.RUNE_POUCH_QUANTITY_1 ? 150 : 0;
        storage.refreshRuneValues(values, type -> ItemID.DRAGON_PICKAXE);
        assertEquals(0, storage.quantity(ItemID.DRAGON_PICKAXE));
        put(storage, InventoryID.INV, ItemID.BH_RUNE_POUCH, 1);
        storage.refreshRuneValues(values, type -> ItemID.DRAGON_PICKAXE);
        assertEquals(150, storage.quantity(ItemID.DRAGON_PICKAXE));
        storage.refreshRuneValues(values, type -> ItemID.DRAGON_PICKAXE);
        assertEquals(150, storage.quantity(ItemID.DRAGON_PICKAXE));
        storage.replace(InventoryID.INV, new Item[0], id -> id);
        storage.refreshRuneValues(values, type -> ItemID.DRAGON_PICKAXE);
        assertEquals(0, storage.quantity(ItemID.DRAGON_PICKAXE));
    }
    @Test
    public void profileSavesOnlyTheTargetAndKeepsDifferentItemsSeparate() {
        GrindstoneProfile profile = TestProfiles.populated(42);
        profile.replace(Collections.emptyList());
        PossessedItemMilestone coins = new PossessedItemMilestone(ItemID.COINS, 3_000_000_000L);
        assertTrue(profile.add(coins));
        assertFalse(profile.add(new PossessedItemMilestone(ItemID.COINS)));
        assertTrue(profile.add(new PossessedItemMilestone(ItemID.DRAGON_PICKAXE)));
        put(profile.getPlayerStorage(), InventoryID.BANK, ItemID.COINS, 1_500_000_000);
        coins.refresh(null);
        assertEquals(1_500_000_000L, coins.getProgressCurrent());
        GrindstoneProfile restored = TestProfiles.REPOSITORY.fromJson(TestProfiles.REPOSITORY.toJson(profile));
        PossessedItemMilestone saved = (PossessedItemMilestone) restored.getGoals().get(0);
        assertEquals(3_000_000_000L, saved.getAmount());
        assertEquals(0, saved.getProgressCurrent());
        assertEquals(0, restored.getPlayerStorage().quantity(ItemID.COINS));
    }
    @Test
    public void unknownBankIsExplicitAndSnapshotsResetBetweenLogins() {
        PlayerItemStorage storage = new PlayerItemStorage();
        assertTrue(storage.describe(ItemID.COINS).contains("Open your bank"));
        put(storage, InventoryID.BANK, ItemID.COINS, 100);
        assertFalse(storage.describe(ItemID.COINS).contains("Open your bank"));
        storage.reset();
        assertEquals(0, storage.quantity(ItemID.COINS));
        assertTrue(storage.describe(ItemID.COINS).contains("Open your bank"));
    }
    @Test(expected = IllegalArgumentException.class)
    public void invalidTargetCannotBeSaved() {
        new PossessedItemMilestone(ItemID.COINS, 0);
    }
    private void put(PlayerItemStorage storage, int container, int item, int amount) {
        storage.replace(container, new Item[]{new Item(item, amount)}, id -> id);
    }
}
