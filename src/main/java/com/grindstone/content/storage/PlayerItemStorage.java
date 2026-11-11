package com.grindstone.content.storage;
import com.grindstone.util.ItemVariants;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class PlayerItemStorage {
    private final Clock clock;
    private final Set<PlayerStorageSource> loadedThisSession = EnumSet.noneOf(PlayerStorageSource.class);
    private final Map<String, Instant> variableSeen = new LinkedHashMap<>();
    private final Map<PlayerStorageSource, Instant> lastSeen = new EnumMap<>(PlayerStorageSource.class);
    public PlayerItemStorage() {
        this(Clock.systemUTC());
    }
    public PlayerItemStorage(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }
    private final Map<PlayerStorageSource, Map<Integer, Long>> containers = new EnumMap<>(PlayerStorageSource.class);
    private final Map<String, Map<Integer, Long>> variables = new LinkedHashMap<>();
    private final Map<Integer, Integer> normalized = new HashMap<>();
    private static final int[] RUNE_TYPES = {VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6};
    private static final int[] RUNE_QUANTITIES = {VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6};
    public void reset() {
        containers.clear();
        variables.clear();
        normalized.clear();
        lastSeen.clear();
        variableSeen.clear();
        loadedThisSession.clear();
    }
    public void beginSession() {
        loadedThisSession.clear();
        normalized.clear();
    }

    public boolean isLoadedThisSession(PlayerStorageSource source) {
        return loadedThisSession.contains(source);
    }

    public Map<PlayerStorageSource, PlayerStorageSnapshot> getContainerSnapshots() {
        Map<PlayerStorageSource, PlayerStorageSnapshot> snapshots = new EnumMap<>(PlayerStorageSource.class);
        containers.forEach((source, items) -> snapshots.put(source, new PlayerStorageSnapshot(items, lastSeen.get(source).toEpochMilli())));
        return Collections.unmodifiableMap(snapshots);
    }

    public Map<String, PlayerStorageSnapshot> getVariableSnapshots() {
        Map<String, PlayerStorageSnapshot> snapshots = new LinkedHashMap<>();
        variables.forEach((source, items) -> snapshots.put(source, new PlayerStorageSnapshot(items, variableSeen.get(source).toEpochMilli())));
        return Collections.unmodifiableMap(snapshots);
    }

    public void restoreContainerSnapshot(PlayerStorageSource source, PlayerStorageSnapshot snapshot) {
        Objects.requireNonNull(source);
        PlayerStorageSnapshot checked = new PlayerStorageSnapshot(snapshot.getItems(), snapshot.getUpdatedAt());
        Instant updatedAt = Instant.ofEpochMilli(checked.getUpdatedAt());
        if (!lastSeen.containsKey(source) || updatedAt.isAfter(lastSeen.get(source))) {
            containers.put(source, new HashMap<>(checked.getItems()));
            lastSeen.put(source, updatedAt);
            loadedThisSession.remove(source);
        }
    }

    public void restoreVariableSnapshot(String source, PlayerStorageSnapshot snapshot) {
        if (!Set.of("Rune pouch", "Nightmare Zone coffer", "Last Man Standing coffer", "Blast Furnace coffer", "Miscellania coffer", "Scar essence mine coffer").contains(source)) {
            throw new IllegalArgumentException("Unknown storage source");
        }
        PlayerStorageSnapshot checked = new PlayerStorageSnapshot(snapshot.getItems(), snapshot.getUpdatedAt());
        Instant updatedAt = Instant.ofEpochMilli(checked.getUpdatedAt());
        if (!variableSeen.containsKey(source) || updatedAt.isAfter(variableSeen.get(source))) {
            variables.put(source, new HashMap<>(checked.getItems()));
            variableSeen.put(source, updatedAt);
        }
    }

    public void mergeSnapshots(PlayerItemStorage saved) {
        saved.getContainerSnapshots().forEach(this::restoreContainerSnapshot);
        saved.getVariableSnapshots().forEach(this::restoreVariableSnapshot);
    }

    public void captureLoaded(Client client) {
        for (PlayerStorageSource source : PlayerStorageSource.values()) {
            observe(client, client.getItemContainer(source.getContainerId()));
        }
        refreshVariables(client);
    }
    public boolean observe(Client client, ItemContainer container) {
        if (container == null || PlayerStorageSource.fromId(container.getId()) == null) {
            return false;
        }
        replace(container.getId(), container.getItems(), id -> normalize(client, id));
        return true;
    }
    public void replace(int containerId, Item[] items, IntUnaryOperator normalize) {
        PlayerStorageSource source = PlayerStorageSource.fromId(containerId);
        if (source == null || items == null) {
            return;
        }
        Map<Integer, Long> counts = new HashMap<>();
        for (Item item : items) {
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0) {
                continue;
            }
            int id = normalize.applyAsInt(item.getId());
            if (id > 0) {
                counts.merge(id, (long) item.getQuantity(), Long::sum);
            }
        }
        containers.put(source, counts);
        lastSeen.put(source, Instant.ofEpochMilli(clock.millis()));
        loadedThisSession.add(source);
    }
    public Map<PlayerStorageSource, Instant> getLastSeen() {
        return Collections.unmodifiableMap(new EnumMap<>(lastSeen));
    }
    public String getFreshness() {
        Instant seen = lastSeen.get(PlayerStorageSource.BANK);
        return seen == null ? "No bank snapshot saved" : "Bank snapshot: " + DateTimeFormatter.ofPattern("HH:mm:ss z", Locale.ROOT).withZone(ZoneId.systemDefault()).format(seen);
    }
    public String getSnapshotLabel() {
        Instant seen = lastSeen.values().stream().max(Instant::compareTo).orElse(null);
        return "Last snapshot: " + (seen == null ? "—" : DateTimeFormatter.ofPattern("HH:mm:ss z", Locale.ROOT).withZone(ZoneId.systemDefault()).format(seen));
    }
    public boolean hasBankSnapshot() {
        return containers.containsKey(PlayerStorageSource.BANK);
    }
    public int normalize(Client client, int id) {
        return normalized.computeIfAbsent(id, key -> {
            ItemComposition definition = client.getItemDefinition(key);
            if (definition == null) {
                return key;
            }
            if (definition.getPlaceholderTemplateId() != -1) {
                return -1;
            }
            return ItemVariants.getNormalId(definition);
        });
    }
    public boolean refreshVariables(Client client) {
        boolean changed = refreshCoinValues(client::getVarbitValue, client::getVarpValue);
        EnumComposition mapping = client.getEnum(EnumID.RUNEPOUCH_RUNE);
        return refreshRuneValues(client::getVarbitValue, type -> mapping == null ? -1 : mapping.getIntValue(type)) || changed;
    }
    public boolean refreshRuneValues(IntUnaryOperator varbits, IntUnaryOperator mapping) {
        Map<Integer, Long> runes = new HashMap<>();
        boolean hasPouch = getQuantity(ItemID.BH_RUNE_POUCH) > 0
        || getQuantity(ItemID.BH_RUNE_POUCH_TROUVER) > 0
        || getQuantity(ItemID.DIVINE_RUNE_POUCH) > 0
        || getQuantity(ItemID.DIVINE_RUNE_POUCH_TROUVER) > 0;
        if (hasPouch) {
            for (int slot = 0; slot < RUNE_TYPES.length; slot++) {
                int type = varbits.applyAsInt(RUNE_TYPES[slot]);
                int quantity = varbits.applyAsInt(RUNE_QUANTITIES[slot]);
                if (type > 0 && quantity > 0) {
                    int rune = mapping.applyAsInt(type);
                    if (rune > 0) {
                        runes.merge(rune, (long) quantity, Long::sum);
                    }
                }
            }
        }
        return updateVariable("Rune pouch", runes);
    }
    public boolean refreshCoinValues(IntUnaryOperator varbits, IntUnaryOperator varps) {
        boolean changed = setCoins("Nightmare Zone coffer", Math.max(0L, varbits.applyAsInt(VarbitID.NZONE_CASH)) * 1000L);
        changed |= setCoins("Last Man Standing coffer", Math.max(0L, varbits.applyAsInt(VarbitID.BR_COFFER)) * 1000L);
        changed |= setCoins("Blast Furnace coffer", varbits.applyAsInt(VarbitID.BLAST_FURNACE_COFFER));
        changed |= setCoins("Miscellania coffer", varbits.applyAsInt(VarbitID.MISC_COFFERS));
        changed |= setCoins("Scar essence mine coffer", varps.applyAsInt(VarPlayerID.SCAR_ESSENCEMINE_COFFER));
        return changed;
    }
    private boolean setCoins(String source, long quantity) {
        Map<Integer, Long> coins = new HashMap<>();
        if (quantity > 0) {
            coins.put(ItemID.COINS, quantity);
        }
        return updateVariable(source, coins);
    }

    private boolean updateVariable(String source, Map<Integer, Long> values) {
        if (values.equals(variables.get(source))) {
            return false;
        }
        variables.put(source, values);
        variableSeen.put(source, Instant.ofEpochMilli(clock.millis()));
        return true;
    }
    public Map<String, Long> getBreakdown(int item) {
        Map<String, Long> result = new LinkedHashMap<>();
        containers.forEach((source, counts) -> {
            long quantity = counts.getOrDefault(item, 0L);
            if (quantity > 0) {
                result.put(source.toString(), quantity);
            }
        });
        variables.forEach((source, counts) -> {
            long quantity = counts.getOrDefault(item, 0L);
            if (quantity > 0) {
                result.put(source, quantity);
            }
        });
        return result;
    }
    public long getQuantity(int item) {
        long quantity = 0;
        for (Map<Integer, Long> counts : containers.values()) {
            quantity += counts.getOrDefault(item, 0L);
        }
        for (Map<Integer, Long> counts : variables.values()) {
            quantity += counts.getOrDefault(item, 0L);
        }
        return quantity;
    }
    public String describe(int item) {
        String amounts = getBreakdown(item).entrySet().stream()
                .map(entry -> entry.getKey() + ": " + String.format(Locale.ROOT, "%,d", entry.getValue()))
                .collect(Collectors.joining("; "));
        return (amounts.isEmpty() ? "No items found in known storage." : amounts + ".")
                + (containers.containsKey(PlayerStorageSource.BANK) ? "" : " Open your bank to include it.")
                + " Reopen storage to refresh saved contents. Excludes shared storage, GE offers, POH storage and deathpiles.";
    }
}
