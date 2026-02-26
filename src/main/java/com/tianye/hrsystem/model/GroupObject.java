package com.tianye.hrsystem.model;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName: ClassObject
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年07月24日 22:07
 **/
public class GroupObject implements Serializable {
    private Long groupId;
    private String groupName;
    private Long memberCount;
    List<ComboboxItem> shifts;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Long memberCount) {
        this.memberCount = memberCount;
    }

    public List<ComboboxItem> getShifts() {
        return shifts;
    }

    public void setShifts(List<ComboboxItem> shifts) {
        this.shifts = shifts;
    }
}
