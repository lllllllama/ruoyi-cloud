package com.ruoyi.research.mapper;

import java.util.List;
import com.ruoyi.research.domain.TaskDeliverableUser;

public interface TaskDeliverableUserMapper
{
    List<TaskDeliverableUser> selectByDeliverableId(Long deliverableId);

    int countByDeliverableId(Long deliverableId);

    int countByDeliverableAndUser(TaskDeliverableUser relation);

    int batchInsert(List<TaskDeliverableUser> relations);

    int deleteByDeliverableId(Long deliverableId);
}
