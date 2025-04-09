package tech.ydb.example.coordination.recipes.lib.locks;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.concurrent.ThreadSafe;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.example.coordination.recipes.lib.util.Listenable;
import tech.ydb.example.coordination.recipes.lib.util.ListenableProvider;

@ThreadSafe
public class InterProcessMutex implements InterProcessLock, ListenableProvider<CoordinationSession.State>, Closeable {
    private final LockInternals lockInternals;

    public InterProcessMutex(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        lockInternals = new LockInternals(client, coordinationNodePath, lockName);
        lockInternals.start();
    }

    @Override
    public void acquire() throws Exception {
        lockInternals.tryAcquire(
                null,
                true,
                null
        );
    }

    @Override
    public boolean acquire(Duration waitDuration) throws Exception {
        Instant deadline = Instant.now().plus(waitDuration);
        return lockInternals.tryAcquire(
                deadline,
                true,
                null
        ) != null;
    }

    @Override
    public boolean release() throws Exception {
        return lockInternals.release();
    }

    @Override
    public boolean isAcquiredInThisProcess() {
        return lockInternals.isAcquired();
    }

    @Override
    public Listenable<CoordinationSession.State> getListenable() {
        return null;
    }

    @Override
    public void close() {
        lockInternals.close();
    }
}
