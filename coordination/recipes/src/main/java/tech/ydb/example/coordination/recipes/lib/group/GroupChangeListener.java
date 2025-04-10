package tech.ydb.example.coordination.recipes.lib.group;

import java.util.Set;

/**
 * Listener interface for handling updates to the list of group instances.
 */
public interface GroupChangeListener {
    /**
     * Invoked when there is a change in the list of group instances.
     *
     * @param groupId    the name of the group
     * @param members    the set of members
     */
    void onChange(String groupId, Set<GroupMember> members);
}
