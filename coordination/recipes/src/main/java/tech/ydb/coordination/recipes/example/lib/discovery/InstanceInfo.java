package tech.ydb.coordination.recipes.example.lib.discovery;

import tech.ydb.coordination.recipes.example.lib.util.ByteSerializable;

public interface InstanceInfo extends ByteSerializable {
    String getServiceId();
    String getInstanceId();
}
