package com.ruoyi.research.api.factory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.research.api.RemoteResearchService;
import com.ruoyi.research.api.domain.ResearchGroupDto;
import com.ruoyi.research.api.domain.ResearchGroupMemberDto;
import com.ruoyi.research.api.domain.ResearchUserPermissionDto;

@Component
public class RemoteResearchFallbackFactory implements FallbackFactory<RemoteResearchService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteResearchFallbackFactory.class);

    @Override
    public RemoteResearchService create(final Throwable throwable)
    {
        log.error("Research service call failed: {}", throwable.getMessage());
        return new RemoteResearchService()
        {
            private <T> R<T> failed()
            {
                return R.fail("Research service call failed: " + throwable.getMessage());
            }

            @Override
            public R<ResearchGroupDto> getGroup(Long groupId, String source) { return failed(); }
            @Override
            public R<ResearchUserPermissionDto> getUserPermission(Long groupId, Long userId, String source) { return failed(); }
            @Override
            public R<Boolean> isGroupMember(Long groupId, Long userId, String source) { return failed(); }
            @Override
            public R<Boolean> isGroupLeader(Long groupId, Long userId, String source) { return failed(); }
            @Override
            public R<Boolean> isUnitManager(Long groupId, Long deptId, Long userId, String source) { return failed(); }
            @Override
            public R<List<Long>> getAllowedGroupIds(Long userId, String source) { return failed(); }
            @Override
            public R<List<ResearchGroupMemberDto>> getSelectableMembers(Long groupId, Long deptId, String source) { return failed(); }
        };
    }
}
