package com.ruoyi.research.domain;

import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class ResearchGroupUnit implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;

    @NotNull(message = "Department is required")
    private Long deptId;

    @Pattern(regexp = "LEAD|PARTICIPANT", message = "Unit type must be LEAD or PARTICIPANT")
    private String unitType;

    private Long managerUserId;
    private String status;
    private String createBy;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }
    public Long getManagerUserId() { return managerUserId; }
    public void setManagerUserId(Long managerUserId) { this.managerUserId = managerUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
