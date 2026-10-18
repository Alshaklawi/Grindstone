package com.grinder.ui.item;

import java.util.function.Consumer;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@FunctionalInterface
public interface ItemLookup {
    Runnable search(String query, Consumer<ItemSearchService.Response> callback);
}
