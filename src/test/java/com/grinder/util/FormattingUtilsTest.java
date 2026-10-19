package com.grinder.util;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class FormattingUtilsTest {
    @Test
    public void htmlTextIsEscapedAndNullSafe() {
        assertEquals("&lt;goal&gt; &amp; progress", HtmlUtils.escape("<goal> & progress"));
        assertEquals("", HtmlUtils.escape(null));
        assertEquals("Dragon pickaxe", HtmlUtils.escape("Dragon pickaxe"));
    }
    @Test
    public void sharedNumberFormattingPreservesExistingDisplayConventions() {
        assertEquals("999", NumberFormats.experience(999));
        assertEquals("1.0K", NumberFormats.experience(1000));
        assertEquals("13.0M", NumberFormats.experience(13_034_431));
        assertEquals("1,000", NumberFormats.target(1000));
        assertEquals("1.5B", NumberFormats.target(1_500_000_000L));
    }
}
