package com.ruoyi.research.mapper;

import java.util.List;
import com.ruoyi.research.domain.TaskFrameworkUnit;

public interface TaskFrameworkUnitMapper
{
    List<TaskFrameworkUnit> selectByFrameworkId(Long frameworkId);

    int batchInsert(List<TaskFrameworkUnit> units);

    int deleteByFrameworkId(Long frameworkId);

    int deleteByFrameworkIds(Long[] frameworkIds);
}
