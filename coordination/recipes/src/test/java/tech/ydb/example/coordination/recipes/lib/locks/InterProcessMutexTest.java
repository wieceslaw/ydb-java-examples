package tech.ydb.example.coordination.recipes.lib.locks;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InterProcessMutexTest {
    private static final Logger log = LoggerFactory.getLogger(InterProcessMutexTest.class);

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    private static GrpcTransport ydbTransport;
    private static CoordinationClient client;

    @BeforeAll
    public static void init() {
        ydbTransport = ydb.createTransport();
        client = CoordinationClient.newClient(ydbTransport);
    }

    @AfterAll
    public static void clean() {
        ydbTransport.close();
    }

    /**
     * Asserts that code does not throw any exceptions
     */
    @Test
    void simpleLockTest() throws Exception {
        InterProcessMutex lock = getInterProcessMutex();

        lock.acquire();
        Thread.sleep(100);
        lock.release();
    }

    /**
     * Asserts that there is no data race around counter that is protected by distributed lock
     * When locksN sessions tries to acquire lock at the same time
     */
    @Test
    void concurrentLockTest() {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int cycles = 10;
        int locksN = 10;

        String nodePath = UUID.randomUUID().toString();
        String lockName = UUID.randomUUID().toString();
        List<InterProcessMutex> locks = new ArrayList<>(locksN);
        for (int i = 0; i < locksN; i++) {
            locks.add(getInterProcessMutex(nodePath, lockName));
        }

        AtomicInteger counter = new AtomicInteger(0);

        // when
        List<Callable<Void>> tasks = locks.stream().map(lock ->
                (Callable<Void>) () -> {
                    for (int i = 0; i < cycles; i++) {
                        lock.acquire();
                        int start = counter.get();
                        log.debug("Lock acquired, cycle = {}, count = {}", i, start);
                        Thread.sleep(100);
                        counter.set(start + 1);
                        log.debug("Lock released, cycle = {}", i);
                        lock.release();
                    }
                    return null;
                }
        ).collect(Collectors.toList());

        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception ignored) {
        }

        // then
        assertEquals(cycles * locksN, counter.get());

        executor.shutdown();
    }

    // Тесты распределенной конкурентности -- сколько циклов? показывают что-то реально?
    // Тесты при разрыве сессии -- мок

    // Положительные кейсы
    // Отрицательные кейсы

    // 1) нет ошибок
    // 2) лок действительно берется и создается, тем кем надо? Через соседнее подключение и describe?

    // Вокруг каждый функциональности (по описанию интерфейса)

    InterProcessMutex getInterProcessMutex() {
        return getInterProcessMutex(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    InterProcessMutex getInterProcessMutex(String nodePath, String lockName) {
        client.createNode(nodePath).join().expectSuccess("cannot create coordination path");
        InterProcessMutex lock = new InterProcessMutex(
                client,
                nodePath,
                lockName
        );
        return lock;
    }

}



























