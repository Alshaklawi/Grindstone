package com.grinder.content.goal.impl.item;
import com.grinder.content.PlayerItemStorage;
import com.grinder.content.PlayerStorageSource;
import com.grinder.content.goal.Goal;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.goal.GoalStatus;
import com.grinder.ui.Icon;
import java.awt.image.BufferedImage;
import java.util.Locale;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class PossesedItemGoal implements Goal
{
    public static final long MAX_TARGET = 1_000_000_000_000L;
    private final int item;
    private long amount;
    private transient int normalizedItem;
    private transient long current;
    private transient PlayerItemStorage storage;
    private transient ItemManager itemManager;
    public PossesedItemGoal(int item, long amount)
    {
        if (item <= 0) { throw new IllegalArgumentException("Item ID must be positive"); }
        this.item = item;
        setTarget(amount);
    }
    public PossesedItemGoal(int item) { this(item, 1); }
    public int getItem() { return item; }
    public long getAmount() { return amount; }
    public void attach(PlayerItemStorage storage, ItemManager itemManager)
    { this.storage = storage; this.itemManager = itemManager; normalizedItem = 0; current = 0; }
    @Override public boolean supportsTarget() { return true; }
    @Override public void setTarget(long target)
    {
        if (target < 1 || target > MAX_TARGET)
        { throw new IllegalArgumentException("Quantity must be between 1 and 1,000,000,000,000"); }
        amount = target;
    }
    @Override public String getTitle(Client client)
    {
        net.runelite.api.ItemComposition definition = client == null ? null : client.getItemDefinition(item);
        return (definition == null ? "Item " + item : definition.getName()) + " owned";
    }
    @Override public BufferedImage getIcon(Client client)
    { return itemManager == null ? Icon.MAIN.getImage() : itemManager.getImage(item); }
    @Override public GoalCategory getCategory() { return GoalCategory.OTHER; }
    @Override public String getDescription()
    { return storage == null ? "Open your bank and item storage to include their contents." : storage.describe(normalizedItem > 0 ? normalizedItem : item); }
    @Override public boolean hasProgress() { return true; }
    @Override public long getProgressCurrent() { return current; }
    @Override public long getProgressMaximum() { return amount; }
    @Override public String getProgressText()
    { return String.format(Locale.ROOT, "%,d / %,d owned", current, amount); }
    @Override public GoalStatus getStatus()
    { return current >= amount ? GoalStatus.COMPLETE : current > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED; }
    @Override public boolean isAffectedBy(ItemContainer container)
    { return container != null && PlayerStorageSource.fromId(container.getId()) != null; }
    @Override public boolean refresh(Client client)
    {
        if (storage == null) { return false; }
        if (normalizedItem == 0) { normalizedItem = client == null ? item : storage.normalize(client, item); }
        long next = storage.quantity(normalizedItem);
        boolean changed = next != current;
        current = next;
        return changed;
    }
    @Override public com.grinder.ui.GoalComponent build(Client client)
    { refresh(client); return Goal.super.build(client); }
}
