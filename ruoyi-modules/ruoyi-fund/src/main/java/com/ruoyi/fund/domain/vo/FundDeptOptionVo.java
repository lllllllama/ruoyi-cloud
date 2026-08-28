package com.ruoyi.fund.domain.vo;

/** Minimal department option; internal leaders and hierarchy are not exposed. */
public class FundDeptOptionVo
{
    private Long deptId;
    private String deptName;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
}
