package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.TaskInfo;

public interface TaskInfoService
{
    TaskInfo selectById(Long taskId);

    List<TaskInfo> selectList(TaskInfo query);

    int insert(TaskInfo task);

    int update(TaskInfo task);

    int deleteByIds(Long[] taskIds);
}
