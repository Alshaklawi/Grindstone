package com.grindstone.ui;

import com.grindstone.GrindstonePlugin;
import java.awt.image.BufferedImage;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.ImageUtil;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum Icon {
    MAIN("grindstone.png"),
    MINIGAMES("minigames_icon.png"),
    QUEST("quests_icon.png"),
    COLLECTION_LOG("collection_log_icon.png"),
    BOSSING("bossing.png"),
    RAIDS("raids_icon.png"),
    SLAYER("slayer_icon.png"),
    CLUES("clues_icon.png"),
    ITEMS("items_icon.png"),
    ACHIEVEMENT_DIARY("achievement_diary_icon.png"),
    COMBAT_ACHIEVEMENT("combat_achievement_icon.png"),
    SKILLING("skilling.png");

    private static final SkillIconManager SKILL_ICONS = new SkillIconManager();

    public static BufferedImage getSkillImage(Skill skill) {
        return SKILL_ICONS.getSkillImage(skill, true);
    }

    private final BufferedImage image;

    public BufferedImage getImage() {
        return this == SLAYER ? getSkillImage(Skill.SLAYER) : image;
    }

    Icon(String file) {
        this.image = ImageUtil.loadImageResource(GrindstonePlugin.class, file);
    }
}
