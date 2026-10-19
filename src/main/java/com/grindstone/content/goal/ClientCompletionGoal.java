package com.grindstone.content.goal;
import com.grindstone.config.GrindstoneConfig;
import com.grindstone.ui.components.GoalComponent;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public abstract class ClientCompletionGoal implements Goal {
    private transient long current;
    private transient long maximum;
    private transient boolean started;
    private transient boolean initialized;
    public abstract String selectionKey();
    protected boolean update(long current, long maximum, boolean started) {
        current = Math.max(0, current);
        maximum = Math.max(0, maximum);
        boolean changed = this.current != current || this.maximum != maximum || this.started != started;
        this.current = current;
        this.maximum = maximum;
        this.started = started;
        initialized = true;
        return changed;
    }
    @Override
    public boolean hasProgress() {
        return maximum > 0;
    }
    @Override
    public long getProgressCurrent() {
        return current;
    }
    @Override
    public long getProgressMaximum() {
        return maximum;
    }
    @Override
    public GoalStatus getStatus() {
        return maximum > 0 && current >= maximum ? GoalStatus.COMPLETE : started || current > 0 ? GoalStatus.IN_PROGRESS : GoalStatus.NOT_STARTED;
    }
    @Override
    public BufferedImage getIcon(Client client) {
        return getCategory().getIcon().getImage();
    }
    @Override
    public boolean isAffectedBy(ItemContainer container) {
        return false;
    }
    @Override
    public Supplier<GoalComponent> prepare(Client client, GrindstoneConfig config) {
        if (!initialized) {
            refresh(client);
        }
        return Goal.super.prepare(client, config);
    }
}
