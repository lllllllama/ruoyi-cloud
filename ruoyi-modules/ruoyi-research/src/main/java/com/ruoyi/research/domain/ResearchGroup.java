package com.ruoyi.research.domain;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class ResearchGroup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long groupId;

    @NotBlank(message = "Group code is required")
    @Size(max = 64, message = "Group code must not exceed 64 characters")
    private String groupCode;

    @NotBlank(message = "Group name is required")
    @Size(max = 200, message = "Group name must not exceed 200 characters")
    private String groupName;

    @NotNull(message = "Lead department is required")
    private Long leadDeptId;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String status;
    private Integer sort;

    @Valid
    private List<ResearchGroupUnit> units;

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
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public List<ResearchGroupUnit> getUnits() { return units; }
    public void setUnits(List<ResearchGroupUnit> units) { this.units = units; }
}
