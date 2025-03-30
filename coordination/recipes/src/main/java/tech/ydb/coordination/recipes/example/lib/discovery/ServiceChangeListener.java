package tech.ydb.coordination.recipes.example.lib.discovery;

import java.util.Set;

/**
 * Listener interface for handling updates to the list of service instances.
 */
public interface ServiceChangeListener {
    /**
     * Invoked when there is a change in the list of service instances.
     *
     * @param serviceId    the name of the service
     * @param newInstances the new list of new instances
     */
    void onChange(String serviceId, Set<InstanceInfo> newInstances);
}
