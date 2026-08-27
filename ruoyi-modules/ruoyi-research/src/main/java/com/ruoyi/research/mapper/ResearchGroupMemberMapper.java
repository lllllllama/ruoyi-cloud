package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.research.domain.ResearchGroupMember;

public interface ResearchGroupMemberMapper
{
    ResearchGroupMember selectById(Long id);

    List<ResearchGroupMember> selectByGroupId(Long groupId);

    int countSameRole(@Param("groupId") Long groupId, @Param("userId") Long userId,
            @Param("memberRole") String memberRole, @Param("excludeId") Long excludeId);

    int insert(ResearchGroupMember member);

    int update(ResearchGroupMember member);

    int deleteByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
