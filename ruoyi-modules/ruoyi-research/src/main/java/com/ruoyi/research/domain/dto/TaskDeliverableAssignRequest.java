package com.ruoyi.research.domain.dto;

import java.util.List;
import javax.validation.constraints.NotNull;

public class TaskDeliverableAssignRequest
{
    @NotNull(message = "Assignee list is required")
    private List<Long> userIds;

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
}
