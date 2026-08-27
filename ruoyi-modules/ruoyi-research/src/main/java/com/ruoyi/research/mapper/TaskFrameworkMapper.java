package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.TaskFramework;

public interface TaskFrameworkMapper
{
    TaskFramework selectById(Long frameworkId);

    List<TaskFramework> selectList(@Param("query") TaskFramework query,
            @Param("allowedGroupIds") List<Long> allowedGroupIds);

    int insert(TaskFramework framework);

    int update(TaskFramework framework);

    int deleteByIds(@Param("frameworkIds") Long[] frameworkIds,
            @Param("updateBy") String updateBy);
}
