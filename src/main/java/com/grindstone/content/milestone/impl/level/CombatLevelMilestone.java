package com.grindstone.content.milestone.impl.level;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.MilestoneComponent;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Player;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class CombatLevelMilestone implements Milestone {
    private static final int MAX_COMBAT_LEVEL = 126;
    private int target;

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        if (target < 3 || target > MAX_COMBAT_LEVEL) {
            throw new IllegalArgumentException("Choose combat level from 3 to 126");
        }
        this.target = (int) target;
    }
    private transient int current;

    @Override
    public String getTitle(Client client) {
        return "Combat level";
    }

    @Override
    public BufferedImage getIcon(Client client) {
        return Icon.BOSSING.getImage();
    }

    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.BOSSING;
    }

    @Override
    public MilestoneStatus getStatus() {
        return current >= getProgressMaximum() ? MilestoneStatus.COMPLETE
                : current > 3 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
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
        return target > 0 ? target : MAX_COMBAT_LEVEL;
    }

    @Override
    public String getProgressText() {
        return current + " / " + getProgressMaximum();
    }

    @Override
    public boolean refresh(Client client) {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        int next = Math.max(3, player.getCombatLevel());
        if (next == current) {
            return false;
        }

        current = next;
        return true;
    }

    @Override
    public Supplier<MilestoneComponent> prepare(Client client, GrindstoneConfig config) {
        refresh(client);
        return Milestone.super.prepare(client, config);
    }
}
