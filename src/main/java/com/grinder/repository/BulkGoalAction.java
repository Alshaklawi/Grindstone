package com.grinder.repository;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public enum BulkGoalAction {
    PIN("Pin selected"),
    UNPIN("Unpin selected"),
    CATEGORY("Move to category"),
    RESET_CATEGORY("Restore original categories"),
    SCHEDULE("Change repeat schedule"),
    DEADLINE("Set deadline / reminder"),
    EXAMPLE("Mark as examples"),
    PERSONAL("Use as real goals"),
    DELETE("Delete selected");

    private final String label;

    BulkGoalAction(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
