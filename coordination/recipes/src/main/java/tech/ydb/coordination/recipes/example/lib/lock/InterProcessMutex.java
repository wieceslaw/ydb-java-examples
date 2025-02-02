package tech.ydb.coordination.recipes.example.lib.lock;


import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.Nullable;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * May throw an exception if the current thread does not own the lock
 */
// TODO: add logs everywhere
@ThreadSafe
public class InterProcessMutex implements InterProcessLock {

    private final CoordinationSession session;
    private final String semaphoreName;
    private final byte[] data;

    private final ConcurrentMap<Thread, LockData> threadData = Maps.newConcurrentMap();

    private static class LockData {
        final SemaphoreLease lease;
        final Thread owningThread;
        final AtomicInteger lockCount = new AtomicInteger(1);

        private LockData(Thread owningThread, SemaphoreLease lease) {
            this.lease = lease;
            this.owningThread = owningThread;
        }
    }

    public InterProcessMutex(
            CoordinationSession session,
            byte[] data,
            String lockName
    ) {
        this.session = session;
        this.data = data;
        this.semaphoreName = lockName;
    }

    public void acquire() throws Exception {
        acquireLock(null);
    }

    public boolean acquire(Duration duration) throws Exception {
        Instant deadline = Instant.now().plus(duration);
        return acquireLock(deadline);
    }

    public void release() {
        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        if (lockData == null) {
            throw new IllegalStateException("You do not own the lock: " + semaphoreName);
        }

        int newLockCount = lockData.lockCount.decrementAndGet();
        if (newLockCount > 0) {
            return;
        }

        if (newLockCount < 0) {
            throw new IllegalStateException("Lock count has gone negative for lock: " + semaphoreName);
        }

        try {
            lockData.lease.release().join();
        } finally {
            threadData.remove(currentThread);
        }
    }

    @Override
    public boolean isAcquiredInThisProcess() {
        return !threadData.isEmpty();
    }

    /**
     * Returns true if the mutex is acquired by the calling thread
     *
     * @return true/false
     */
    public boolean isOwnedByCurrentThread() {
        LockData lockData = threadData.get(Thread.currentThread());
        return (lockData != null) && (lockData.lockCount.get() > 0);
    }

    private boolean acquireLock(@Nullable Instant deadline) throws Exception {
        /*
           Note on concurrency: a given lockData instance
           can be only acted on by a single thread so locking isn't necessary
        */

        Thread currentThread = Thread.currentThread();

        LockData lockData = threadData.get(currentThread);
        if (lockData != null) {
            // re-entering
            lockData.lockCount.incrementAndGet();
            return true;
        }

        SemaphoreLease lease = internalLock(deadline);
        if (lease != null) {
            LockData newLockData = new LockData(currentThread, lease);
            threadData.put(currentThread, newLockData);
            return true;
        }
        return false;
    }

    private SemaphoreLease internalLock(@Nullable Instant deadline) {
        System.out.println("deadline: " + deadline);
        while (session.getState().isActive() && (deadline == null || Instant.now().isBefore(deadline))) {
            Result<SemaphoreLease> leaseResult = session.acquireEphemeralSemaphore(
                            semaphoreName, true, data, Duration.ofSeconds(5) // TODO: change to deadline
                    )
                    .join();
            Status status = leaseResult.getStatus();
            System.out.println(status);
            if (status.isSuccess()) {
                System.out.println("success");
                return leaseResult.getValue();
            }

            if (status.getCode() == StatusCode.TIMEOUT) {
                System.out.println("trying to acquire lock again...");
                continue;
            }

            if (!status.getCode().isRetryable(true)) {
                status.expectSuccess("Unable to retry acquiring semaphore");
                return null;
            }
        }
        if (!session.getState().isActive()) {
            throw new IllegalStateException("Session is not active");
        }
        return null;
    }

}