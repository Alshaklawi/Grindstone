package com.grindstone.content.milestone.impl.item;
import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.storage.PlayerItemStorage;
import com.grindstone.content.storage.PlayerStorageSource;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.MilestoneComponent;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.function.Supplier;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class PossessedItemMilestone implements Milestone {
    public static final long MAX_TARGET = 1_000_000_000_000L;
    @Getter
    private final int item;
    @Getter
    private long amount;
    private transient int normalizedItem;
    private transient long current;
    private transient PlayerItemStorage storage;
    private transient ItemManager itemManager;
    public PossessedItemMilestone(int item, long amount) {
        if (item <= 0) {
            throw new IllegalArgumentException("Item ID must be positive");
        }
        this.item = item;
        setTarget(amount);
    }
    public PossessedItemMilestone(int item) {
        this(item, 1);
    }
    public void attach(PlayerItemStorage storage, ItemManager itemManager) {
        this.storage = storage;
        this.itemManager = itemManager;
        normalizedItem = 0;
        current = 0;
    }
    @Override
    public boolean supportsTarget() {
        return true;
    }
    @Override
    public void setTarget(long target) {
        if (target < 1 || target > MAX_TARGET) {
            throw new IllegalArgumentException("Quantity must be between 1 and 1,000,000,000,000");
        }
        amount = target;
    }
    @Override
    public String getTitle(Client client) {
        ItemComposition definition = client == null ? null : client.getItemDefinition(item);
        return (definition == null ? "Item " + item : definition.getName()) + " owned";
    }
    @Override
    public BufferedImage getIcon(Client client) {
        return itemManager == null ? Icon.MAIN.getImage() : itemManager.getImage(item);
    }
    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.ITEMS;
    }
    @Override
    public String getDescription() {
        return "Normal and noted quantities are combined; bank placeholders are excluded. " + (storage == null ? "Open your bank and item storage to include their contents." : storage.describe(normalizedItem > 0 ? normalizedItem : item));
    }
    @Override
    public String getCardDescription() {
        return "";
    }
    @Override
    public boolean hasProgress() {
        return true;
    }
    @Override
    public long getProgressCurrent() {
        return current;
    }
    @Override
    public long getProgressMaximum() {
        return amount;
    }
    @Override
    public String getProgressText() {
        return String.format(Locale.ROOT, "%,d / %,d owned", current, amount);
    }
    @Override
    public MilestoneStatus getStatus() {
        return current >= amount ? MilestoneStatus.COMPLETE : current > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
    }
    @Override
    public boolean isAffectedBy(ItemContainer container) {
        return container != null && PlayerStorageSource.fromId(container.getId()) != null;
    }
    @Override
    public boolean refresh(Client client) {
        if (storage == null) {
            return false;
        }
        if (normalizedItem == 0) {
            normalizedItem = client == null ? item : storage.normalize(client, item);
        }
        long next = storage.getQuantity(normalizedItem);
        boolean changed = next != current;
        current = next;
        return changed;
    }
    @Override
    public Supplier<MilestoneComponent> prepare(Client client, GrindstoneConfig config) {
        refresh(client);
        Supplier<MilestoneComponent> prepared = Milestone.super.prepare(client, config);
        long quantity = amount;
        return () -> {
            MilestoneComponent component = prepared.get();
            component.setRequiredQuantity(quantity);
            return component;
        };
    }
}
