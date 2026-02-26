package com.tianye.hrsystem.model;

/**
 * @ClassName: UserObject
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年09月28日 17:55
 **/
public class UserObject {
    private String  id;
    private String name;
    private String  groupId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
