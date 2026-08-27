package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.ResearchGroup;

public interface ResearchGroupService
{
    ResearchGroup selectById(Long groupId);

    List<ResearchGroup> selectList(ResearchGroup query);

    int insert(ResearchGroup group);

    int update(ResearchGroup group);

    int deleteByIds(Long[] groupIds);
}
