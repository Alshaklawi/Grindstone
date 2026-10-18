package com.grinder;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GrinderPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(GrinderPlugin.class);
        RuneLite.main(args);
    }
}