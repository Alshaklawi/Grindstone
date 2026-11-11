package com.grindstone.overlay;

import com.grindstone.repository.RepeatSchedule;
import java.awt.image.BufferedImage;
import java.util.List;
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
    BufferedImage icon;
    String unit;
    List<Source> sources;
    int remainingSources;

    public ProgressOverlayEntry(String title, RepeatSchedule schedule, long current, long target, boolean complete, boolean ready, boolean pinned, BufferedImage icon) {
        this(title, schedule, current, target, complete, ready, pinned, icon, "", List.of(), 0);
    }

    public ProgressOverlayEntry(String title, RepeatSchedule schedule, long current, long target, boolean complete, boolean ready, boolean pinned, BufferedImage icon, String unit, List<Source> sources, int remainingSources) {
        this.title = cleanTitle(title);
        this.schedule = Objects.requireNonNull(schedule);
        this.current = Math.max(0, current);
        this.target = Math.max(0, target);
        this.complete = complete;
        this.ready = ready;
        this.pinned = pinned;
        this.icon = icon;
        this.unit = unit;
        this.sources = List.copyOf(sources);
        this.remainingSources = remainingSources;
    }

    private static String cleanTitle(String title) {
        return Text.removeTags(Objects.requireNonNull(title)).replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 11/11/2026
     */
    @Value
    public static class Source {
        String title;
        BufferedImage icon;
        long current;
        long target;
        boolean ready;

        public Source(String title, BufferedImage icon, long current, long target, boolean ready) {
            this.title = cleanTitle(title);
            this.icon = icon;
            this.current = current;
            this.target = target;
            this.ready = ready;
        }
    }
}
