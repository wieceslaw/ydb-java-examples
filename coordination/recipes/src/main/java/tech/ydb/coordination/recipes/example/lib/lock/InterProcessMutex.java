package tech.ydb.coordination.recipes.example.lib.lock;


import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@ThreadSafe
public class InterProcessMutex implements InterProcessLock {

    private static final Logger logger = LoggerFactory.getLogger(InterProcessMutex.class);
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(100);

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
        logger.debug("Trying to acquire without timeout");
        acquireLock(null);
    }

    public boolean acquire(Duration duration) throws Exception {
        logger.debug("Trying to acquire with deadline: {}", duration);
        Instant deadline = Instant.now().plus(duration);
        return acquireLock(deadline);
    }

    public void release() {
        logger.debug("Trying to release");
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
            logger.debug("Already acquired lock: {}, re-entering", semaphoreName);
            lockData.lockCount.incrementAndGet();
            return true;
        }

        SemaphoreLease lease = internalLock(deadline);
        if (lease != null) {
            LockData newLockData = new LockData(currentThread, lease);
            threadData.put(currentThread, newLockData);
            return true;
        }
        logger.debug("Unable to acquire lock");
        return false;
    }

    private SemaphoreLease internalLock(@Nullable Instant deadline) {
        int retryCount = 0;
        while (session.getState().isActive() && (deadline == null || Instant.now().isBefore(deadline))) {
            retryCount++;

            Duration timeout;
            if (deadline == null) {
                timeout = DEFAULT_TIMEOUT;
            } else {
                timeout = Duration.between(Instant.now(), deadline); // TODO: use external Clock instead of Instant.now()?
            }
            Result<SemaphoreLease> leaseResult = session.acquireEphemeralSemaphore(
                            semaphoreName, true, data, timeout // TODO: change Session API to use deadlines
                    )
                    .join();
            Status status = leaseResult.getStatus();
            logger.debug("Lease result status: {}", status);

            if (status.isSuccess()) {
                logger.debug("Successfully acquired the lock");
                return leaseResult.getValue();
            }

            if (status.getCode() == StatusCode.TIMEOUT) {
                logger.debug("Trying to acquire again, retries: {}", retryCount);
                continue;
            }

            if (!status.getCode().isRetryable(true)) {
                status.expectSuccess("Unable to retry acquiring semaphore");
                return null;
            }
        }
        return null;
    }

}