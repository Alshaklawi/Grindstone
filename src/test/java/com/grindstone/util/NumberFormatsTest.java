package com.grindstone.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/11/2026
 */
public class NumberFormatsTest {
    @Test
    public void calculatesExactWholePercentagesWithoutRoundingUpOrOverflow() {
        assertEquals(58, NumberFormats.calculatePercentage(58, 100));
        assertEquals(58, NumberFormats.calculatePercentage(29, 50));
        assertEquals(57, NumberFormats.calculatePercentage(5799, 10000));
        assertEquals(99, NumberFormats.calculatePercentage(Long.MAX_VALUE - 1, Long.MAX_VALUE));
        assertEquals(100, NumberFormats.calculatePercentage(Long.MAX_VALUE, Long.MAX_VALUE));
    }
}
