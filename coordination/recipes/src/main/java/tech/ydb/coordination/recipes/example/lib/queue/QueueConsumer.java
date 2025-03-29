package tech.ydb.coordination.recipes.example.lib.queue;

import java.time.Duration;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface QueueConsumer {
    /**
     * Blocks until data is available
     * null if timeout
     */
    byte[] take() throws Exception;

    /**
     * Blocks until data is available
     * null if timeout
     */
    byte @Nullable [] take(Duration timeout) throws Exception;
}
