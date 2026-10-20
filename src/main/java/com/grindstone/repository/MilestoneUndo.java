package com.grindstone.repository;

import lombok.Getter;
import java.util.function.BooleanSupplier;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class MilestoneUndo {
    private GrindstoneProfile profile;
    private long revision;
    private Runnable action;
    private BooleanSupplier allowed;
    @Getter
    private String label;
    public void record(GrindstoneProfile profile, String label, Runnable action, BooleanSupplier allowed) {
        this.profile = profile;
        this.revision = profile.getRevision();
        this.label = label;
        this.action = action;
        this.allowed = allowed;
    }
    public void clear() {
        profile = null;
        action = null;
        allowed = null;
        label = null;
    }
    public boolean undo(GrindstoneProfile active) {
        boolean valid = active != null && active == profile && active.getRevision() == revision && action != null && allowed.getAsBoolean();
        Runnable saved = action;
        clear();
        if (valid) {
            try {
                saved.run();
            } catch (IllegalStateException exception) {
                return false;
            }
        }
        return valid;
    }
}
