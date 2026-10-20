package com.grindstone.content.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
@Getter
public final class PlayerStorageSnapshot {
    private final Map<Integer, Long> items;
    private final long updatedAt;

    public PlayerStorageSnapshot(Map<Integer, Long> items, long updatedAt) {
        if (items == null || items.size() > 10000 || updatedAt <= 0) {
            throw new IllegalArgumentException("Invalid storage snapshot");
        }
        Map<Integer, Long> copied = new HashMap<>();
        items.forEach((item, quantity) -> {
            if (item == null || item <= 0 || quantity == null || quantity <= 0 || quantity > 100L * Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid storage item quantity");
            }
            copied.put(item, quantity);
        });
        this.items = Collections.unmodifiableMap(copied);
        this.updatedAt = updatedAt;
    }
}
