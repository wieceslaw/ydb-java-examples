package tech.ydb.coordination.recipes.example.lib.lock;

public class LockAcquireFailedException extends RuntimeException {
    public LockAcquireFailedException(String message) {
        super(message);
    }
}
