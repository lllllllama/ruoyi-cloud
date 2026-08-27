package com.ruoyi.research.service.impl;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.mapper.TaskInfoMapper;
import com.ruoyi.research.service.TaskCompletionService;

@Service
public class TaskCompletionServiceImpl implements TaskCompletionService
{
    private static final String STATUS_RUNNING = "1";
    private static final String STATUS_FINISHED = "2";
    private static final String STATUS_CLOSED = "3";

    @Autowired
    private TaskInfoMapper taskMapper;

    @Override
    public void recalculateFromTask(Long taskId)
    {
        Set<Long> visited = new HashSet<>();
        Long currentId = taskId;
        while (currentId != null && currentId.longValue() != 0L)
        {
            if (!visited.add(currentId))
            {
                throw new ServiceException("Invalid cyclic task data detected");
            }
            TaskInfo task = taskMapper.selectForUpdate(currentId);
            if (task == null)
            {
                throw new ServiceException("Research task does not exist");
            }
            if (!STATUS_CLOSED.equals(task.getStatus()))
            {
                boolean finished;
                if (taskMapper.countActiveChildren(currentId) > 0)
                {
                    finished = taskMapper.countUnfinishedEffectiveChildren(currentId) == 0;
                }
                else
                {
                    finished = taskMapper.countUnfinishedRequiredDeliverables(currentId) == 0;
                }
                String status = finished ? STATUS_FINISHED : STATUS_RUNNING;
                if (taskMapper.updateCompletionStatus(currentId, status, SecurityUtils.getUsername()) == 0)
                {
                    throw new ServiceException("Failed to update task completion status");
                }
            }
            currentId = task.getParentId();
        }
    }
}
