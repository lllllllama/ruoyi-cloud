package com.ruoyi.fund.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.fund.domain.FundOperationLog;
import com.ruoyi.fund.service.IFundOperationLogService;

@RestController
@RequestMapping("/fund/operation-log")
public class FundOperationLogController
{
    @Autowired
    private IFundOperationLogService operationLogService;

    @GetMapping("/list")
    public AjaxResult list(FundOperationLog query)
    {
        return AjaxResult.success(operationLogService.selectAuthorizedList(query));
    }
}
