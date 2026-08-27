package com.ruoyi.research.api.domain;

import java.io.Serializable;

public class ResearchGroupUnitDto implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;
    private Long deptId;
    private String deptName;
    private String unitType;
    private Long managerUserId;
    private String managerUserName;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }
    public Long getManagerUserId() { return managerUserId; }
    public void setManagerUserId(Long managerUserId) { this.managerUserId = managerUserId; }
    public String getManagerUserName() { return managerUserName; }
    public void setManagerUserName(String managerUserName) { this.managerUserName = managerUserName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
