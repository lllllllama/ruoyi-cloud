package com.ruoyi.research.api.domain;

import java.io.Serializable;
import java.util.List;

public class ResearchGroupDto implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long groupId;
    private String groupCode;
    private String groupName;
    private Long leadDeptId;
    private String description;
    private String status;
    private List<ResearchGroupUnitDto> units;
    private List<ResearchGroupMemberDto> members;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Long getLeadDeptId() { return leadDeptId; }
    public void setLeadDeptId(Long leadDeptId) { this.leadDeptId = leadDeptId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<ResearchGroupUnitDto> getUnits() { return units; }
    public void setUnits(List<ResearchGroupUnitDto> units) { this.units = units; }
    public List<ResearchGroupMemberDto> getMembers() { return members; }
    public void setMembers(List<ResearchGroupMemberDto> members) { this.members = members; }
}
