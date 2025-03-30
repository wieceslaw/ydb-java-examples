package tech.ydb.coordination.recipes.example.lib.discovery;

import java.util.Collections;
import java.util.Set;

import tech.ydb.coordination.recipes.example.lib.util.Serializer;

public class ServiceDiscoveryImpl implements ServiceDiscovery {
//    private final Map<String,  > servicesMap; // map to watcher or registerer
    private final Serializer<InstanceInfo> instanceInfoSerializer;

    public ServiceDiscoveryImpl(Serializer<InstanceInfo> instanceInfoSerializer) {
        this.instanceInfoSerializer = instanceInfoSerializer;
    }

    @Override
    public void registerService(InstanceInfo instanceInfo) throws ServiceDiscoveryException {
        byte[] instanceData = instanceInfoSerializer.serialize(instanceInfo);
        // TODO: acquireEphemeralSemaphore non-exclusive with instance data
        // TODO: what if already acquired, but without data?
    }

    @Override
    public void unregisterService(String serviceId, String instanceId) throws ServiceDiscoveryException {
        // TODO: releaseSemaphore
    }

    @Override
    public Set<InstanceInfo> getServiceInstances(String serviceId) throws ServiceDiscoveryException {
        return Collections.emptySet();
    }

    @Override
    public void subscribe(String serviceId, ServiceChangeListener listener) throws ServiceDiscoveryException {
        // TODO: add watcher
    }

    @Override
    public void unsubscribe(String serviceId, ServiceChangeListener listener) throws ServiceDiscoveryException {
        // TODO: remove watcher
    }
}
