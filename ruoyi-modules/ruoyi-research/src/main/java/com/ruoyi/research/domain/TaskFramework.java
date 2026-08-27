package com.ruoyi.research.domain;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class TaskFramework extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long frameworkId;

    @NotNull(message = "Research group is required")
    private Long groupId;

    @NotBlank(message = "Framework name is required")
    @Size(max = 200, message = "Framework name must not exceed 200 characters")
    private String frameworkName;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must not be earlier than 1900")
    @Max(value = 9999, message = "Year must not exceed 9999")
    private Integer year;

    @NotNull(message = "Lead department is required")
    private Long leadDeptId;

    @Size(max = 2000, message = "Overall goal must not exceed 2000 characters")
    private String overallGoal;

    private String status;
    private Integer sort;
    private String groupName;
    private String leadDeptName;

    @Valid
    private List<TaskFrameworkUnit> units;

    public Long getFrameworkId() { return frameworkId; }
    public void setFrameworkId(Long frameworkId) { this.frameworkId = frameworkId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getFrameworkName() { return frameworkName; }
    public void setFrameworkName(String frameworkName) { this.frameworkName = frameworkName; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Long getLeadDeptId() { return leadDeptId; }
    public void setLeadDeptId(Long leadDeptId) { this.leadDeptId = leadDeptId; }
    public String getOverallGoal() { return overallGoal; }
    public void setOverallGoal(String overallGoal) { this.overallGoal = overallGoal; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getLeadDeptName() { return leadDeptName; }
    public void setLeadDeptName(String leadDeptName) { this.leadDeptName = leadDeptName; }
    public List<TaskFrameworkUnit> getUnits() { return units; }
    public void setUnits(List<TaskFrameworkUnit> units) { this.units = units; }
}
