package com.grindstone.util;

import com.grindstone.content.storage.PlayerItemStorage;
import net.runelite.api.Item;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class ItemVariantsTest {
    @Test
    public void resolvesNotedAndPlaceholderIdsToTheirNormalItem() {
        assertEquals(ItemID.DRAGON_PICKAXE, ItemVariants.getNormalId(100_001, 1, ItemID.DRAGON_PICKAXE, -1, -1));
        assertEquals(ItemID.DRAGON_PICKAXE, ItemVariants.getNormalId(100_002, -1, -1, 1, ItemID.DRAGON_PICKAXE));
        assertEquals(ItemID.DRAGON_PICKAXE, ItemVariants.getNormalId(ItemID.DRAGON_PICKAXE, -1, 100_001, -1, 100_002));
        assertEquals(-1, ItemVariants.getNormalId(100_001, 1, -1, -1, -1));
        assertEquals(-1, ItemVariants.getNormalId(100_002, -1, -1, 1, -1));
        assertEquals(-1, ItemVariants.getNormalId(-1, -1, -1, -1, -1));
    }

    @Test
    public void notedAndNormalStacksShareOneQuantityWithoutCountingPlaceholders() {
        PlayerItemStorage storage = new PlayerItemStorage();
        int note = 100_001;
        int placeholder = 100_002;
        storage.replace(InventoryID.INV, new Item[]{new Item(ItemID.DRAGON_PICKAXE, 2), new Item(note, 3)}, id -> ItemVariants.getNormalId(id, id == note ? 1 : -1, ItemID.DRAGON_PICKAXE, -1, -1));
        storage.replace(InventoryID.BANK, new Item[]{new Item(ItemID.DRAGON_PICKAXE, 4), new Item(placeholder, 1)}, id -> id == placeholder ? -1 : id);
        assertEquals(9, storage.getQuantity(ItemID.DRAGON_PICKAXE));
        assertEquals(0, storage.getQuantity(note));
        assertEquals(0, storage.getQuantity(placeholder));
    }
}
