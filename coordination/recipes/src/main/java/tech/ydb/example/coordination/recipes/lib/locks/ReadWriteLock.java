package tech.ydb.example.coordination.recipes.lib.locks;

import java.time.Duration;
import java.time.Instant;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.example.coordination.recipes.lib.util.Listenable;
import tech.ydb.example.coordination.recipes.lib.util.ListenableProvider;

public class ReadWriteLock {
    private final InternalLock readLock;
    private final InternalLock writeLock;

    public ReadWriteLock(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        LockInternals lockInternals = new LockInternals(
                client, coordinationNodePath, lockName
        );
        lockInternals.start();
        // TODO: Share same lockInternals?

        // захватили read - ОК
        // захватили write - ОК
        // захватили read, захватили write - ОК через время, освободили write - ?
        // захватили write, захватили read - ОК сразу, освободили read - ?

        this.readLock = new InternalLock(lockInternals, false);
        this.writeLock = new InternalLock(lockInternals, true);
    }

    public InterProcessLock writeLock() {
        return readLock;
    }

    public InterProcessLock readLock() {
        return writeLock;
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

}
