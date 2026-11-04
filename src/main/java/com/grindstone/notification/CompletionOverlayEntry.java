package com.grindstone.notification;

import java.awt.image.BufferedImage;
import lombok.Value;
import net.runelite.client.util.Text;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@Value
public class CompletionOverlayEntry {
    String title;
    String detail;
    BufferedImage icon;

    public CompletionOverlayEntry(String title, String detail, BufferedImage icon) {
        this.title = Text.removeTags(title).replaceAll("[\\r\\n\\t]", " ").trim();
        this.detail = Text.removeTags(detail).replaceAll("[\\r\\n\\t]", " ").trim();
        this.icon = icon;
    }
}
