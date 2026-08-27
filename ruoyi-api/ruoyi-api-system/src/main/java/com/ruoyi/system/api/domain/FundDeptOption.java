package com.ruoyi.system.api.domain;

import java.io.Serializable;

public class FundDeptOption implements Serializable
{
    private static final long serialVersionUID = 1L;
    private Long deptId;
    private String deptName;
    private String leader;
    private Long parentId;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
