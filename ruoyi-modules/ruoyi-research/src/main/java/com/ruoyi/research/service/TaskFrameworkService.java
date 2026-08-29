package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.TaskFramework;
import com.ruoyi.research.domain.vo.TaskFrameworkGroupOptionVo;

public interface TaskFrameworkService
{
    TaskFramework selectById(Long frameworkId);

    List<TaskFramework> selectList(TaskFramework query);

    List<TaskFramework> selectOptions();

    List<TaskFrameworkGroupOptionVo> selectManagedGroupOptions();

    int insert(TaskFramework framework);

    int update(TaskFramework framework);

    int deleteByIds(Long[] frameworkIds);
}
