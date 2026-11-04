package com.grindstone.ui;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import com.grindstone.ui.components.MilestoneComponent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@Value
class MilestoneView {
    Milestone goal;
    MilestoneComponent component;
    String title;
    String searchText;
    MilestoneCategory category;
    MilestoneStatus status;
    @Getter(AccessLevel.NONE)
    boolean hasProgress;
    long progressCurrent;
    long progressMaximum;

    public boolean hasProgress() {
        return hasProgress;
    }
}
