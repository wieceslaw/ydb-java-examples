package tech.ydb.example.coordination.recipes.lib.group;

import java.util.Arrays;
import java.util.Objects;

public class GroupMember {
    private final String groupId;
    private final String memberId;
    private final byte[] memberData;

    public GroupMember(String groupId, String memberId, byte[] memberData) {
        this.groupId = groupId;
        this.memberId = memberId;
        this.memberData = memberData;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getMemberId() {
        return memberId;
    }

    public byte[] getMemberData() {
        return memberData;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GroupMember that = (GroupMember) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(memberId, that.memberId) &&
                Objects.deepEquals(memberData, that.memberData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, memberId, Arrays.hashCode(memberData));
    }

    @Override
    public String toString() {
        return "GroupMemberInfo{" +
                "groupId='" + groupId + '\'' +
                ", memberId='" + memberId + '\'' +
                ", memberData=" + Arrays.toString(memberData) +
                '}';
    }
}
