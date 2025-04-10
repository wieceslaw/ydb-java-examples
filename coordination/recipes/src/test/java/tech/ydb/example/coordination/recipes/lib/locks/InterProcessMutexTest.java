package tech.ydb.example.coordination.recipes.lib.locks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class InterProcessMutexTest {
    private static final Logger log = LoggerFactory.getLogger(InterProcessMutexTest.class);

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    private static String connectionString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ydb.useTls() ? "grpcs://" : "grpc://");
        sb.append(ydb.endpoint());
        sb.append(ydb.database());
        return sb.toString();
    }

    @Test
    public void startUp() {
        try (GrpcTransport transport = GrpcTransport.forConnectionString(connectionString())
                .withAuthProvider((AuthRpcProvider<Object>) o -> null)
                .build()) {

            CoordinationClient client = CoordinationClient.newClient(transport);
            client.createNode("examples/app").join().expectSuccess("cannot create coordination path");
            ReadWriteInterProcessLock lock = new ReadWriteInterProcessLock(
                    client,
                    "examples/app",
                    "default_lock"
            );
            test(lock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void test(ReadWriteInterProcessLock lock) throws Exception {
        // scenario:
        // lock()
        // sleep()
        // unlock()

        lock.readLock().acquire();
        assertThrows(
                LockUpgradeFailedException.class,
                () -> lock.writeLock().acquire(),
                "Unable to upgrade from read lock to write lock"
        );
        assertThrows(
                LockAlreadyAcquiredException.class,
                () -> lock.readLock().acquire(),
                "Read lock is already acquired"
        );
    }

    /**
     * Asserts that code does not throw any exceptions
     */
    @Test
    void simpleReadLockTest() throws Exception {
        ReadWriteInterProcessLock lock = getReadWriteInterProcessLock();

        InterProcessLock readLock = lock.readLock();
        readLock.acquire();
        Thread.sleep(100);
        readLock.release();
    }

    /**
     * Asserts that code does not throw any exceptions
     */
    @Test
    void simpleWriteLockTest() throws Exception {
        ReadWriteInterProcessLock lock = getReadWriteInterProcessLock();

        InterProcessLock writeLock = lock.writeLock();
        writeLock.acquire();
        Thread.sleep(100);
        writeLock.release();
    }

    /**
     * Asserts that code does not throw any exceptions
     */
    @Test
    void combinedReadAndWriteLockTest() throws Exception {
        ReadWriteInterProcessLock lock = getReadWriteInterProcessLock();

        InterProcessLock readLock = lock.readLock();
        InterProcessLock writeLock = lock.writeLock();

        readLock.acquire();
        Thread.sleep(100);
        readLock.release();

        writeLock.acquire();
        Thread.sleep(100);
        writeLock.release();
    }

    @Test
    void concurrentReadAndWriteLockTest() throws Exception {
        String nodePath = "NodePathConcurrentReadAndWriteLockTest";
        String lockName = "LockNameConcurrentReadAndWriteLockTest";

        ReadWriteInterProcessLock lock1 = getReadWriteInterProcessLock(nodePath, lockName);
        ReadWriteInterProcessLock lock2 = getReadWriteInterProcessLock(nodePath, lockName);

        InterProcessLock readLock = lock1.readLock();
        InterProcessLock writeLock = lock2.writeLock();

        int readTaskCycles = 5;
        int writeTaskCycles = 5;

        // Вопросы:

        // Тесты распределенной конкурентности -- сколько циклов? показывают что-то реально?
        // Тесты при разрыве сессии -- как реализовать?

        // Положительные кейсы
        // Отрицательные кейсы

        // 1) нет ошибок
        // 2) лок действительно берется и создается, тем кем надо? Через соседнее подключение и describe?

        // Вокруг каждый функциональности (по описанию интерфейса)

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<?> readLockTask = executor.submit(() -> {
            try {
                for (int i = 0; i < readTaskCycles; i++) {
                    readLock.acquire();
                    log.debug("Read lock acquired");
                    Thread.sleep(100);
                    readLock.release();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> writeLockTask = executor.submit(() -> {
            try {
                for (int i = 0; i < writeTaskCycles; i++) {
                    writeLock.acquire();
                    log.debug("Write lock acquired");
                    Thread.sleep(100);
                    writeLock.release();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        writeLockTask.get();
        readLockTask.get();

        executor.shutdown();
    }

    ReadWriteInterProcessLock getReadWriteInterProcessLock() {
        return getReadWriteInterProcessLock(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    // TODO: close?
    ReadWriteInterProcessLock getReadWriteInterProcessLock(String nodePath, String lockName) {
        GrpcTransport ydbTransport = ydb.createTransport();
        CoordinationClient client = CoordinationClient.newClient(ydbTransport);
        client.createNode(nodePath).join().expectSuccess("cannot create coordination path");
        ReadWriteInterProcessLock lock = new ReadWriteInterProcessLock(
                client,
                nodePath,
                lockName
        );
        return lock;
    }

}



























