package com.grindstone.repository;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.impl.item.PossessedItemMilestone;
import com.grindstone.content.storage.PlayerItemStorage;
import com.grindstone.content.storage.PlayerStorageSource;
import java.util.Collections;
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
    public void combinesPersonalContainersWithLongArithmetic() {
        PlayerItemStorage storage = new PlayerItemStorage();
        put(storage, InventoryID.BANK, ItemID.COINS, 2_000_000_000);
        put(storage, InventoryID.INV, ItemID.COINS, 1_000_000_000);
        put(storage, InventoryID.LOOTING_BAG, ItemID.COINS, 500_000_000);
        assertEquals(3_500_000_000L, storage.getQuantity(ItemID.COINS));
        assertEquals(3, storage.getBreakdown(ItemID.COINS).size());
    }
    @Test
    public void snapshotsReplaceRatherThanAccumulateAndTransfersKeepTheTotal() {
        PlayerItemStorage storage = new PlayerItemStorage();
        put(storage, InventoryID.BANK, ItemID.COINS, 100);
        put(storage, InventoryID.INV, ItemID.COINS, 10);
        put(storage, InventoryID.BANK, ItemID.COINS, 90);
        put(storage, InventoryID.INV, ItemID.COINS, 20);
        assertEquals(110, storage.getQuantity(ItemID.COINS));
        storage.replace(InventoryID.BANK, new Item[0], id -> id);
        assertEquals(20, storage.getQuantity(ItemID.COINS));
    }
    @Test
    public void notesAreCombinedAndPlaceholdersOrInvalidSlotsAreNotCounted() {
        PlayerItemStorage storage = new PlayerItemStorage();
        int note = 100_001;
        int placeholder = 100_002;
        storage.replace(InventoryID.BANK, new Item[]{ new Item(ItemID.DRAGON_PICKAXE, 2), new Item(note, 3), new Item(placeholder, 5), new Item(-1, 10), new Item(ItemID.COINS, 0), null}, id -> id == note ? ItemID.DRAGON_PICKAXE : id == placeholder ? -1 : id);
        assertEquals(5, storage.getQuantity(ItemID.DRAGON_PICKAXE));
        assertEquals(0, storage.getQuantity(note));
        assertEquals(0, storage.getQuantity(placeholder));
    }
    @Test
    public void collectionLogAndSharedStorageDoNotInventOwnedItems() {
        PlayerItemStorage storage = new PlayerItemStorage();
        put(storage, InventoryID.COLLECTION_TRANSMIT, ItemID.COINS, 1000);
        put(storage, InventoryID.INV_GROUP_TEMP, ItemID.COINS, 1000);
        put(storage, InventoryID.RAIDS_SHAREDSTORAGE, ItemID.COINS, 1000);
        put(storage, InventoryID.INV_PLAYER_SNAPSHOT, ItemID.COINS, 1000);
        assertEquals(0, storage.getQuantity(ItemID.COINS));
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
        assertEquals(15_600, storage.getQuantity(ItemID.COINS));
        storage.refreshCoinValues(id -> -1, id -> -1);
        assertEquals(0, storage.getQuantity(ItemID.COINS));
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
        assertEquals(0, storage.getQuantity(ItemID.DRAGON_PICKAXE));
        put(storage, InventoryID.INV, ItemID.BH_RUNE_POUCH, 1);
        storage.refreshRuneValues(values, type -> ItemID.DRAGON_PICKAXE);
        assertEquals(150, storage.getQuantity(ItemID.DRAGON_PICKAXE));
        storage.refreshRuneValues(values, type -> ItemID.DRAGON_PICKAXE);
        assertEquals(150, storage.getQuantity(ItemID.DRAGON_PICKAXE));
        storage.replace(InventoryID.INV, new Item[0], id -> id);
        storage.refreshRuneValues(values, type -> ItemID.DRAGON_PICKAXE);
        assertEquals(0, storage.getQuantity(ItemID.DRAGON_PICKAXE));
    }
    @Test
    public void profileSavesTargetsAndStorageSnapshotsAndKeepsDifferentItemsSeparate() {
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
        assertEquals(1_500_000_000L, restored.getPlayerStorage().getQuantity(ItemID.COINS));
        assertEquals(profile.getPlayerStorage().getLastSeen(), restored.getPlayerStorage().getLastSeen());
        assertFalse(restored.getPlayerStorage().isLoadedThisSession(PlayerStorageSource.BANK));
        saved.refresh(null);
        assertEquals(1_500_000_000L, saved.getProgressCurrent());
    }
    private void put(PlayerItemStorage storage, int container, int item, int amount) {
        storage.replace(container, new Item[]{new Item(item, amount)}, id -> id);
    }
}
