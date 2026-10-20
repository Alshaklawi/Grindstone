package com.grindstone.content.milestone.impl;

import lombok.Getter;
import com.grindstone.content.milestone.ManualMilestone;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.content.milestone.MilestoneStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.runelite.api.Client;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class ChecklistMilestone implements ManualMilestone {
    @Getter
    private final String id;
    private final String title;
    @Getter
    private final String description;
    private final List<String> steps;
    private boolean[] checked;

    public ChecklistMilestone(String title, String description, List<String> steps) {
        this(UUID.randomUUID().toString(), title, description, steps, new boolean[steps.size()]);
    }

    public ChecklistMilestone(String id, String title, String description, List<String> steps, boolean[] checked) {
        this.id = UUID.fromString(id).toString();
        this.title = title == null ? "" : title.trim();
        this.description = description == null ? "" : description.trim();
        this.steps = new ArrayList<>(steps);
        this.checked = checked.clone();
        validate();
    }

    public void validate() {
        UUID.fromString(id);
        if (title == null || title.isEmpty() || title.length() > 80 || description == null || description.length() > 2000 || steps == null || steps.isEmpty() || steps.size() > 100 || checked == null || checked.length != steps.size()) {
            throw new IllegalArgumentException("Use a name, optional description and 1 to 100 checklist steps.");
        }
        for (String step : steps) {
            if (step == null || step.trim().isEmpty() || step.length() > 200) {
                throw new IllegalArgumentException("Each step must contain 1 to 200 characters.");
            }
        }
        if (steps.stream().distinct().count() != steps.size()) {
            throw new IllegalArgumentException("Use distinct checklist step names.");
        }
    }


    public List<String> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public boolean[] getChecks() {
        return checked.clone();
    }

    public void setChecked(int index, boolean value) {
        checked[index] = value;
    }

    public void restoreChecks(boolean[] checks) {
        if (checks.length != steps.size()) {
            throw new IllegalArgumentException("Checklist shape changed.");
        }
        checked = checks.clone();
    }

    @Override
    public void resetProgress() {
        Arrays.fill(checked, false);
    }

    @Override
    public String getTitle(Client client) {
        return title;
    }


    @Override
    public MilestoneCategory getCategory() {
        return MilestoneCategory.OTHER;
    }

    @Override
    public boolean hasProgress() {
        return true;
    }

    @Override
    public long getProgressCurrent() {
        int count = 0;
        for (boolean done : checked) {
            if (done) {
                count++;
            }
        }
        return count;
    }

    @Override
    public long getProgressMaximum() {
        return steps.size();
    }

    @Override
    public MilestoneStatus getStatus() {
        return getProgressCurrent() == steps.size() ? MilestoneStatus.COMPLETE : getProgressCurrent() > 0 ? MilestoneStatus.IN_PROGRESS : MilestoneStatus.NOT_STARTED;
    }
}
