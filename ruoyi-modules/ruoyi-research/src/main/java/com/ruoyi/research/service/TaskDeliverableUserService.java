package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.TaskDeliverableUser;

public interface TaskDeliverableUserService
{
    List<TaskDeliverableUser> selectByDeliverableId(Long deliverableId);

    int assign(Long deliverableId, List<Long> userIds);
}
