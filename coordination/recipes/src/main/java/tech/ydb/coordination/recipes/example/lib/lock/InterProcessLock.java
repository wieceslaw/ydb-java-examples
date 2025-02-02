package tech.ydb.coordination.recipes.example.lib.lock;

import java.time.Duration;

public interface InterProcessLock {
    void acquire() throws Exception;

    boolean acquire(Duration waitDuration) throws Exception;

    void release() throws Exception;

    /**
     * Returns true if the mutex is acquired by a thread in this JVM
     *
     * @return true/false
     */
    boolean isAcquiredInThisProcess();
}
