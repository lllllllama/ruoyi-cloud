package com.ruoyi.research.domain.vo;

/** Minimal research-group option exposed to authenticated selectors. */
public class ResearchGroupOptionVo
{
    private Long groupId;
    private String groupCode;
    private String groupName;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
