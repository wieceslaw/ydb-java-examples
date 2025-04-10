package tech.ydb.example.coordination.recipes.lib.util;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface Listenable<T> {
    void addListener(Consumer<T> listener);

    /**
     * Listener call will be processed in executor
     */
    void addListener(Consumer<T> listener, ExecutorService executor);

    void removeListener(Consumer<T> listener);

    void clearListeners();
}
