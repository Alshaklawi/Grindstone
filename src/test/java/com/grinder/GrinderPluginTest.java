package com.grinder;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.ui.laf.RuneLiteLAF;
import javax.swing.SwingUtilities;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GrinderPluginTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (!RuneLiteLAF.setup()) {
                throw new IllegalStateException("Unable to install the RuneLite look and feel");
            }
        });
        ExternalPluginManager.loadBuiltin(GrinderPlugin.class);
        RuneLite.main(args);
    }
}
