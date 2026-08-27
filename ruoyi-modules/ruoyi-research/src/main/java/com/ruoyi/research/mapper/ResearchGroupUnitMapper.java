package com.ruoyi.research.mapper;

import java.util.List;
import com.ruoyi.research.domain.ResearchGroupUnit;

public interface ResearchGroupUnitMapper
{
    List<ResearchGroupUnit> selectByGroupId(Long groupId);

    int batchInsert(List<ResearchGroupUnit> units);

    int deleteByGroupId(Long groupId);

    int deleteByGroupIds(Long[] groupIds);
}
