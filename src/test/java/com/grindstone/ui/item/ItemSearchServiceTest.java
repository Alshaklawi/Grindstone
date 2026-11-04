package com.grindstone.ui.item;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public class ItemSearchServiceTest {
    @Test
    public void overlappingSearchesSeeEveryIndexedItemExactlyOnce() throws Exception {
        List<BooleanSupplier> tasks = new ArrayList<>();
        ClientThread thread = new ClientThread() {
            @Override
            public void invokeLater(BooleanSupplier task) {
                tasks.add(task);
            }
        };
        Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class}, (proxy, method, arguments) -> {
            switch (method.getName()) {
                case "getGameState":
                    return GameState.LOGGED_IN;
                case "getItemCount":
                    return 1024;
                case "getItemDefinition":
                    int id = (Integer) arguments[0];
                    return Proxy.newProxyInstance(ItemComposition.class.getClassLoader(), new Class<?>[]{ItemComposition.class}, (item, accessor, args) -> {
                        switch (accessor.getName()) {
                            case "getNote":
                            case "getPlaceholderTemplateId":
                                return -1;
                            case "getMembersName":
                                return id == 200 || id == 400 ? "Dragon sword " + id : "Other item";
                            default:
                                throw new AssertionError(accessor.getName());
                        }
                    });
                default:
                    throw new AssertionError(method.getName());
            }
        });
        ItemSearchService service = new ItemSearchService(client, thread, id -> null);
        List<ItemSearchService.Response> first = new ArrayList<>();
        List<ItemSearchService.Response> second = new ArrayList<>();
        service.search("dragon sword", first::add);
        service.search("sword dragon", second::add);
        for (int round = 0; round < 2048 && !tasks.isEmpty(); round++) {
            tasks.removeIf(BooleanSupplier::getAsBoolean);
        }
        assertTrue(tasks.isEmpty());
        SwingUtilities.invokeAndWait(() -> { });
        for (List<ItemSearchService.Response> responses : List.of(first, second)) {
            ItemSearchService.Response result = responses.get(responses.size() - 1);
            assertTrue(result.complete);
            assertEquals(2, result.results.size());
            assertEquals(200, result.results.get(0).id);
            assertEquals(400, result.results.get(1).id);
        }
        List<ItemSearchService.Response> cached = new ArrayList<>();
        service.search("DRAGON sword", cached::add);
        for (int round = 0; round < 2048 && !tasks.isEmpty(); round++) {
            tasks.removeIf(BooleanSupplier::getAsBoolean);
        }
        SwingUtilities.invokeAndWait(() -> { });
        assertTrue(tasks.isEmpty());
        assertEquals(2, cached.get(cached.size() - 1).results.size());
        List<ItemSearchService.Response> cancelled = new ArrayList<>();
        Runnable cancel = service.search("dragon", cancelled::add);
        cancel.run();
        assertTrue(tasks.get(0).getAsBoolean());
        SwingUtilities.invokeAndWait(() -> { });
        assertTrue(cancelled.isEmpty());
    }
}
