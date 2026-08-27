package com.ruoyi.system.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.InnerAuth;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;
import com.ruoyi.system.api.domain.SysDept;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysUserMapper;

/** 资金微服务所需的组织、用户内部只读接口。 */
@RestController
@RequestMapping("/fund-support")
public class SysFundSupportController
{
    @Autowired private SysDeptMapper deptMapper;
    @Autowired private SysUserMapper userMapper;

    @InnerAuth
    @GetMapping("/depts")
    public R<List<FundDeptOption>> depts()
    {
        List<SysDept> source = deptMapper.selectDeptList(new SysDept());
        List<FundDeptOption> result = new ArrayList<>();
        for (SysDept dept : source) result.add(toDept(dept));
        return R.ok(result);
    }

    @InnerAuth
    @GetMapping("/dept/{deptId}")
    public R<FundDeptOption> dept(@PathVariable Long deptId)
    {
        SysDept dept = deptMapper.selectDeptById(deptId);
        return dept == null ? R.fail("部门不存在") : R.ok(toDept(dept));
    }

    @InnerAuth
    @GetMapping("/dept/{deptId}/users")
    public R<List<FundUserOption>> users(@PathVariable Long deptId)
    {
        SysUser query = new SysUser();
        query.setDeptId(deptId);
        List<SysUser> source = userMapper.selectUserList(query);
        List<FundUserOption> result = new ArrayList<>();
        for (SysUser user : source)
        {
            // SysUserMapper.selectUserList 会包含子部门用户；资金责任人必须严格属于当前拨付单位。
            if (deptId.equals(user.getDeptId()))
            {
                result.add(toUser(user));
            }
        }
        return R.ok(result);
    }

    @InnerAuth
    @GetMapping("/user/{userId}")
    public R<FundUserOption> user(@PathVariable Long userId)
    {
        SysUser user = userMapper.selectUserById(userId);
        return user == null ? R.fail("用户不存在") : R.ok(toUser(user));
    }

    private FundDeptOption toDept(SysDept dept)
    {
        FundDeptOption dto = new FundDeptOption();
        dto.setDeptId(dept.getDeptId()); dto.setDeptName(dept.getDeptName());
        dto.setLeader(dept.getLeader()); dto.setParentId(dept.getParentId());
        return dto;
    }

    private FundUserOption toUser(SysUser user)
    {
        FundUserOption dto = new FundUserOption();
        dto.setUserId(user.getUserId()); dto.setDeptId(user.getDeptId());
        dto.setUserName(user.getUserName()); dto.setNickName(user.getNickName());
        return dto;
    }
}
