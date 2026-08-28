package com.ruoyi.fund.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.Logical;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.fund.domain.vo.FundDeptOptionVo;
import com.ruoyi.fund.service.IFundOrgService;
import com.ruoyi.system.api.domain.FundDeptOption;

@RestController
@RequestMapping("/org")
public class FundOrgController
{
    @Autowired
    private IFundOrgService service;

    @RequiresPermissions(value = { "fund:allocation:add", "fund:allocation:edit" }, logical = Logical.OR)
    @GetMapping("/depts")
    public AjaxResult depts()
    {
        List<FundDeptOptionVo> options = new ArrayList<>();
        for (FundDeptOption dept : service.getDepts())
        {
            FundDeptOptionVo option = new FundDeptOptionVo();
            option.setDeptId(dept.getDeptId());
            option.setDeptName(dept.getDeptName());
            options.add(option);
        }
        return AjaxResult.success(options);
    }
}
