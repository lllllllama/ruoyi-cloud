package com.ruoyi.research.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.ResearchGroup;
import com.ruoyi.research.domain.ResearchGroupUnit;
import com.ruoyi.research.mapper.ResearchGroupMapper;
import com.ruoyi.research.mapper.ResearchGroupUnitMapper;
import com.ruoyi.research.service.ResearchGroupService;

@Service
public class ResearchGroupServiceImpl implements ResearchGroupService
{
    private static final String STATUS_ACTIVE = "0";
    private static final String UNIT_LEAD = "LEAD";
    private static final String UNIT_PARTICIPANT = "PARTICIPANT";

    @Autowired
    private ResearchGroupMapper groupMapper;

    @Autowired
    private ResearchGroupUnitMapper unitMapper;

    @Override
    public ResearchGroup selectById(Long groupId)
    {
        ResearchGroup group = groupMapper.selectResearchGroupById(groupId);
        if (group != null)
        {
            group.setUnits(unitMapper.selectByGroupId(groupId));
        }
        return group;
    }

    @Override
    public List<ResearchGroup> selectList(ResearchGroup query)
    {
        List<ResearchGroup> groups = groupMapper.selectResearchGroupList(query);
        for (ResearchGroup group : groups)
        {
            group.setUnits(unitMapper.selectByGroupId(group.getGroupId()));
        }
        return groups;
    }

    @Override
    @Transactional
    public int insert(ResearchGroup group)
    {
        normalizeDefaults(group);
        validateGroupCode(group);
        group.setCreateBy(SecurityUtils.getUsername());
        int rows = groupMapper.insertResearchGroup(group);
        saveUnits(group);
        return rows;
    }

    @Override
    @Transactional
    public int update(ResearchGroup group)
    {
        if (group.getGroupId() == null || groupMapper.selectResearchGroupById(group.getGroupId()) == null)
        {
            throw new ServiceException("Research group does not exist");
        }
        normalizeDefaults(group);
        validateGroupCode(group);
        group.setUpdateBy(SecurityUtils.getUsername());
        int rows = groupMapper.updateResearchGroup(group);
        unitMapper.deleteByGroupId(group.getGroupId());
        saveUnits(group);
        return rows;
    }

    @Override
    @Transactional
    public int deleteByIds(Long[] groupIds)
    {
        if (groupIds == null || groupIds.length == 0)
        {
            return 0;
        }
        unitMapper.deleteByGroupIds(groupIds);
        return groupMapper.deleteResearchGroups(groupIds);
    }

    private void validateGroupCode(ResearchGroup group)
    {
        if (groupMapper.countByGroupCode(group.getGroupCode(), group.getGroupId()) > 0)
        {
            throw new ServiceException("Research group code already exists");
        }
    }

    private void normalizeDefaults(ResearchGroup group)
    {
        group.setGroupCode(group.getGroupCode().trim());
        group.setGroupName(group.getGroupName().trim());
        if (StringUtils.isEmpty(group.getStatus()))
        {
            group.setStatus(STATUS_ACTIVE);
        }
        if (group.getSort() == null)
        {
            group.setSort(0);
        }
    }

    private void saveUnits(ResearchGroup group)
    {
        List<ResearchGroupUnit> normalized = normalizeUnits(group);
        if (!normalized.isEmpty())
        {
            unitMapper.batchInsert(normalized);
        }
        group.setUnits(normalized);
    }

    private List<ResearchGroupUnit> normalizeUnits(ResearchGroup group)
    {
        List<ResearchGroupUnit> normalized = new ArrayList<>();
        Set<Long> seenDeptIds = new HashSet<>();
        ResearchGroupUnit leadInput = null;

        if (group.getUnits() != null)
        {
            for (ResearchGroupUnit unit : group.getUnits())
            {
                if (unit == null || unit.getDeptId() == null)
                {
                    continue;
                }
                if (group.getLeadDeptId().equals(unit.getDeptId()))
                {
                    leadInput = unit;
                    continue;
                }
                if (UNIT_LEAD.equals(unit.getUnitType()))
                {
                    throw new ServiceException("Only leadDeptId may use unit type LEAD");
                }
                addUnit(normalized, seenDeptIds, group, unit, UNIT_PARTICIPANT);
            }
        }

        ResearchGroupUnit lead = leadInput == null ? new ResearchGroupUnit() : leadInput;
        lead.setDeptId(group.getLeadDeptId());
        addUnit(normalized, seenDeptIds, group, lead, UNIT_LEAD);
        return normalized;
    }

    private void addUnit(List<ResearchGroupUnit> target, Set<Long> seenDeptIds, ResearchGroup group,
            ResearchGroupUnit source, String unitType)
    {
        if (!seenDeptIds.add(source.getDeptId()))
        {
            throw new ServiceException("A department may only appear once in a research group");
        }
        source.setId(null);
        source.setGroupId(group.getGroupId());
        source.setUnitType(unitType);
        source.setStatus(StringUtils.isEmpty(source.getStatus()) ? STATUS_ACTIVE : source.getStatus());
        source.setCreateBy(SecurityUtils.getUsername());
        target.add(source);
    }
}
