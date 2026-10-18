package com.grinder.content.goal.impl.item;

import com.grinder.content.goal.Goal;
import lombok.Getter;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class PossesedItemGoal implements Goal {

    @Getter
    protected final int item;

    @Getter
    protected final int amount;

    public PossesedItemGoal(int item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    public PossesedItemGoal(int item) {
        this(item, 1);
    }
}
