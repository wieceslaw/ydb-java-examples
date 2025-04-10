package tech.ydb.example.coordination.recipes.lib.group;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Group membership management. Adds this instance into a group and keeps a cache of members in the group.
 */
public interface GroupMembership extends Closeable {
    void start() throws Exception;

    GroupMember getCurrentMember();

    Set<GroupMember> getCurrentMembers();

    void subscribe(GroupChangeListener listener);

    void subscribe(GroupChangeListener listener, Executor executor);

    void unsubscribe(GroupChangeListener listener);
}
