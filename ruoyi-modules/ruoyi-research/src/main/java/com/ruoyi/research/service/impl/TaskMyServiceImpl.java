package com.ruoyi.research.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.research.domain.vo.MyTaskVo;
import com.ruoyi.research.mapper.TaskMyMapper;
import com.ruoyi.research.service.TaskMyService;
import com.ruoyi.research.service.TaskPermissionService;

@Service
public class TaskMyServiceImpl implements TaskMyService
{
    private static final long NEAR_DUE_DAYS = 7L;

    @Autowired
    private TaskMyMapper taskMyMapper;

    @Autowired
    private TaskPermissionService taskPermissionService;

    @Override
    public List<MyTaskVo> selectMyTasks(Long userId)
    {
        List<MyTaskVo> tasks = taskMyMapper.selectMyTasks(userId);
        for (MyTaskVo task : tasks)
        {
            task.setCanSubmit(taskPermissionService.canSubmitDeliverable(task.getDeliverableId(), userId));
            task.setCanCreateSubmission(taskPermissionService.canCreateSubmission(
                    task.getDeliverableId(), userId));
            task.setTimeStatus(calculateTimeStatus(task.getDeadline(), task.getStatus()));
        }
        return tasks;
    }

    String calculateTimeStatus(Date deadline, String businessStatus)
    {
        if (deadline == null || "2".equals(businessStatus) || "3".equals(businessStatus))
        {
            return "NORMAL";
        }
        LocalDate dueDate = deadline instanceof java.sql.Date
                ? ((java.sql.Date) deadline).toLocalDate()
                : deadline.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (remainingDays < 0)
        {
            return "OVERDUE";
        }
        if (remainingDays <= NEAR_DUE_DAYS)
        {
            return "NEAR_DUE";
        }
        return "NORMAL";
    }
}
