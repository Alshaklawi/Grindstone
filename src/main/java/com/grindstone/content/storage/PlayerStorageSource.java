package com.grindstone.content.storage;
import net.runelite.api.gameval.InventoryID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum PlayerStorageSource {
    INVENTORY("Inventory", InventoryID.INV),
    EQUIPMENT("Equipment", InventoryID.WORN),
    BANK("Bank", InventoryID.BANK),
    LOOTING_BAG("Looting bag", InventoryID.LOOTING_BAG),
    SEED_BOX("Seed box", InventoryID.SEED_BOX),
    SEED_VAULT("Seed vault", InventoryID.SEED_VAULT),
    FORESTRY_KIT("Forestry kit", InventoryID.FORESTRY_KIT),
    TACKLE_BOX("Tackle box", InventoryID.TACKLE_BOX),
    HUNTSMANS_KIT("Huntsman's kit", InventoryID.HUNTSMANS_KIT),
    PREPOT("Chugging barrel", InventoryID.PREPOT_DEVICE_INV),
    DEATH("Death's Office", InventoryID.DEATH_PERMANENT),
    BOAT_1("Boat 1 cargo", InventoryID.SAILING_BOAT_1_CARGOHOLD),
    BOAT_2("Boat 2 cargo", InventoryID.SAILING_BOAT_2_CARGOHOLD),
    BOAT_3("Boat 3 cargo", InventoryID.SAILING_BOAT_3_CARGOHOLD),
    BOAT_4("Boat 4 cargo", InventoryID.SAILING_BOAT_4_CARGOHOLD),
    BOAT_5("Boat 5 cargo", InventoryID.SAILING_BOAT_5_CARGOHOLD);
    private final String name;
    private final int container;
    PlayerStorageSource(String name, int container) {
        this.name = name;
        this.container = container;
    }
    public int getContainerId() {
        return container;
    }
    @Override
    public String toString() {
        return name;
    }
    public static PlayerStorageSource fromId(int id) {
        for (PlayerStorageSource source : values()) {
            if (source.container == id) {
                return source;
            }
        }
        return null;
    }
}
