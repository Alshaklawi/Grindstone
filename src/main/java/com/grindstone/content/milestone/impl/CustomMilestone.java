package com.grindstone.content.milestone.impl;

import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.content.milestone.ManualMilestone;
import java.util.UUID;
import lombok.Getter;
import net.runelite.api.Client;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class CustomMilestone implements ManualMilestone {
    @Getter
    private final String id;
    private String title;
    @Getter
    private String description;
    @Getter
    private MilestoneCategory category;
    private long current;
    private long target;

    public CustomMilestone(String title, String description, MilestoneCategory category, long current, long target) {
        this(UUID.randomUUID().toString(), title, description, category, current, target);
    }

    public CustomMilestone(String id, String title, String description, MilestoneCategory category, long current, long target) {
        this.id = UUID.fromString(id).toString();
        update(title, description, category, current, target);
    }

    @Override
    public void resetProgress() {
        current = 0;
    }

    public void update(String title, String description, MilestoneCategory category, long current, long target) {
        String name = title == null ? "" : title.trim();
        String details = description == null ? "" : description.trim();
        if (name.isEmpty() || name.length() > 80) {
            throw new IllegalArgumentException("Use a title between 1 and 80 characters.");
        }
        if (details.length() > 2000) {
            throw new IllegalArgumentException("Keep the description within 2,000 characters.");
        }
        if (target < 1 || target > Integer.MAX_VALUE || current < 0 || current > target) {
            throw new IllegalArgumentException("Use a positive target and progress between zero and the target.");
        }
        this.title = name;
        this.description = details;
        this.category = category == null ? MilestoneCategory.OTHER : category;
        this.current = current;
        this.target = target;
    }

    @Override
    public String getTitle(Client client) {
        return title;
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
        return target;
    }

    @Override
    public boolean supportsTarget() {
        return true;
    }

    @Override
    public void setTarget(long target) {
        update(title, description, category, current, target);
    }

    @Override
    public MilestoneStatus getStatus() {
        return current >= target ? MilestoneStatus.COMPLETE : current > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
    }
}
