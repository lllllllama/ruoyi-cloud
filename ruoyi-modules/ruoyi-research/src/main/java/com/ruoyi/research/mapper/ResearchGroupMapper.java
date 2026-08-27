package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.ResearchGroup;

public interface ResearchGroupMapper
{
    ResearchGroup selectResearchGroupById(Long groupId);

    List<ResearchGroup> selectResearchGroupList(ResearchGroup group);

    int countByGroupCode(@Param("groupCode") String groupCode, @Param("excludeGroupId") Long excludeGroupId);

    int insertResearchGroup(ResearchGroup group);

    int updateResearchGroup(ResearchGroup group);

    int deleteResearchGroups(Long[] groupIds);
}
