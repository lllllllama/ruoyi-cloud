package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.TaskDeliverable;

public interface TaskDeliverableService
{
    TaskDeliverable selectById(Long deliverableId);

    List<TaskDeliverable> selectList(TaskDeliverable query);

    int insert(TaskDeliverable deliverable);

    int update(TaskDeliverable deliverable);

    int delete(Long deliverableId);
}
