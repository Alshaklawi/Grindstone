package com.grindstone.ui;

import com.grindstone.GrindstonePlugin;
import java.awt.image.BufferedImage;
import lombok.Getter;
import net.runelite.client.util.ImageUtil;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum Icon {
    MAIN("/grindstone.png"),
    QUEST("/quests_icon.png"),
    COLLECTION_LOG("/collection_log_icon.png"),
    BOSSING("/bossing.png"),
    ACHIEVEMENT_DIARY("/achievement_diary_icon.png"),
    COMBAT_ACHIEVEMENT("/combat_achievement_icon.png"),
    SKILLING("/skilling.png");

    @Getter
    private final BufferedImage image;

    Icon(String file) {
        this.image = ImageUtil.loadImageResource(GrindstonePlugin.class, file);
    }
}
