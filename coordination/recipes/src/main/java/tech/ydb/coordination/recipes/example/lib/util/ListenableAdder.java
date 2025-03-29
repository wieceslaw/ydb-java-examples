package tech.ydb.coordination.recipes.example.lib.util;

import java.util.function.Consumer;

public interface ListenableAdder<T> {
    void addListener(Consumer<T> listener);
    void removeListener(Consumer<T> listener);
}
