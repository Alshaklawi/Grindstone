package com.grinder.ui.util;

import com.grinder.content.goal.Goal;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public final class GoalCardCache<T> {
    private final Map<Goal, List<?>> keys = new IdentityHashMap<>();
    private final Map<Goal, T> cards = new IdentityHashMap<>();
    public T get(Goal goal, List<?> key) { return key.equals(keys.get(goal)) ? cards.get(goal) : null; }
    public void put(Goal goal, List<?> key, T card) { keys.put(goal, key); cards.put(goal, card); }
    public void invalidate(Goal goal) { keys.remove(goal); cards.remove(goal); }
    public void clear() { keys.clear(); cards.clear(); }
    public void retain(List<Goal> goals) {
        java.util.Set<Goal> retained = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        retained.addAll(goals);
        keys.keySet().retainAll(retained); cards.keySet().retainAll(retained);
    }
}
