package com.grindstone.ui.util;

import com.grindstone.content.milestone.Milestone;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class MilestoneCardCache<T> {
    private final Map<Milestone, List<?>> keys = new IdentityHashMap<>();
    private final Map<Milestone, T> cards = new IdentityHashMap<>();
    public synchronized T get(Milestone goal, List<?> key) {
        return key.equals(keys.get(goal)) ? cards.get(goal) : null;
    }
    public synchronized void put(Milestone goal, List<?> key, T card) {
        keys.put(goal, key);
        cards.put(goal, card);
    }
    public synchronized void invalidate(Milestone goal) {
        keys.remove(goal);
        cards.remove(goal);
    }
    public synchronized void clear() {
        keys.clear();
        cards.clear();
    }
    public synchronized void retain(List<Milestone> goals) {
        Set<Milestone> retained = Collections.newSetFromMap(new IdentityHashMap<>());
        retained.addAll(goals);
        keys.keySet().retainAll(retained);
        cards.keySet().retainAll(retained);
    }
}
