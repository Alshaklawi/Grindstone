package com.grindstone.content.diary;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum DiaryTier {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    ELITE("Elite");
    private final String name;
    DiaryTier(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return name;
    }
}
