package com.grindstone;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class GrindstonePluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(GrindstonePlugin.class);
        RuneLite.main(args);
    }
}
