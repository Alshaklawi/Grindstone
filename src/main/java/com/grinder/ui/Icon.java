package com.grinder.ui;

import com.grinder.GrinderPlugin;
import lombok.NonNull;
import net.runelite.client.util.ImageUtil;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.util.function.UnaryOperator;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum Icon {
    MAIN("/grinder.png"),
    QUEST("/quests_icon.png"),
    COLLECTION_LOG("/collection_log_icon.png"),
    BOSSING("/bossing.png"),
    ACHIEVEMENT_DIARY("/achievement_diary_icon.png"),
    COMBAT_ACHIEVEMENT("/combat_achievement_icon.png"),
    SKILLING("/skilling.png");


    private final String file;
    private final BufferedImage image;

    Icon(String file) {
        this.file = file;
        this.image = ImageUtil.loadImageResource(GrinderPlugin.class, file);
    }

    public BufferedImage getImage() {
        return image;
    }

    public ImageIcon getIcon() {
        return getIcon(UnaryOperator.identity());
    }

    public ImageIcon getIcon(@NonNull UnaryOperator<BufferedImage> func) {
        BufferedImage img = func.apply(getImage());
        return new ImageIcon(img);
    }
}
