package tech.ydb.coordination.recipes.example.lib.discovery;

import java.util.Set;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.recipes.example.lib.locks.LockInternals;

public class ServiceDiscoveryImpl implements ServiceDiscovery {
    private final CoordinationClient client;
    private final String coordinationNode;
    private final String semaphoreName;
    private final LockInternals lockInternals;
    private final String data;

    public ServiceDiscoveryImpl(
            CoordinationClient client,
            String coordinationNodePath,
            String semaphoreName,
            String data
    ) {
        this.client = client;
        this.coordinationNode = coordinationNodePath;
        this.semaphoreName = semaphoreName;
        this.lockInternals = new LockInternals(client, coordinationNodePath, semaphoreName);
        this.data = data;
    }

    @Override
    public void registerService(String serviceName, String serviceAddress) throws ServiceDiscoveryException {
        try {
            lockInternals.tryAcquire(null, false, data.getBytes());
        } catch (Exception e) {
            throw new ServiceDiscoveryException("Error", e);
        }
    }

    @Override
    public void unregisterService(String serviceName, String serviceAddress) throws ServiceDiscoveryException {
        lockInternals.release();
    }

    @Override
    public Set<String> getServiceInstances(String serviceName) throws ServiceDiscoveryException {
    }

    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) throws ServiceDiscoveryException {

    }

    @Override
    public void unsubscribe(String serviceName, ServiceChangeListener listener) throws ServiceDiscoveryException {

    }
}

