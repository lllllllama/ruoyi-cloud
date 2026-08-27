package com.ruoyi.research.domain;

import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotNull;

public class TaskFrameworkUnit implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long frameworkId;
    private Long groupId;

    @NotNull(message = "Collaborating department is required")
    private Long deptId;

    private String deptName;
    private String createBy;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFrameworkId() { return frameworkId; }
    public void setFrameworkId(Long frameworkId) { this.frameworkId = frameworkId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
