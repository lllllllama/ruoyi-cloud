package com.ruoyi.research.api.domain;

import java.io.Serializable;

public class ResearchUserPermissionDto implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long groupId;
    private Long userId;
    private Boolean groupMember;
    private Boolean groupLeader;
    private Boolean groupCore;
    private Boolean groupExpert;
    private Boolean unitManager;
    private Boolean canView;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Boolean getGroupMember() { return groupMember; }
    public void setGroupMember(Boolean groupMember) { this.groupMember = groupMember; }
    public Boolean getGroupLeader() { return groupLeader; }
    public void setGroupLeader(Boolean groupLeader) { this.groupLeader = groupLeader; }
    public Boolean getGroupCore() { return groupCore; }
    public void setGroupCore(Boolean groupCore) { this.groupCore = groupCore; }
    public Boolean getGroupExpert() { return groupExpert; }
    public void setGroupExpert(Boolean groupExpert) { this.groupExpert = groupExpert; }
    public Boolean getUnitManager() { return unitManager; }
    public void setUnitManager(Boolean unitManager) { this.unitManager = unitManager; }
    public Boolean getCanView() { return canView; }
    public void setCanView(Boolean canView) { this.canView = canView; }
}
