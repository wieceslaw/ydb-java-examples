package tech.ydb.examples.simple;

import tech.ydb.core.rpc.RpcTransport;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.query.ExplainDataQueryResult;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;


/**
 * @author Sergey Polovko
 */
public class ExplainDataQuery extends SimpleExample {

    @Override
    void run(RpcTransport transport, String pathPrefix) {
        String tablePath = pathPrefix + getClass().getSimpleName();
        TableClient tableClient = TableClient.newClient(GrpcTableRpc.useTransport(transport)).build();

        Session session = tableClient.createSession()
            .join()
            .expect("cannot create session");

        session.dropTable(tablePath)
            .join();

        {
            String query =
                "CREATE TABLE [" + tablePath + "] (" +
                    "  key Uint32," +
                    "  value String," +
                    "  PRIMARY KEY(key)" +
                    ");";
            session.executeSchemeQuery(query)
                .join()
                .expect("cannot create table");
        }

        {
            String query = "SELECT * FROM [" + tablePath + "];";
            ExplainDataQueryResult result = session.explainDataQuery(query)
                .join()
                .expect("cannot explain query");

            System.out.println("--[ast]----------------------\n" + result.getQueryAst());
            System.out.println();
            System.out.println("--[plan]---------------------\n" + result.getQueryPlan());
        }

        session.close()
            .join()
            .expect("cannot close session");
    }

    public static void main(String[] args) {
        new ExplainDataQuery().doMain();
    }
}
