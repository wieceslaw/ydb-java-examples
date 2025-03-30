package tech.ydb.coordination.recipes.example.lib.discovery;

/**
 * Exception indicating issues during interaction with the Service Discovery system.
 */
public class ServiceDiscoveryException extends Exception {
    /**
     * Constructs a new ServiceDiscoveryException with the specified detail message.
     *
     * @param message the detail message
     */
    ServiceDiscoveryException(String message) {
        super(message);
    }

    /**
     * Constructs a new ServiceDiscoveryException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    ServiceDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
