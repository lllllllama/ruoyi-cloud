package com.ruoyi.research.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.ResearchGroup;
import com.ruoyi.research.domain.ResearchGroupUnit;
import com.ruoyi.research.domain.TaskFramework;
import com.ruoyi.research.domain.TaskFrameworkUnit;
import com.ruoyi.research.domain.vo.TaskFrameworkGroupOptionVo;
import com.ruoyi.research.domain.vo.TaskFrameworkUnitOptionVo;
import com.ruoyi.research.mapper.ResearchGroupMapper;
import com.ruoyi.research.mapper.ResearchGroupUnitMapper;
import com.ruoyi.research.mapper.TaskFrameworkMapper;
import com.ruoyi.research.mapper.TaskFrameworkUnitMapper;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.TaskFrameworkService;
import com.ruoyi.research.service.TaskPermissionService;
import com.ruoyi.system.api.domain.FundDeptOption;

@Service
public class TaskFrameworkServiceImpl implements TaskFrameworkService
{
    private static final String STATUS_ACTIVE = "0";

    @Autowired
    private TaskFrameworkMapper frameworkMapper;

    @Autowired
    private TaskFrameworkUnitMapper unitMapper;

    @Autowired
    private ResearchGroupMapper groupMapper;

    @Autowired
    private ResearchGroupUnitMapper groupUnitMapper;

    @Autowired
    private TaskPermissionService permissionService;

    @Autowired
    private ResearchOrgService orgService;

    @Override
    public TaskFramework selectById(Long frameworkId)
    {
        TaskFramework framework = requireFramework(frameworkId);
        assertCanView(framework.getGroupId());
        framework.setUnits(unitMapper.selectByFrameworkId(frameworkId));
        return framework;
    }

    @Override
    public List<TaskFramework> selectList(TaskFramework query)
    {
        List<Long> managedGroupIds = permissionService.getManagedGroupIds(SecurityUtils.getUserId());
        if (managedGroupIds == null || managedGroupIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<TaskFramework> frameworks = frameworkMapper.selectList(query, managedGroupIds);
        for (TaskFramework framework : frameworks)
        {
            framework.setUnits(unitMapper.selectByFrameworkId(framework.getFrameworkId()));
        }
        return frameworks;
    }

    @Override
    public List<TaskFramework> selectOptions()
    {
        List<Long> allowedGroupIds = permissionService.getAllowedGroupIds(SecurityUtils.getUserId());
        if (allowedGroupIds == null || allowedGroupIds.isEmpty())
        {
            return new ArrayList<>();
        }
        TaskFramework query = new TaskFramework();
        query.setStatus(STATUS_ACTIVE);
        return frameworkMapper.selectList(query, allowedGroupIds);
    }

    @Override
    public List<TaskFrameworkGroupOptionVo> selectManagedGroupOptions()
    {
        List<Long> managedGroupIds = permissionService.getManagedGroupIds(SecurityUtils.getUserId());
        List<TaskFrameworkGroupOptionVo> options = new ArrayList<>();
        if (managedGroupIds == null || managedGroupIds.isEmpty())
        {
            return options;
        }

        Map<Long, String> deptNames = new HashMap<>();
        List<FundDeptOption> departments = orgService.getDepts();
        if (departments != null)
        {
            for (FundDeptOption department : departments)
            {
                if (department != null && department.getDeptId() != null)
                {
                    deptNames.put(department.getDeptId(), department.getDeptName());
                }
            }
        }

        for (Long groupId : managedGroupIds)
        {
            ResearchGroup group = groupMapper.selectResearchGroupById(groupId);
            if (group == null || !STATUS_ACTIVE.equals(group.getStatus()))
            {
                continue;
            }
            TaskFrameworkGroupOptionVo option = new TaskFrameworkGroupOptionVo();
            option.setGroupId(group.getGroupId());
            option.setGroupName(group.getGroupName());
            List<TaskFrameworkUnitOptionVo> unitOptions = new ArrayList<>();
            List<ResearchGroupUnit> units = groupUnitMapper.selectByGroupId(groupId);
            if (units != null)
            {
                for (ResearchGroupUnit unit : units)
                {
                    if (unit == null || !STATUS_ACTIVE.equals(unit.getStatus()))
                    {
                        continue;
                    }
                    TaskFrameworkUnitOptionVo unitOption = new TaskFrameworkUnitOptionVo();
                    unitOption.setDeptId(unit.getDeptId());
                    unitOption.setDeptName(deptNames.get(unit.getDeptId()));
                    unitOption.setUnitType(unit.getUnitType());
                    unitOptions.add(unitOption);
                }
            }
            option.setUnits(unitOptions);
            options.add(option);
        }
        return options;
    }

    @Override
    @Transactional
    public int insert(TaskFramework framework)
    {
        assertCanMaintainFramework();
        normalize(framework);
        validateGroupAndUnits(framework);
        framework.setCreateBy(SecurityUtils.getUsername());
        int rows = frameworkMapper.insert(framework);
        saveUnits(framework);
        return rows;
    }

    @Override
    @Transactional
    public int update(TaskFramework framework)
    {
        assertCanMaintainFramework();
        if (framework.getFrameworkId() == null)
        {
            throw new ServiceException("Framework ID is required");
        }
        TaskFramework old = requireFramework(framework.getFrameworkId());
        if (!old.getGroupId().equals(framework.getGroupId()))
        {
            throw new ServiceException("A framework cannot be moved to another research group");
        }
        normalize(framework);
        validateGroupAndUnits(framework);
        framework.setUpdateBy(SecurityUtils.getUsername());
        int rows = frameworkMapper.update(framework);
        unitMapper.deleteByFrameworkId(framework.getFrameworkId());
        saveUnits(framework);
        return rows;
    }

    @Override
    @Transactional
    public int deleteByIds(Long[] frameworkIds)
    {
        assertCanMaintainFramework();
        if (frameworkIds == null || frameworkIds.length == 0)
        {
            return 0;
        }
        for (Long frameworkId : frameworkIds)
        {
            requireFramework(frameworkId);
        }
        unitMapper.deleteByFrameworkIds(frameworkIds);
        return frameworkMapper.deleteByIds(frameworkIds, SecurityUtils.getUsername());
    }

    private void normalize(TaskFramework framework)
    {
        framework.setFrameworkName(framework.getFrameworkName().trim());
        if (StringUtils.isEmpty(framework.getStatus()))
        {
            framework.setStatus(STATUS_ACTIVE);
        }
        if (framework.getSort() == null)
        {
            framework.setSort(0);
        }
    }

    private void validateGroupAndUnits(TaskFramework framework)
    {
        ResearchGroup group = groupMapper.selectResearchGroupById(framework.getGroupId());
        if (group == null || !STATUS_ACTIVE.equals(group.getStatus()))
        {
            throw new ServiceException("Research group does not exist or is disabled");
        }
        validateGroupUnit(framework.getGroupId(), framework.getLeadDeptId(), "Lead department");
        orgService.getDept(framework.getLeadDeptId());

        Set<Long> deptIds = new HashSet<>();
        if (framework.getUnits() == null)
        {
            return;
        }
        for (TaskFrameworkUnit unit : framework.getUnits())
        {
            if (unit == null || unit.getDeptId() == null)
            {
                throw new ServiceException("Collaborating department is required");
            }
            if (framework.getLeadDeptId().equals(unit.getDeptId()))
            {
                throw new ServiceException("Lead department must not be repeated as a collaborating department");
            }
            if (!deptIds.add(unit.getDeptId()))
            {
                throw new ServiceException("A collaborating department may only appear once");
            }
            validateGroupUnit(framework.getGroupId(), unit.getDeptId(), "Collaborating department");
            unit.setDeptName(orgService.getDept(unit.getDeptId()).getDeptName());
        }
    }

    private void validateGroupUnit(Long groupId, Long deptId, String label)
    {
        if (groupUnitMapper.countActiveByGroupAndDept(groupId, deptId) == 0)
        {
            throw new ServiceException(label + " must be an active unit of the research group");
        }
    }

    private void saveUnits(TaskFramework framework)
    {
        if (framework.getUnits() == null || framework.getUnits().isEmpty())
        {
            return;
        }
        for (TaskFrameworkUnit unit : framework.getUnits())
        {
            unit.setId(null);
            unit.setFrameworkId(framework.getFrameworkId());
            unit.setGroupId(framework.getGroupId());
            unit.setCreateBy(SecurityUtils.getUsername());
        }
        unitMapper.batchInsert(framework.getUnits());
    }

    private TaskFramework requireFramework(Long frameworkId)
    {
        TaskFramework framework = frameworkMapper.selectById(frameworkId);
        if (framework == null)
        {
            throw new ServiceException("Annual task framework does not exist");
        }
        return framework;
    }

    private void assertCanView(Long groupId)
    {
        permissionService.assertCanViewFrameworkGroup(groupId, SecurityUtils.getUserId());
    }

    private void assertCanMaintainFramework()
    {
        permissionService.assertCanMaintainFramework(SecurityUtils.getUserId());
    }
}
