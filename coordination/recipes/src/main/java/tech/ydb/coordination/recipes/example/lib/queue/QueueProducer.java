package tech.ydb.coordination.recipes.example.lib.queue;

import java.time.Duration;

public interface QueueProducer {
    /**
    Blocks until node is free to take data
    */
    void offer(byte[] data) throws Exception;

    /**
     Blocks until node is free to take data
     false - if timeout
     */
    boolean offer(byte[] data, Duration timeout) throws Exception;
}
