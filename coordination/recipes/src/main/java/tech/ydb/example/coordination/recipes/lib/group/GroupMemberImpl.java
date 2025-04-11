package tech.ydb.example.coordination.recipes.lib.group;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.example.coordination.recipes.lib.locks.LockInternals;
import tech.ydb.example.coordination.recipes.lib.watch.SemaphoreWatchListener;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class GroupMemberImpl implements GroupMembership {
    private final GroupMember currentMemberInfo;
    private final LockInternals lockInternals;
    private final SemaphoreWatchListener semaphoreWatchListener;

    public GroupMemberImpl(
            CoordinationClient client,
            String coordinationNodePath,
            String groupId,
            String memberId,
            byte[] memberData
    ) {
        this.currentMemberInfo = new GroupMember(
                groupId,
                memberId,
                memberData
        );
        this.lockInternals = new LockInternals(
                client,
                coordinationNodePath,
                groupId
        );
        this.semaphoreWatchListener = new SemaphoreWatchListener(
                lockInternals.getCoordinationSession(),
                groupId
        );
    }

    @Override
    public void start() throws Exception {
        lockInternals.start();
        // move to another thread?
        lockInternals.tryAcquire(
                null,
                false,
                currentMemberInfo.getMemberData()
        );
        semaphoreWatchListener.start();
    }

    @Override
    public GroupMember getCurrentMember() {
        return currentMemberInfo;
    }

    @Override
    public Set<GroupMember> getCurrentMembers() {
        return semaphoreWatchListener.getParticipants().stream().map(it -> new GroupMember(
                currentMemberInfo.getGroupId(),
                "", // TODO: get ID from data
                it.getData()
        )).collect(Collectors.toSet());
    }

    @Override
    public void subscribe(GroupChangeListener listener) {

    }

    @Override
    public void subscribe(GroupChangeListener listener, Executor executor) {

    }

    @Override
    public void unsubscribe(GroupChangeListener listener) {

    }

    @Override
    public void close() throws IOException {
        semaphoreWatchListener.close();
        lockInternals.close();
    }
}
