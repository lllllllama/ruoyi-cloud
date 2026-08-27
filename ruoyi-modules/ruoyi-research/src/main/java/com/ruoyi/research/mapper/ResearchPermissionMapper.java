package com.ruoyi.research.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ResearchPermissionMapper
{
    int countActiveMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    int countActiveRole(@Param("groupId") Long groupId, @Param("userId") Long userId,
            @Param("memberRole") String memberRole);

    int countActiveUnitMember(@Param("groupId") Long groupId, @Param("deptId") Long deptId,
            @Param("userId") Long userId);

    int countActiveUnitManager(@Param("groupId") Long groupId, @Param("deptId") Long deptId,
            @Param("userId") Long userId);

    List<Long> selectAllowedGroupIds(Long userId);

    List<Long> selectAllActiveGroupIds();
}
