package tech.ydb.coordination.recipes.example.lib.discovery;

import java.util.Set;

/**
 * The ServiceDiscovery interface provides methods to register, deregister, and discover service instances
 * in a distributed system. It also supports subscription to changes in service instances for dynamic updating.
 */
public interface ServiceDiscovery {

    /**
     * Registers a new service instance with the specified name and address.
     *
     * @param instanceInfo info about service instance
     * @throws ServiceDiscoveryException if the registration fails
     */
    void registerService(InstanceInfo instanceInfo) throws ServiceDiscoveryException;

    /**
     * Unregisters a service instance with the specified name and address.
     *
     * @param serviceId the name of the service
     * @param instanceId the id of the service instance
     * @throws ServiceDiscoveryException if the unregistration fails
     */
    void unregisterService(String serviceId, String instanceId) throws ServiceDiscoveryException;

    /**
     * Retrieves a list of addresses for all registered instances of a given service.
     *
     * @param serviceId the name of the service
     * @return a set of infos about instances
     * @throws ServiceDiscoveryException if retrieving the list fails
     */
    Set<InstanceInfo> getServiceInstances(String serviceId) throws ServiceDiscoveryException;

    /**
     * Subscribes to changes in the list of instances for the specified service.
     *
     * @param serviceId the name of the service
     * @param listener the listener for changes
     * @throws ServiceDiscoveryException if the subscription fails
     */
    void subscribe(String serviceId, ServiceChangeListener listener) throws ServiceDiscoveryException;

    /**
     * Cancels the subscription to changes for the specified service.
     *
     * @param serviceId the name of the service
     * @param listener the listener for changes
     * @throws ServiceDiscoveryException if the unsubscription fails
     */
    void unsubscribe(String serviceId, ServiceChangeListener listener) throws ServiceDiscoveryException;
}

