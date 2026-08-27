package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.TaskDeliverable;

public interface TaskDeliverableMapper
{
    TaskDeliverable selectById(Long deliverableId);

    TaskDeliverable selectForUpdate(Long deliverableId);

    List<TaskDeliverable> selectList(@Param("query") TaskDeliverable query,
            @Param("allowedGroupIds") List<Long> allowedGroupIds);

    int insert(TaskDeliverable deliverable);

    int update(TaskDeliverable deliverable);

    int countSubmissions(Long deliverableId);

    int countArchivedSubmissions(Long deliverableId);

    int updateArchiveProgress(@Param("deliverableId") Long deliverableId,
            @Param("archivedNum") int archivedNum,
            @Param("status") String status,
            @Param("updateBy") String updateBy);

    int deleteById(@Param("deliverableId") Long deliverableId,
            @Param("updateBy") String updateBy);
}
