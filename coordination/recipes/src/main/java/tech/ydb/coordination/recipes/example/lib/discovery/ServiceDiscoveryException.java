package tech.ydb.coordination.recipes.example.lib.discovery;

/**
 * Исключение, указывающее на проблемы при взаимодействии с Service Discovery.
 */
class ServiceDiscoveryException extends Exception {
    public ServiceDiscoveryException(String message) {
        super(message);
    }

    public ServiceDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
