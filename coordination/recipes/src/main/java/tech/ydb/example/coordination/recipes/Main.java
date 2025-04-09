package tech.ydb.example.coordination.recipes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.auth.iam.CloudAuthHelper;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.core.grpc.GrpcTransport;

import java.util.concurrent.locks.Lock;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar jdbc-coordination-api-example.jar <connection_url>");
            return;
        }

        String connectionString = args[0];
        LockApp app = new LockApp(connectionString);
        app.run();
        app.close();
    }
}
