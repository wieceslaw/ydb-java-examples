package tech.ydb.example.coordination.recipes.lib.locks;

public class LockAlreadyAcquiredException extends LockAcquireFailedException {
    public LockAlreadyAcquiredException(String coordinationNodePath, String semaphoreName) {
        super(
                "Lock=" + semaphoreName + " on path=" + coordinationNodePath + " is already acquired",
                coordinationNodePath,
                semaphoreName
        );
    }
}
