package com.grindstone.overlay;

import com.grindstone.repository.RepeatSchedule;
import java.util.Objects;
import lombok.Value;
import net.runelite.client.util.Text;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
@Value
public class ProgressOverlayEntry {
    String title;
    RepeatSchedule schedule;
    long current;
    long target;
    boolean complete;
    boolean ready;
    boolean pinned;

    public ProgressOverlayEntry(String title, RepeatSchedule schedule, long current, long target, boolean complete, boolean ready, boolean pinned) {
        this.title = Text.removeTags(Objects.requireNonNull(title)).replaceAll("[\\r\\n\\t]", " ").trim();
        this.schedule = Objects.requireNonNull(schedule);
        this.current = Math.max(0, current);
        this.target = Math.max(0, target);
        this.complete = complete;
        this.ready = ready;
        this.pinned = pinned;
    }
}
