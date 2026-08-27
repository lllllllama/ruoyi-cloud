package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.TaskInfo;

public interface TaskInfoMapper
{
    TaskInfo selectById(Long taskId);

    List<TaskInfo> selectList(@Param("query") TaskInfo query,
            @Param("allowedGroupIds") List<Long> allowedGroupIds);

    int insert(TaskInfo task);

    int update(TaskInfo task);

    int deleteByIds(@Param("taskIds") Long[] taskIds, @Param("updateBy") String updateBy);
}
