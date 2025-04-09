package tech.ydb.example.coordination.recipes.lib.election;

public interface LeaderElectionListener {
    void takeLeadership() throws Exception;
}
