package com.grindstone.notification;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public final class CompletionTransitions {
    private final Map<String, String> cycles = new HashMap<>();
    private final Map<String, Boolean> reached = new HashMap<>();

    public boolean observe(String id, String cycle, boolean complete) {
        String previous = cycles.put(id, cycle);
        if (!cycle.equals(previous)) {
            reached.put(id, complete);
            return false;
        }
        boolean alreadyReached = Boolean.TRUE.equals(reached.get(id));
        if (complete) {
            reached.put(id, true);
        }
        return complete && !alreadyReached;
    }

    public void retain(Set<String> ids) {
        cycles.keySet().retainAll(ids);
        reached.keySet().retainAll(ids);
    }

    public void clear() {
        cycles.clear();
        reached.clear();
    }
}
