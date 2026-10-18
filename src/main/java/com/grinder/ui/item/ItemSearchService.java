package com.grinder.ui.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
@Slf4j
@Singleton
public final class ItemSearchService implements ItemLookup {
    private static final int BATCH_SIZE = 128;
    private static final int RESULT_LIMIT = 50;
    private final Client client;
    private final ClientThread clientThread;
    private final ItemManager itemManager;
    private final List<Result> index = new ArrayList<>();
    private int nextItemId;

    @Inject
    public ItemSearchService(Client client, ClientThread clientThread, ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.itemManager = itemManager;
    }

    private static void deliver(AtomicBoolean cancelled, Consumer<Response> callback, Response response) {
        SwingUtilities.invokeLater(() -> {
            if (!cancelled.get()) {
                callback.accept(response);
            }
        });
    }

    static boolean isIdQuery(String query) {
        return query.matches("#?\\d+");
    }

    static int parseId(String query) {
        try {
            return Integer.parseInt(query.startsWith("#") ? query.substring(1) : query);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    static boolean matchesName(String name, String query) {
        String normalized = name.toLowerCase(Locale.ROOT);
        for (String word : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!normalized.contains(word)) {
                return false;
            }
        }
        return true;
    }

    static int rank(String name, String query) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.equals(query) ? 0 : normalized.startsWith(query) ? 1 : 2;
    }

    @Override
    public Runnable search(String text, Consumer<Response> callback) {
        AtomicBoolean cancelled = new AtomicBoolean();
        String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        List<Result> matches = new ArrayList<>();
        AtomicBoolean initialized = new AtomicBoolean();
        int[] batches = {0};
        clientThread.invokeLater(() -> {
            if (cancelled.get()) {
                return true;
            }
            if (client.getGameState() != GameState.LOGGED_IN) {
                deliver(cancelled, callback, new Response(new ArrayList<>(), "Log in to search client items.", true));
                return true;
            }
            if (isIdQuery(query)) {
                int id = parseId(query);
                if (id <= 0 || id >= client.getItemCount()) {
                    deliver(cancelled, callback, new Response(new ArrayList<>(), "No item exists for that ID.", true));
                    return true;
                }
                Result result = readItem(id, true);
                List<Result> found = new ArrayList<>();
                if (result != null) {
                    found.add(withImage(result));
                }
                deliver(cancelled, callback, new Response(found,
                        result == null ? "That ID has no usable item definition." : "Item ID resolved. Confirm its name and icon below.", true));
                return true;
            }
            if (query.length() < 2) {
                deliver(cancelled, callback, new Response(new ArrayList<>(), "Enter at least 2 letters, or an item ID.", true));
                return true;
            }
            if (!initialized.getAndSet(true)) {
                for (Result result : index) {
                    if (matchesName(result.name, query)) {
                        matches.add(result);
                    }
                }
            }
            int end = Math.min(client.getItemCount(), nextItemId + BATCH_SIZE);
            int start = nextItemId;
            long deadline = System.nanoTime() + 3_000_000L;
            while (nextItemId < end && (nextItemId == start || System.nanoTime() < deadline)) {
                Result result = readItem(nextItemId++, false);
                if (result != null) {
                    index.add(result);
                    if (matchesName(result.name, query)) {
                        matches.add(result);
                    }
                }
            }
            boolean complete = nextItemId >= client.getItemCount();
            if (complete || batches[0]++ % 8 == 0) {
                List<Result> ordered = new ArrayList<>(matches);
                ordered.sort(Comparator.comparingInt((Result result) -> rank(result.name, query))
                        .thenComparing(result -> result.name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(result -> result.id));
                List<Result> visible = new ArrayList<>();
                for (int i = 0; i < Math.min(RESULT_LIMIT, ordered.size()); i++) {
                    visible.add(withImage(ordered.get(i)));
                }
                String message = !complete ? "Searching client items... "
                        + (client.getItemCount() == 0 ? 0 : nextItemId * 100 / client.getItemCount()) + "%"
                        : ordered.isEmpty() ? "No matches. Try a shorter name or a numeric ID."
                        : ordered.size() > RESULT_LIMIT ? "First 50 matches. Refine the name to narrow the list."
                        : ordered.size() + " matching items. Select one to confirm.";
                deliver(cancelled, callback, new Response(visible, message, complete));
            }
            return complete;
        });
        return () -> cancelled.set(true);
    }

    private Result readItem(int id, boolean explicitId) {
        if (id <= 0) {
            return null;
        }
        try {
            ItemComposition item = client.getItemDefinition(id);
            if (item == null) {
                return null;
            }
            String name = item.getMembersName();
            if (name == null || name.trim().isEmpty()) {
                name = item.getName();
            }
            if (name == null || name.trim().isEmpty() || "null".equalsIgnoreCase(name)) {
                return null;
            }
            if (!explicitId && (item.getNote() != -1 || item.getPlaceholderTemplateId() != -1)) {
                return null;
            }
            return new Result(id, name, null);
        } catch (RuntimeException exception) {
            log.debug("Unable to read item definition {}", id, exception);
            return null;
        }
    }

    private Result withImage(Result result) {
        return new Result(result.id, result.name, itemManager.getImage(result.id));
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/18/2026
     */
    public static final class Result {
        public final int id;
        public final String name;
        public final AsyncBufferedImage image;

        public Result(int id, String name, AsyncBufferedImage image) {
            this.id = id;
            this.name = name;
            this.image = image;
        }
    }

    /**
     * @author Ayman Yassen
     * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
     * @since 10/18/2026
     */
    public static final class Response {
        public final List<Result> results;
        public final String message;
        public final boolean complete;

        public Response(List<Result> results, String message, boolean complete) {
            this.results = java.util.Collections.unmodifiableList(new ArrayList<>(results));
            this.message = message;
            this.complete = complete;
        }
    }
}
