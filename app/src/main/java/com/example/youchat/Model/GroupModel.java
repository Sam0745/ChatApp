package com.example.youchat.Model;

import java.util.HashMap;
import java.util.Map;

public class GroupModel {
    private String groupId;
    private String groupName;

    public GroupModel() {
    }

    public GroupModel(String groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;

    }



    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}