package com.grinder.ui;

import com.grinder.GrinderPlugin;
import lombok.NonNull;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.function.UnaryOperator;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum Icon {
    MAIN("/grinder.png"),
    QUEST("/quests.png"),
    COLLECTION_LOG("/collection_log.png"),
    ACHIEVEMENT_DIARY("/achievement_diary.png"),
    COMBAT_ACHIEVEMENT("/combat_achievement.png");


    private final String file;

    Icon(String file) {
        this.file = file;
    }

    public BufferedImage getImage() {
        return ImageUtil.loadImageResource(GrinderPlugin.class, file);
    }

    public ImageIcon getIcon() {
        return getIcon(UnaryOperator.identity());
    }

    public ImageIcon getIcon(@NonNull UnaryOperator<BufferedImage> func) {
        BufferedImage img = func.apply(getImage());
        return new ImageIcon(img);
    }

}
