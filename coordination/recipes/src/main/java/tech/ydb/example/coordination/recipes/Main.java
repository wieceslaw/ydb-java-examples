package tech.ydb.example.coordination.recipes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.core.grpc.GrpcTransport;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar jdbc-coordination-api-example.jar <connection_url>");
            return;
        }

        String connectionString = args[0];

        LockApp app = null;
        try (GrpcTransport transport = GrpcTransport.forConnectionString(connectionString)
                .withAuthProvider((AuthRpcProvider<Object>) o -> null)
                .build()) {

            CoordinationClient client = CoordinationClient.newClient(transport);
            app = new LockApp(client);
            app.run();
        } finally {
            if (app != null) {
                app.close();
            }
        }
    }
}
