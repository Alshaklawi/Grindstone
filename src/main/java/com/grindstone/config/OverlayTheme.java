package com.grindstone.config;

import lombok.RequiredArgsConstructor;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
@RequiredArgsConstructor
public enum OverlayTheme {
    CLASSIC("Classic"),
    SLATE("Slate"),
    GUILD("Guild"),
    RUNELITE("RuneLite");

    private final String title;

    @Override
    public String toString() {
        return title;
    }
}
