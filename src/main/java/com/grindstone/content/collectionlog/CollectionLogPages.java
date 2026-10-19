package com.grindstone.content.collectionlog;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.ItemComposition;
import net.runelite.api.StructComposition;
import net.runelite.api.gameval.ItemID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class CollectionLogPages {
    private static final int[] TAB_STRUCTS = {471, 472, 473, 474, 475};
    private static final int TAB_PAGES_PARAM = 683;
    private static final int PAGE_TITLE_PARAM = 689;
    private static final int PAGE_ITEMS_PARAM = 690;
    private CollectionLogPages() {
    }
    public static int canonicalItemId(int item) {
        switch (item) {
            case ItemID.TOL_TEA: return ItemID.TOL_TEA_DUMMY;
            case ItemID.TOL_PLAIN_SACK: return ItemID.TOL_PLAIN_SACK_DUMMY;
            case ItemID.TOL_GREEN_SACK: return ItemID.TOL_GREEN_SACK_DUMMY;
            case ItemID.TOL_RED_SACK: return ItemID.TOL_RED_SACK_DUMMY;
            case ItemID.TOL_BLACK_SACK: return ItemID.TOL_BLACK_SACK_DUMMY;
            case ItemID.TOL_GOLD_SACK: return ItemID.TOL_GOLD_SACK_DUMMY;
            case ItemID.TOL_RUNE_SACK: return ItemID.TOL_RUNE_SACK_DUMMY;
            case ItemID.ABYSSALSIRE_UNSIRED: return ItemID.ABYSSALSIRE_UNSIRED_DUMMY;
            case ItemID.COAL_BAG: return ItemID.COAL_BAG_DUMMY;
            case ItemID.GEM_BAG: return ItemID.GEM_BAG_DUMMY;
            case ItemID.PLANK_SACK: return ItemID.PLANK_SACK_DUMMY;
            case ItemID.FLAMTAER_BAG: return ItemID.FLAMTAER_BAG_DUMMY;
            default: return item;
        }
    }
    public static Map<Integer, String> discover(Client client) {
        Map<Integer, String> pages = new LinkedHashMap<>();
        if (client == null) {
            return pages;
        }
        for (int id : TAB_STRUCTS) {
            StructComposition tab = client.getStructComposition(id);
            if (tab == null) {
                continue;
            }
            EnumComposition entries = client.getEnum(tab.getIntValue(TAB_PAGES_PARAM));
            if (entries == null || entries.getIntVals() == null) {
                continue;
            }
            for (int page : entries.getIntVals()) {
                if (page <= 0) {
                    continue;
                }
                String name = title(client, page, "");
                if (!name.isEmpty()) {
                    pages.put(page, name);
                }
            }
        }
        return Collections.unmodifiableMap(pages);
    }
    public static String title(Client client, int page, String fallback) {
        StructComposition struct = client == null ? null : client.getStructComposition(page);
        String name = struct == null ? null : struct.getStringValue(PAGE_TITLE_PARAM);
        return name == null || name.trim().isEmpty() || "null".equals(name) ? fallback : name;
    }
    public static int[] itemIds(Client client, int page) {
        StructComposition struct = client == null ? null : client.getStructComposition(page);
        if (struct == null) {
            return new int[0];
        }
        int enumId = struct.getIntValue(PAGE_ITEMS_PARAM);
        EnumComposition items = enumId <= 0 ? null : client.getEnum(enumId);
        return items == null || items.getIntVals() == null ? new int[0] : Arrays.stream(items.getIntVals()).filter(id -> id > 0).map(CollectionLogPages::canonicalItemId).distinct().toArray();
    }
    public static ItemComposition[] items(Client client, int page) {
        if (client == null) {
            return new ItemComposition[0];
        }
        return Arrays.stream(itemIds(client, page)).mapToObj(client::getItemDefinition).filter(Objects::nonNull).toArray(ItemComposition[]::new);
    }
}
