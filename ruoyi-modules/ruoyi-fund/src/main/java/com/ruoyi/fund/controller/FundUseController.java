package com.ruoyi.fund.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.fund.domain.FundUsePlan;
import com.ruoyi.fund.domain.FundUseRecord;
import com.ruoyi.fund.domain.dto.FundFinishRequest;
import com.ruoyi.fund.service.IFundBudgetService;
import com.ruoyi.fund.service.IFundUseService;

@RestController
@RequestMapping("/use")
public class FundUseController extends BaseController
{
    @Autowired private IFundUseService service;
    @Autowired private IFundBudgetService budgetService;

    @GetMapping("/overview/{groupId}")
    public AjaxResult overview(@PathVariable Long groupId) { return AjaxResult.success(budgetService.useOverview(groupId)); }

    @RequiresPermissions("fund:use:list")
    @GetMapping("/plan/list")
    public TableDataInfo list(FundUsePlan query)
    {
        startPage();
        List<FundUsePlan> list = service.selectPlanList(query);
        return getDataTable(list);
    }

    @GetMapping("/plan/{id}")
    public AjaxResult get(@PathVariable Long id) { return AjaxResult.success(service.selectPlan(id)); }

    @RequiresPermissions("fund:use:add")
    @Log(title = "资金使用计划", businessType = BusinessType.INSERT)
    @PostMapping("/plan")
    public AjaxResult add(@Validated @RequestBody FundUsePlan plan) { return toAjax(service.insertPlan(plan)); }

    @RequiresPermissions("fund:use:edit")
    @Log(title = "资金使用计划", businessType = BusinessType.UPDATE)
    @PutMapping("/plan")
    public AjaxResult edit(@Validated @RequestBody FundUsePlan plan) { return toAjax(service.updatePlan(plan)); }

    @RequiresPermissions("fund:use:remove")
    @Log(title = "资金使用计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/plan/{id}")
    public AjaxResult delete(@PathVariable Long id) { return toAjax(service.deletePlan(id)); }

    @GetMapping("/plan/{id}/records")
    public AjaxResult records(@PathVariable Long id) { return AjaxResult.success(service.selectRecords(id)); }

    @RequiresPermissions("fund:use:record")
    @Log(title = "资金使用记录", businessType = BusinessType.INSERT)
    @PostMapping("/record")
    public AjaxResult addRecord(@Validated @RequestBody FundUseRecord record) { return toAjax(service.insertRecord(record)); }

    @RequiresPermissions("fund:use:record")
    @Log(title = "资金使用记录", businessType = BusinessType.UPDATE)
    @PutMapping("/record")
    public AjaxResult editRecord(@Validated @RequestBody FundUseRecord record) { return toAjax(service.updateRecord(record)); }

    @RequiresPermissions("fund:use:record")
    @Log(title = "资金使用记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/record/{id}")
    public AjaxResult deleteRecord(@PathVariable Long id) { return toAjax(service.deleteRecord(id)); }

    @RequiresPermissions("fund:use:finish")
    @GetMapping("/plan/{id}/finish-check")
    public AjaxResult finishCheck(@PathVariable Long id) { return AjaxResult.success(service.finishCheck(id)); }

    @RequiresPermissions("fund:use:finish")
    @PutMapping("/plan/{id}/finish")
    public AjaxResult finish(@PathVariable Long id, @RequestBody FundFinishRequest request)
    {
        service.finish(id, request);
        return AjaxResult.success();
    }

    @RequiresPermissions("fund:use:finish")
    @PutMapping("/plan/{id}/force-finish/confirm")
    public AjaxResult confirmForceFinish(@PathVariable Long id)
    {
        service.confirmForceFinish(id);
        return AjaxResult.success();
    }
}
