package tech.ydb.example.coordination.recipes.lib.locks;

public class LockUpgradeFailedException extends LockAcquireFailedException {
    public LockUpgradeFailedException(String coordinationNodePath, String semaphoreName) {
        super(
                "Unable to upgrade lease from inclusive to exclusive, " +
                        "name=" + semaphoreName + ", " + "path=" + coordinationNodePath,
                coordinationNodePath,
                semaphoreName
        );
    }
}
