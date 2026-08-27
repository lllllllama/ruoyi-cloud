package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.TaskInfo;

public interface TaskInfoMapper
{
    TaskInfo selectById(Long taskId);

    List<TaskInfo> selectChildren(Long parentId);

    List<TaskInfo> selectByFrameworkId(Long frameworkId);

    List<TaskInfo> selectList(@Param("query") TaskInfo query,
            @Param("allowedGroupIds") List<Long> allowedGroupIds);

    int insert(TaskInfo task);

    int update(TaskInfo task);

    int updateLevel(@Param("taskId") Long taskId, @Param("level") Integer level,
            @Param("updateBy") String updateBy);

    int countActiveChildren(Long taskId);

    int countSubmissions(Long taskId);

    int countActiveDeliverables(Long taskId);

    int deleteByIds(@Param("taskIds") Long[] taskIds, @Param("updateBy") String updateBy);
}
