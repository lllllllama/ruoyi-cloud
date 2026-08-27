package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.TaskFramework;

public interface TaskFrameworkService
{
    TaskFramework selectById(Long frameworkId);

    List<TaskFramework> selectList(TaskFramework query);

    int insert(TaskFramework framework);

    int update(TaskFramework framework);

    int deleteByIds(Long[] frameworkIds);
}
