package com.grinder.content.storage;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Locale;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EnumComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class PlayerItemStorage {
    private final java.time.Clock clock;
    private final Map<PlayerStorageSource, java.time.Instant> lastSeen = new EnumMap<>(PlayerStorageSource.class);
    public PlayerItemStorage() { this(java.time.Clock.systemUTC()); }
    public PlayerItemStorage(java.time.Clock clock) { this.clock = java.util.Objects.requireNonNull(clock); }
    private final Map<PlayerStorageSource, Map<Integer, Long>> containers = new EnumMap<>(PlayerStorageSource.class);
    private final Map<String, Map<Integer, Long>> variables = new LinkedHashMap<>();
    private final Map<Integer, Integer> normalized = new HashMap<>();
    private static final int[] RUNE_TYPES = {VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6};
    private static final int[] RUNE_QUANTITIES = {VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6};
    public void reset()
    { containers.clear(); variables.clear(); normalized.clear(); lastSeen.clear(); }
    public void captureLoaded(Client client) {
        for (PlayerStorageSource source : PlayerStorageSource.values())
        { observe(client, client.getItemContainer(source.getContainerId())); }
        refreshVariables(client);
    }
    public boolean observe(Client client, ItemContainer container) {
        if (container == null || PlayerStorageSource.fromId(container.getId()) == null) { return false; }
        replace(container.getId(), container.getItems(), id -> normalize(client, id));
        return true;
    }
    public void replace(int containerId, Item[] items, IntUnaryOperator normalize) {
        PlayerStorageSource source = PlayerStorageSource.fromId(containerId);
        if (source == null || items == null) { return; }
        Map<Integer, Long> counts = new HashMap<>();
        for (Item item : items) {
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0) { continue; }
            int id = normalize.applyAsInt(item.getId());
            if (id > 0) { counts.merge(id, (long) item.getQuantity(), Long::sum); }
        }
        containers.put(source, counts);
        lastSeen.put(source, clock.instant());
    }
    public Map<PlayerStorageSource, java.time.Instant> lastSeen() { return java.util.Collections.unmodifiableMap(new EnumMap<>(lastSeen)); }
    public String freshness() {
        java.time.Instant seen = lastSeen.get(PlayerStorageSource.BANK);
        return seen == null ? "Bank not loaded this login" : "Bank snapshot: " + java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss z", Locale.ROOT).withZone(java.time.ZoneId.systemDefault()).format(seen);
    }
    public int normalize(Client client, int id) {
        return normalized.computeIfAbsent(id, key -> {
            ItemComposition definition = client.getItemDefinition(key);
            if (definition == null) { return key; }
            if (definition.getPlaceholderTemplateId() != -1) { return -1; }
            return definition.getNote() != -1 ? definition.getLinkedNoteId() : key;
        });
    }
    public void refreshVariables(Client client) {
        refreshCoinValues(client::getVarbitValue, client::getVarpValue);
        EnumComposition mapping = client.getEnum(EnumID.RUNEPOUCH_RUNE);
        refreshRuneValues(client::getVarbitValue, type -> mapping == null ? -1 : mapping.getIntValue(type));
    }
    public void refreshRuneValues(IntUnaryOperator varbits, IntUnaryOperator mapping) {
        Map<Integer, Long> runes = new HashMap<>();
        boolean hasPouch = quantity(ItemID.BH_RUNE_POUCH) > 0
                || quantity(ItemID.BH_RUNE_POUCH_TROUVER) > 0
                || quantity(ItemID.DIVINE_RUNE_POUCH) > 0
                || quantity(ItemID.DIVINE_RUNE_POUCH_TROUVER) > 0;
        if (hasPouch) {
            for (int slot = 0; slot < RUNE_TYPES.length; slot++) {
                int type = varbits.applyAsInt(RUNE_TYPES[slot]);
                int quantity = varbits.applyAsInt(RUNE_QUANTITIES[slot]);
                if (type > 0 && quantity > 0) {
                    int rune = mapping.applyAsInt(type);
                    if (rune > 0) { runes.merge(rune, (long) quantity, Long::sum); }
                }
            }
        }
        variables.put("Rune pouch", runes);
    }
    public void refreshCoinValues(IntUnaryOperator varbits, IntUnaryOperator varps) {
        setCoins("Nightmare Zone coffer", Math.max(0L, varbits.applyAsInt(VarbitID.NZONE_CASH)) * 1000L);
        setCoins("Last Man Standing coffer", Math.max(0L, varbits.applyAsInt(VarbitID.BR_COFFER)) * 1000L);
        setCoins("Blast Furnace coffer", varbits.applyAsInt(VarbitID.BLAST_FURNACE_COFFER));
        setCoins("Miscellania coffer", varbits.applyAsInt(VarbitID.MISC_COFFERS));
        setCoins("Scar essence mine coffer", varps.applyAsInt(VarPlayerID.SCAR_ESSENCEMINE_COFFER));
    }
    private void setCoins(String source, long quantity) {
        Map<Integer, Long> coins = new HashMap<>();
        if (quantity > 0) { coins.put(ItemID.COINS, quantity); }
        variables.put(source, coins);
    }
    public Map<String, Long> breakdown(int item) {
        Map<String, Long> result = new LinkedHashMap<>();
        containers.forEach((source, counts) -> {
            long quantity = counts.getOrDefault(item, 0L);
            if (quantity > 0) { result.put(source.toString(), quantity); }
        });
        variables.forEach((source, counts) -> {
            long quantity = counts.getOrDefault(item, 0L);
            if (quantity > 0) { result.put(source, quantity); }
        });
        return result;
    }
    public long quantity(int item)
    { return breakdown(item).values().stream().mapToLong(Long::longValue).sum(); }
    public String describe(int item) {
        String amounts = breakdown(item).entrySet().stream()
                .map(entry -> entry.getKey() + ": " + String.format(Locale.ROOT, "%,d", entry.getValue()))
                .collect(Collectors.joining("; "));
        return (amounts.isEmpty() ? "No items found in known storage." : amounts + ".")
                + (containers.containsKey(PlayerStorageSource.BANK) ? "" : " Open your bank to include it.")
                + " Known contents this login only; open other storage to refresh it. Shared storage, GE offers, POH, deathpiles and unexposed storage are excluded.";
    }
}
