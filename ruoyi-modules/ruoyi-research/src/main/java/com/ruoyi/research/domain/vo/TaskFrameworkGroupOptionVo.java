package com.ruoyi.research.domain.vo;

import java.util.ArrayList;
import java.util.List;

public class TaskFrameworkGroupOptionVo
{
    private Long groupId;
    private String groupName;
    private List<TaskFrameworkUnitOptionVo> units = new ArrayList<>();

    public Long getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public List<TaskFrameworkUnitOptionVo> getUnits()
    {
        return units;
    }

    public void setUnits(List<TaskFrameworkUnitOptionVo> units)
    {
        this.units = units;
    }
}
