package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.ResearchGroupMember;

public interface ResearchGroupMemberService
{
    List<ResearchGroupMember> selectByGroupId(Long groupId);

    int insert(Long groupId, ResearchGroupMember member);

    int update(Long groupId, ResearchGroupMember member);

    int delete(Long groupId, Long userId);
}
