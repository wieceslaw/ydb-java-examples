package tech.ydb.example.coordination.recipes.lib.util;

import java.util.function.Consumer;

public interface ListenableAdder<T> {
    void addListener(Consumer<T> listener);
    void removeListener(Consumer<T> listener);
}
