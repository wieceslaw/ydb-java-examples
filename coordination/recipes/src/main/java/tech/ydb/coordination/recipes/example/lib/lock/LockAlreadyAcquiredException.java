package tech.ydb.coordination.recipes.example.lib.lock;

public class LockAlreadyAcquiredException extends RuntimeException {
    public LockAlreadyAcquiredException(String message) {
        super(message);
    }
}
