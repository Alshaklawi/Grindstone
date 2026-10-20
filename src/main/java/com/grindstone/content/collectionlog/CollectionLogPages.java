package com.grindstone.content.collectionlog;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
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
    private static final int TABS_ENUM = 2102;
    private static final int ITEM_REPLACEMENTS_ENUM = 3721;
    private static volatile Map<Integer, Integer> replacements = Collections.emptyMap();
    private static final int TAB_PAGES_PARAM = 683;
    private static final int PAGE_TITLE_PARAM = 689;
    private static final int PAGE_ITEMS_PARAM = 690;
    private CollectionLogPages() {
    }
    public static void refreshReplacements(Client client) {
        EnumComposition mapping = client.getEnum(ITEM_REPLACEMENTS_ENUM);
        if (mapping == null || mapping.getKeys() == null || mapping.getIntVals() == null || mapping.getKeys().length != mapping.getIntVals().length) {
            return;
        }
        Map<Integer, Integer> values = new HashMap<>();
        for (int index = 0; index < mapping.getKeys().length; index++) {
            if (mapping.getKeys()[index] > 0 && mapping.getIntVals()[index] > 0) {
                values.put(mapping.getKeys()[index], mapping.getIntVals()[index]);
            }
        }
        replacements = Collections.unmodifiableMap(values);
    }

    public static int getCanonicalItemId(int item) {
        Integer replacement = replacements.get(item);
        if (replacement != null) {
            return replacement;
        }
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
        refreshReplacements(client);
        EnumComposition tabs = client.getEnum(TABS_ENUM);
        if (tabs == null || tabs.getIntVals() == null) {
            return pages;
        }
        for (int id : tabs.getIntVals()) {
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
                String name = getTitle(client, page, "");
                if (!name.isEmpty()) {
                    pages.put(page, name);
                }
            }
        }
        return Collections.unmodifiableMap(pages);
    }
    public static String getTitle(Client client, int page, String fallback) {
        StructComposition struct = client == null ? null : client.getStructComposition(page);
        String name = struct == null ? null : struct.getStringValue(PAGE_TITLE_PARAM);
        return name == null || name.trim().isEmpty() || "null".equals(name) ? fallback : name;
    }
    public static int[] getItemIds(Client client, int page) {
        StructComposition struct = client == null ? null : client.getStructComposition(page);
        if (struct == null) {
            return new int[0];
        }
        int enumId = struct.getIntValue(PAGE_ITEMS_PARAM);
        EnumComposition items = enumId <= 0 ? null : client.getEnum(enumId);
        return items == null || items.getIntVals() == null ? new int[0] : Arrays.stream(items.getIntVals()).filter(id -> id > 0).map(CollectionLogPages::getCanonicalItemId).distinct().toArray();
    }
    public static ItemComposition[] getItems(Client client, int page) {
        if (client == null) {
            return new ItemComposition[0];
        }
        return Arrays.stream(getItemIds(client, page)).mapToObj(client::getItemDefinition).filter(Objects::nonNull).toArray(ItemComposition[]::new);
    }
}
