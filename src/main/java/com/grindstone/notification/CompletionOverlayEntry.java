package com.grindstone.notification;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.UUID;
import lombok.Value;
import net.runelite.client.util.Text;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@Value
public class CompletionOverlayEntry {
    String id;
    String title;
    String detail;
    BufferedImage icon;

    public CompletionOverlayEntry(String title, String detail, BufferedImage icon) {
        this(UUID.randomUUID().toString(), title, detail, icon);
    }

    public CompletionOverlayEntry(String id, String title, String detail, BufferedImage icon) {
        this.id = Objects.requireNonNull(id);
        this.title = Text.removeTags(title).replaceAll("[\\r\\n\\t]", " ").trim();
        this.detail = Text.removeTags(detail).replaceAll("[\\r\\n\\t]", " ").trim();
        this.icon = icon;
    }
}
