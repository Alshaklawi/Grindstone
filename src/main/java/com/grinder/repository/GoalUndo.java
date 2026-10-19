package com.grinder.repository;

import java.util.function.BooleanSupplier;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class GoalUndo {
    private GrinderProfile profile;
    private long revision;
    private Runnable action;
    private BooleanSupplier allowed;
    private String label;
    public void record(GrinderProfile profile, String label, Runnable action, BooleanSupplier allowed) {
        this.profile = profile;
        this.revision = profile.revision();
        this.label = label;
        this.action = action;
        this.allowed = allowed;
    }
    public String getLabel() {
        return label;
    }
    public void clear() {
        profile = null;
        action = null;
        allowed = null;
        label = null;
    }
    public boolean undo(GrinderProfile active) {
        boolean valid = active != null && active == profile && active.revision() == revision && action != null && allowed.getAsBoolean();
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
