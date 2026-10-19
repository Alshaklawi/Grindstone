
package com.grinder.content.collectionlog;

import lombok.Getter;
import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum CollectionLogEntry {
    VETION_AND_CALVARION(502, "Vet'ion and Calvar'ion"),
    VENENATIS_AND_SPINDEL(501, "Venenatis and Spindel"),
    CHAMBERS_OF_XERIC(507, "Chambers of Xeric"),
    WINTERTODT(504, "Wintertodt"),
    CORPOREAL_BEAST(484, "Corporeal Beast"),
    HESPORI(541, "Hespori");

    private static final int COLLECTION_LOG_CONTAINER_ID = InventoryID.COLLECTION_TRANSMIT;
    private static final int TITLE_PARAM_ID = 689;
    private static final int ENUM_PARAM_ID = 690;

    @Getter
    private final int struct;
    private final String displayName;

    CollectionLogEntry(final int struct, String displayName) {
        this.struct = struct;
        this.displayName = displayName;
    }

    public String getTitle(Client client) {
        if (client == null || client.getStructComposition(struct) == null) {
            return displayName;
        }

        String title = client.getStructComposition(struct).getStringValue(TITLE_PARAM_ID);
        return title == null || title.trim().isEmpty() ? displayName : title;
    }

    public ItemComposition[] getItems(Client client) {
        if (client == null || client.getStructComposition(struct) == null) {
            return new ItemComposition[0];
        }

        int enumId = client.getStructComposition(struct).getIntValue(ENUM_PARAM_ID);
        if (enumId <= 0 || client.getEnum(enumId) == null || client.getEnum(enumId).getIntVals() == null) {
            return new ItemComposition[0];
        }

        int[] itemIds = client.getEnum(enumId).getIntVals();

        return Arrays.stream(itemIds)
                .mapToObj(client::getItemDefinition)
                .toArray(ItemComposition[]::new);
    }

    public boolean isObtained(Client client, ItemComposition item) {
        if (item == null) {
            return false;
        }

        return isObtained(client.getItemContainer(COLLECTION_LOG_CONTAINER_ID), item.getId());
    }

    public boolean isObtained(ItemContainer container, ItemComposition item) {
        return item != null && isObtained(container, item.getId());
    }

    public boolean isObtained(ItemContainer container, int itemId) {
        return container != null && itemId > 0 && container.contains(itemId);
    }

    public boolean isComplete(Client client) {
        ItemComposition[] items = getItems(client);
        ItemContainer container = client.getItemContainer(COLLECTION_LOG_CONTAINER_ID);

        if (items.length == 0) {
            return false;
        }

        for (ItemComposition item : items) {
            if (item != null && !isObtained(container, item.getId())) {
                return false;
            }
        }

        return true;
    }

    public boolean contains(Client client, int itemId) {
        if (client == null || client.getStructComposition(struct) == null) {
            return false;
        }

        int enumId = client.getStructComposition(struct).getIntValue(ENUM_PARAM_ID);
        return enumId > 0 && client.getEnum(enumId) != null && client.getEnum(enumId).getIntVals() != null && Arrays.stream(client.getEnum(enumId).getIntVals()).anyMatch(id -> id == itemId);
    }
}
