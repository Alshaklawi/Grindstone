package com.grinder.content;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;

import java.util.Arrays;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum CollectionLogEntry {

    VETION_AND_CALVARION(502),
    VENENATIS_AND_SPINDEL(501),
    CHAMBERS_OF_XERIC(507),
    WINTERTODT(504),
    CORPOREAL_BEAST(484),
    HESPORI(541);


    private static final int TITLE_PARAM_ID = 689;
    private static final int ENUM_PARAM_ID = 690;
    private final int struct;

    CollectionLogEntry(final int struct) {
        this.struct = struct;
    }


    public String getTitle(Client client) {
        return client.getStructComposition(struct).getStringValue(TITLE_PARAM_ID);
    }

    public ItemComposition[] getItems(Client client) {
        final int id = client.getStructComposition(struct).getIntValue(ENUM_PARAM_ID);
        return Arrays.stream(client.getEnum(id).getIntVals()).mapToObj(client::getItemDefinition).toArray(ItemComposition[]::new);
    }

    public boolean isObtained(Client client, ItemComposition item) {
        final ItemContainer container = client.getItemContainer(620);
        if (container == null) {
            return false;
        }
        return container.contains(item.getId());
    }

    public boolean isComplete(Client client) {
        return false;
    }

    public boolean contains(Client client, int item) {
        return Arrays.stream(getItems(client)).anyMatch(other -> other.getId() == item);
    }
}
