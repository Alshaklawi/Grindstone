package com.grindstone.content.tracker;

import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/19/2026
 */
public class BossGoalShortcutTest {
    @Test
    public void matchesOnlySupportedBossesWithoutInventingTrackers() {
        assertSame(TrackerEntry.COWBOSS, TrackerEntry.forBossName("Brutus"));
        assertSame(TrackerEntry.COWBOSS_HARDMODE, TrackerEntry.forBossName("Demonic Brutus"));
        assertSame(TrackerEntry.LEVIATHAN, TrackerEntry.forBossName("The Leviathan"));
        assertSame(TrackerEntry.ARMADYL, TrackerEntry.forBossName("Kree'arra"));
        assertSame(TrackerEntry.CALVARION, TrackerEntry.forBossName("Calvar'ion"));
        assertNull(TrackerEntry.forBossName("Cow"));
        assertNull(TrackerEntry.forBossName("Gemstone crab"));
        assertNull(TrackerEntry.forBossName("Unknown boss"));
        assertNull(TrackerEntry.forBossName(null));
    }
}
