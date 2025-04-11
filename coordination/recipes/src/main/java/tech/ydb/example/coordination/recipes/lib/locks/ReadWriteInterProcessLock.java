package tech.ydb.example.coordination.recipes.lib.locks;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.example.coordination.recipes.lib.util.Listenable;
import tech.ydb.example.coordination.recipes.lib.util.ListenableProvider;

public class ReadWriteInterProcessLock implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteInterProcessLock.class);

    private final LockInternals readLockInternals;
    private final LockInternals writeLockInternals;
    private final InternalLock readLock;
    private final InternalLock writeLock;

    public ReadWriteInterProcessLock(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        this.readLockInternals = new LockInternals(
                client, coordinationNodePath, lockName
        );
        readLockInternals.start();
        this.readLock = new InternalLock(readLockInternals, false);

        this.writeLockInternals = new LockInternals(
                client, coordinationNodePath, lockName
        );
        writeLockInternals.start();
        this.writeLock = new InternalLock(writeLockInternals, true);
    }

    public InterProcessLock writeLock() {
        return writeLock;
    }

    public InterProcessLock readLock() {
        return readLock;
    }

    private static class InternalLock implements InterProcessLock, ListenableProvider<CoordinationSession.State> {
        private final LockInternals lockInternals;
        private final boolean isExclusive;

        private InternalLock(LockInternals lockInternals, boolean isExclusive) {
            this.lockInternals = lockInternals;
            this.isExclusive = isExclusive;
        }

        @Override
        public void acquire() throws Exception {
            lockInternals.tryAcquire(
                    null,
                    isExclusive,
                    null
            );
        }

        @Override
        public boolean acquire(Duration waitDuration) throws Exception {
            Objects.requireNonNull(waitDuration, "wait duration must not be null");

            Instant deadline = Instant.now().plus(waitDuration);
            return lockInternals.tryAcquire(
                    deadline,
                    isExclusive,
                    null
            ) != null;
        }

        @Override
        public boolean release() {
            return lockInternals.release();
        }

        @Override
        public boolean isAcquiredInThisProcess() {
            return lockInternals.isAcquired();
        }

        @Override
        public Listenable<CoordinationSession.State> getListenable() {
            return lockInternals.getListenable();
        }
    }

    @Override
    public void close() {
        readLockInternals.close();
        writeLockInternals.close();
    }

}
