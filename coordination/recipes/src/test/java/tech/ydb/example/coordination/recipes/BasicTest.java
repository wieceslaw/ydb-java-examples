package tech.ydb.example.coordination.recipes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.example.coordination.recipes.lib.locks.LockAlreadyAcquiredException;
import tech.ydb.example.coordination.recipes.lib.locks.LockInternals;
import tech.ydb.example.coordination.recipes.lib.locks.LockUpgradeFailedException;
import tech.ydb.example.coordination.recipes.lib.locks.ReadWriteInterProcessLock;
import tech.ydb.test.junit5.YdbHelperExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BasicTest {
    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();
    private static final Logger log = LoggerFactory.getLogger(BasicTest.class);

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
}
