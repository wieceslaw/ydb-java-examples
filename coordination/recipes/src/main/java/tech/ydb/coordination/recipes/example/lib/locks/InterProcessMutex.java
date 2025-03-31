package tech.ydb.coordination.recipes.example.lib.locks;

import java.time.Duration;

import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.example.lib.util.Listenable;
import tech.ydb.coordination.recipes.example.lib.util.ListenableProvider;

@ThreadSafe
public class InterProcessMutex implements InterProcessLock, ListenableProvider<CoordinationSession.State> {
    private static final Logger logger = LoggerFactory.getLogger(InterProcessMutex.class);

    private final LockInternals lockInternals;

    public InterProcessMutex(
            CoordinationClient client,
            String coordinationNodePath,
            String lockName
    ) {
        lockInternals = new LockInternals(
                client,
                coordinationNodePath,
                lockName
        );
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
        return lockInternals.tryAcquire(
                waitDuration,
                true,
                null
        );
    }

    @Override
    public boolean release() {
        return lockInternals.release();
    }

    @Override
    public boolean isAcquiredInThisProcess() {
        return lockInternals.getProcessLease() == null;
    }

    @Override
    public Listenable<CoordinationSession.State> getListenable() {
        return lockInternals.getListenable();
    }
}
