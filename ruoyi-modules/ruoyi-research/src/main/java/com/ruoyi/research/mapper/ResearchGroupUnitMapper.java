package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.ResearchGroupUnit;

public interface ResearchGroupUnitMapper
{
    List<ResearchGroupUnit> selectByGroupId(Long groupId);

    int countActiveByGroupAndDept(@Param("groupId") Long groupId, @Param("deptId") Long deptId);

    int batchInsert(List<ResearchGroupUnit> units);

    int deleteByGroupId(Long groupId);

    int deleteByGroupIds(Long[] groupIds);
}
