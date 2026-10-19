package com.grindstone.ui;

import com.grindstone.GrindstonePlugin;
import net.runelite.client.util.ImageUtil;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public class IconTest {
    @Test
    public void allPackagedIconsLoad() {
        for (Icon icon : Icon.values()) {
            assertNotNull(icon.name(), icon.getImage());
            assertTrue(icon.name(), icon.getImage().getWidth() > 0 && icon.getImage().getHeight() > 0);
        }
        assertNotNull(ImageUtil.loadImageResource(GrindstonePlugin.class, "grindstone.png"));
    }
}
