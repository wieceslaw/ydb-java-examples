package tech.ydb.example.coordination.recipes.lib.locks;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.annotation.concurrent.ThreadSafe;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.example.coordination.recipes.lib.util.*;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

@ThreadSafe
public class LockInternals implements ListenableProvider<CoordinationSession.State>, Closeable {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Logger logger = LoggerFactory.getLogger(LockInternals.class);

    private final String coordinationNodePath;
    private final String semaphoreName;
    private final CoordinationSession session;
    private final ListenerWrapper<CoordinationSession.State> sessionListenerWrapper;

    private CompletableFuture<Status> sessionConnectionTask = null;
    private volatile LeaseData leaseData = null; // TODO: needs to be volatile?

    public static class LeaseData {
        private final SemaphoreLease processLease;
        private final boolean isExclusive;
        private final long leaseSessionId;

        public LeaseData(SemaphoreLease processLease, boolean isExclusive, long leaseSessionId) {
            this.processLease = processLease;
            this.isExclusive = isExclusive;
            this.leaseSessionId = leaseSessionId;
        }

        public boolean isExclusive() {
            return isExclusive;
        }

        public SemaphoreLease getProcessLease() {
            return processLease;
        }

        public long getLeaseSessionId() {
            return leaseSessionId;
        }

        @Override
        public String toString() {
            return "LeaseData{" +
                    "processLease=" + processLease +
                    ", isExclusive=" + isExclusive +
                    ", leaseSessionId=" + leaseSessionId +
                    '}';
        }
    }

    public LockInternals(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = lockName;
        this.session = client.createSession(coordinationNodePath);
        this.sessionListenerWrapper = new ListenerWrapper<>(
                new ListenableAdder<CoordinationSession.State>() {
                    @Override
                    public void addListener(Consumer<CoordinationSession.State> listener) {
                        session.addStateListener(listener);
                    }

                    @Override
                    public void removeListener(Consumer<CoordinationSession.State> listener) {
                        session.removeStateListener(listener);
                    }
                }
        );
    }

    public void start() {
        this.sessionConnectionTask = session.connect().thenApply(status -> {
            logger.debug("Session connection status: {}", status);
            return status;
        });

        Consumer<CoordinationSession.State> listener = state -> {
            switch (state) {
                case RECONNECTED: {
                    logger.debug("Session RECONNECTED");
                    reconnect();
                    break;
                }
                case CLOSED: {
                    logger.debug("Session CLOSED, releasing lock");
                    leaseData = null;
                    break;
                }
                case LOST: {
                    logger.debug("Session LOST, releasing lock");
                    leaseData = null;
                    break;
                }
            }
        };

        session.addStateListener(listener);
    }

    private CoordinationSession connectedSession() {
        if (sessionConnectionTask == null) {
            throw new IllegalStateException("Not started yet");
        }
        try {
            sessionConnectionTask.get().expectSuccess("Unable to connect to session on: " + coordinationNodePath);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return session;
    }

    private void reconnect() {
        LeaseData currentLeaseData = leaseData;
        CoordinationSession coordinationSession = session;
        long oldId = currentLeaseData.getLeaseSessionId();
        long newId = coordinationSession.getId();
        if (oldId != newId) {
            logger.warn(
                    "Current session with new id: {} lost lease after reconnection on semaphore: {}",
                    newId,
                    semaphoreName
            );
            leaseData = null;
        } else {
            logger.debug("Successfully reestablished session with same id: {}", newId);
        }
    }

    // TODO: interruptible?
    public synchronized boolean release() {
        logger.debug("Trying to release");
        if (leaseData == null) {
            logger.debug("Already released");
            return false;
        }

        try {
            boolean releasedSuccess = leaseData.getProcessLease().release()
                    .thenApply(it -> {
                        logger.debug("Released lock");
                        return true;
                    })
                    .get();
            leaseData = null;
            return releasedSuccess;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param deadline
     * @return true - if successfully acquired lock
     * @throws Exception
     * @throws LockAlreadyAcquiredException
     * @throws LockAcquireFailedException
     */
    // TODO: deadlock?
    public synchronized LeaseData tryAcquire(
            @Nullable Instant deadline,
            boolean exclusive,
            byte[] data
    ) throws Exception {
        logger.debug("Trying to acquire with deadline: {}, exclusive: {}", deadline, exclusive);

        if (leaseData != null) {
            if (leaseData.isExclusive() == exclusive) {
                throw new LockAlreadyAcquiredException(
                        coordinationNodePath,
                        semaphoreName
                );
            }
            if (!leaseData.isExclusive() && exclusive) {
                throw new LockUpgradeFailedException(
                        coordinationNodePath,
                        semaphoreName
                );
            }
        }

        Optional<SemaphoreLease> lease = tryBlockingLock(deadline, exclusive, data);
        if (lease.isPresent()) {
            leaseData = new LeaseData(lease.get(), exclusive, 1);
            logger.debug("Successfully acquired lock: {}", semaphoreName);
            return leaseData;
        }
        logger.debug("Unable to acquire lock: {}", semaphoreName);
        return null;
    }

    private Optional<SemaphoreLease> tryBlockingLock(
            @Nullable Instant deadline,
            boolean exclusive,
            byte[] data
    ) throws Exception {
        int retryCount = 0;
        CoordinationSession coordinationSession = connectedSession();

        while (coordinationSession.getState().isActive() && (deadline == null || Instant.now().isBefore(deadline))) {
            retryCount++;

            Duration timeout;
            if (deadline == null) {
                timeout = DEFAULT_TIMEOUT;
            } else {
                timeout = Duration.between(Instant.now(), deadline); // TODO: use external Clock instead of Instant?
            }

//            leaseSessionId = coordinationSession.getId();
            CompletableFuture<Result<SemaphoreLease>> acquireTask = coordinationSession.acquireEphemeralSemaphore(
                    semaphoreName, exclusive, data, timeout // TODO: change Session API to use deadlines
            );
            Result<SemaphoreLease> leaseResult;
            try {
                leaseResult = acquireTask.get();
            } catch (InterruptedException e) {
                // If acquire is interrupted, then release immediately
                Thread.currentThread().interrupt();
                acquireTask.thenAccept(acquireResult -> {
                    if (!acquireResult.getStatus().isSuccess()) {
                        return;
                    }
                    SemaphoreLease lease = acquireResult.getValue();
                    lease.release();
                });
                throw e;
            }

            Status status = leaseResult.getStatus();
            logger.debug("Lease result status: {}", status);

            if (status.isSuccess()) {
                logger.debug("Successfully acquired the lock");
                return Optional.of(leaseResult.getValue());
            }

            if (status.getCode() == StatusCode.TIMEOUT) {
                logger.debug("Trying to acquire semaphore {} again, retries: {}", semaphoreName, retryCount);
                continue;
            }

            if (!status.getCode().isRetryable(true)) {
                status.expectSuccess("Unable to retry acquiring semaphore");
                throw new LockAcquireFailedException(coordinationNodePath, semaphoreName);
            }
        }

        if (deadline != null && Instant.now().compareTo(deadline) >= 0) {
            return Optional.empty();
        }

        throw new LockAcquireFailedException(coordinationNodePath, semaphoreName);
    }

    public String getCoordinationNodePath() {
        return coordinationNodePath;
    }

    public String getSemaphoreName() {
        return semaphoreName;
    }

    public CoordinationSession getCoordinationSession() {
        return session;
    }

    public CoordinationSession getConnectedCoordinationSession() {
        return connectedSession();
    }

    public @Nullable LeaseData getLeaseData() {
        return leaseData;
    }

    public boolean isAcquired() {
        return leaseData != null;
    }

    @Override
    public Listenable<CoordinationSession.State> getListenable() {
        return sessionListenerWrapper;
    }

    @Override
    public void close() {
        try {
            release();
        } catch (Exception ignored) {
        }

        session.close();
        sessionListenerWrapper.clearListeners();
    }
}
