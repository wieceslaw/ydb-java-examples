package tech.ydb.example.coordination.recipes.lib.util;

/**
 * Exception thrown during serialization/deserialization.
 */
public class SerializationException extends RuntimeException {
    SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
