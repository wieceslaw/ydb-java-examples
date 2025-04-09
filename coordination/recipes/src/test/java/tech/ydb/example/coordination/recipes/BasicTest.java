package tech.ydb.example.coordination.recipes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.example.coordination.recipes.lib.locks.LockInternals;
import tech.ydb.test.junit5.YdbHelperExtension;

public class BasicTest {
    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();
    private static final Logger log = LoggerFactory.getLogger(BasicTest.class);

    private static String connectionString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ydb.useTls() ? "grpcs://" : "grpc://" );
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
            LockInternals lock = new LockInternals(
                    client,
                    "examples/app",
                    "default_lock"
            );
            lock.start();
            test(lock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void test(LockInternals lock) throws Exception {
        LockInternals.LeaseData leaseData = lock.tryAcquire(
                null,
                true,
                null
        );
        log.info("Lease data: {}", leaseData);
        LockInternals.LeaseData leaseData1 = lock.tryAcquire(
                null,
                false,
                null
        );
        log.info("Lease data: {}", leaseData1);
    }
}
