package tech.ydb.coordination.recipes.example.lib.queue;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
import tech.ydb.coordination.recipes.example.lib.locks.LockInternals;
import tech.ydb.coordination.recipes.example.lib.watch.SemaphoreWatcher;

// TODO: Interrupts
public class SimpleDistributedQueue implements QueueConsumer, QueueProducer, Closeable {
    private final LockInternals lockInternals;
    private final SemaphoreWatcher semaphoreWatcher;

    public SimpleDistributedQueue(LockInternals lockInternals) {
        this.lockInternals = lockInternals;
        this.semaphoreWatcher = new SemaphoreWatcher(
                lockInternals.getCoordinationSession(),
                lockInternals.getSemaphoreName()
        );
    }

    public void start() {
        lockInternals.start();
        semaphoreWatcher.start();
    }

    /*
        T value = null;
        while (value == null) { -- spin loop is ineffective on high contention
            semaphore.acquire(exclusively);
            value = semaphore.read(); -- may be null
            semaphore.update(null);
            semaphore.release();
        }
        return value;
    */
    @Override
    public byte[] take() throws Exception {
        // TODO: rewrite
        ByteBuffer byteBuffer = waitUntil(() -> getData(null), Objects::nonNull).get();
        return byteBuffer.array();
    }

    @Override
    public byte @Nullable [] take(Duration timeout) throws Exception {
        ByteBuffer byteBuffer = waitUntil(() -> getData(timeout), Objects::nonNull)
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException) {
                        return null;
                    }
                    throw new RuntimeException(ex);
                })
                .get();
        return byteBuffer.array();
    }

    private CompletableFuture<@Nullable ByteBuffer> getData(@Nullable Duration timeout) {
        return semaphoreWatcher.waitUntil(watchData -> watchData.getData() != null).thenApply(nodeData -> {
            try {
                boolean acquired = lockInternals.tryAcquire(
                        timeout,
                        true,
                        null
                );
                if (!acquired) {
                    throw new TimeoutException();
                }

                byte[] dataSync = lockInternals.getDataSync();
                if (dataSync != null) {
                    lockInternals.update(null);
                }
                return dataSync == null ? null : ByteBuffer.wrap(dataSync);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lockInternals.release();
            }
        });
    }

    /*
        boolean isWritten = false;
        while (!isWritten) { -- spin loop is ineffective on high contention
             semaphore.acquire(exclusively);
             if (isEmpty) {
                 semaphore.update(data);
                 isWritten = true;
             }
             semaphore.release();
        }
        return isWritten;
    */
    @Override
    public void offer(byte[] data) throws Exception {
        waitUntil(() -> tryPut(data, null), Function.identity())
                .get();
    }

    @Override
    public boolean offer(byte[] data, @Nullable Duration timeout) throws Exception {
        return waitUntil(() -> tryPut(data, timeout), Function.identity())
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException) {
                        return false;
                    }
                    throw new RuntimeException(ex);
                })
                .get();
    }

    private CompletableFuture<Boolean> tryPut(byte[] data, @Nullable Duration timeout) {
        return semaphoreWatcher.waitUntil(watchData -> watchData.getData() == null).thenApply(nodeData -> {
            try {
                boolean acquired = lockInternals.tryAcquire(
                        timeout,
                        true,
                        null
                );
                if (!acquired) {
                    throw new TimeoutException();
                }

                byte[] dataSync = lockInternals.getDataSync();
                if (dataSync == null) {
                    lockInternals.update(data);
                    return true;
                }
                return false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lockInternals.release();
            }
        });
    }

    private <T> CompletableFuture<T> waitUntil(
            Supplier<CompletableFuture<T>> supplier,
            Function<T, Boolean> condition
    ) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        BiConsumer<T, Throwable> checkCondition = new BiConsumer<T, Throwable>() {
            @Override
            public void accept(T value, Throwable ex) {
                if (isCompleted.get()) {
                    return;
                }

                if (ex != null) {
                    isCompleted.set(true);
                    resultFuture.completeExceptionally(ex);
                    return;
                }

                if (condition.apply(value)) {
                    isCompleted.set(true);
                    resultFuture.complete(value);
                } else {
                    supplier.get().whenCompleteAsync(this);
                }
            }
        };
        supplier.get().whenCompleteAsync(checkCondition);
        return resultFuture;
    }

    @Override
    public void close() {
        lockInternals.close();
        semaphoreWatcher.close();
    }
}
