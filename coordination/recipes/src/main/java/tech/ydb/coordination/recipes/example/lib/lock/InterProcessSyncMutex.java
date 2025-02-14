package tech.ydb.coordination.recipes.example.lib.lock;


import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.annotation.concurrent.ThreadSafe;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * May throw an exception if the current thread does not own the lock
 */
@ThreadSafe
public class InterProcessSyncMutex implements InterProcessLock {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(1);
    private static final Logger logger = LoggerFactory.getLogger(InterProcessSyncMutex.class);

    private final CoordinationSession session;
    private final String semaphoreName;
    private final byte[] data;

    private volatile SemaphoreLease processLease = null;

    public InterProcessSyncMutex(
            CoordinationClient client,
            String coordinationNodePath,
            byte[] data,
            String lockName
    ) {
        ExecutorService sessionExecutorService = Executors.newSingleThreadExecutor();
        this.session = client.createSession(
                coordinationNodePath,
                CoordinationSessionSettings.newBuilder()
                        .withExecutor(sessionExecutorService)
                .build()
        );
        this.data = data;
        this.semaphoreName = lockName;

        this.session.connect().join().expectSuccess("Unable to create session");
        session.addStateListener(new Consumer<CoordinationSession.State>() {
            @Override
            public void accept(CoordinationSession.State state) {
                switch (state) {
                    case RECONNECTING: {
                        // TODO: Lock is not acquired nor released?
                        break;
                    }
                    case RECONNECTED: {
                        Result<SemaphoreDescription> result = session.describeSemaphore(
                                semaphoreName,
                                DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS
                        ).join();
                        if (!result.isSuccess()) {
                            logger.error("Unable to describe semaphore {}", semaphoreName);
                            return;
                        }
                        SemaphoreDescription semaphoreDescription = result.getValue();
                        SemaphoreDescription.Session owner = semaphoreDescription.getOwnersList().getFirst();
                        if (owner.getId() != session.getId()) {
                            logger.warn(
                                    "Session with id: {} lost lease after reconnection on semaphore: {}",
                                    owner.getId(),
                                    semaphoreName
                            );
                            release();
                        }
                        break;
                    }
                    case CLOSED: {
                        logger.debug("Session is CLOSED, releasing lock");
                        release();
                        break;
                    }
                    case LOST: {
                        logger.debug("Session is LOST, releasing lock");
                        release();
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void acquire() throws Exception {
        logger.debug("Trying to acquire without timeout");
        acquireLock(null);
    }

    @Override
    public boolean acquire(Duration duration) throws Exception {
        logger.debug("Trying to acquire with deadline: {}", duration);
        Instant deadline = Instant.now().plus(duration);
        return acquireLock(deadline);
    }

    @Override
    public void release() {
        logger.debug("Trying to release");
        if (processLease == null) {
            logger.debug("Already released");
            // TODO: throw Exception/return false if already released?
            return;
        }

        if (processLease != null) {
            synchronized (this) {
                if (processLease != null) {
                    processLease.release().join();
                    processLease = null;
                    logger.debug("Released lock");
                }
            }
        }

        logger.debug("Already released");
        // TODO: throw Exception/return false if already released?
    }

    @Override
    public boolean isAcquiredInThisProcess() {
        return processLease != null;
    }

    private boolean acquireLock(@Nullable Instant deadline) throws Exception {
        if (processLease == null) {
            logger.debug("Already acquired lock: {}", semaphoreName);
            return false;
        }

        synchronized (this) {
            if (processLease == null) {
                SemaphoreLease lease = internalLock(deadline);
                if (lease != null) {
                    this.processLease = lease;
                    logger.debug("Successfully acquired lock: {}", semaphoreName);
                    return true;
                }
            }
        }
        logger.debug("Already acquired lock: {}", semaphoreName);
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
