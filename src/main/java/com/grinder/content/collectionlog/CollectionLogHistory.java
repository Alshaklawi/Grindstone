package com.grinder.content.collectionlog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CollectionLogHistory {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
    private final Map<Integer, Long> firstSeen = new HashMap<>();

    public void observe(ItemContainer container) {
        if (container == null || container.getId() != InventoryID.COLLECTION_TRANSMIT) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Item item : container.getItems()) {
            if (item != null && item.getId() > 0 && item.getQuantity() > 0) {
                record(item.getId(), now);
            }
        }
    }

    public void record(int itemId, long epochMillis) {
        if (itemId <= 0 || epochMillis <= 0) {
            return;
        }
        firstSeen.merge(CollectionLogPages.canonicalItemId(itemId), epochMillis, Math::min);
    }

    public boolean contains(int itemId) {
        return firstSeen.containsKey(CollectionLogPages.canonicalItemId(itemId));
    }

    public Long firstSeenAt(int itemId) {
        return firstSeen.get(CollectionLogPages.canonicalItemId(itemId));
    }

    public Map<Integer, Long> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(firstSeen));
    }

    public String firstSeenText(int itemId) {
        Long time = firstSeenAt(itemId);
        return time == null ? "Not yet observed in your collection log" : "First seen: " + FORMAT.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()));
    }
}
