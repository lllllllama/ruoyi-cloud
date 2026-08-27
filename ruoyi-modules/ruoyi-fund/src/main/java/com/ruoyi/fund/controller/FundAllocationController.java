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
import com.ruoyi.fund.domain.FundAllocationPlan;
import com.ruoyi.fund.domain.FundAllocationRecord;
import com.ruoyi.fund.domain.dto.FundAssignRequest;
import com.ruoyi.fund.domain.dto.FundFinishRequest;
import com.ruoyi.fund.service.IFundAllocationService;
import com.ruoyi.fund.service.IFundBudgetService;

@RestController
@RequestMapping("/allocation")
public class FundAllocationController extends BaseController
{
    @Autowired private IFundAllocationService service;
    @Autowired private IFundBudgetService budgetService;

    @GetMapping("/overview/{groupId}")
    public AjaxResult overview(@PathVariable Long groupId) { return AjaxResult.success(budgetService.allocationOverview(groupId)); }

    @GetMapping("/plan/list")
    public TableDataInfo list(FundAllocationPlan query)
    {
        startPage();
        List<FundAllocationPlan> list = service.selectPlanList(query);
        return getDataTable(list);
    }

    @GetMapping("/plan/{id}")
    public AjaxResult get(@PathVariable Long id) { return AjaxResult.success(service.selectPlan(id)); }

    @RequiresPermissions("fund:allocation:add")
    @Log(title = "资金拨付计划", businessType = BusinessType.INSERT)
    @PostMapping("/plan")
    public AjaxResult add(@Validated @RequestBody FundAllocationPlan plan) { return toAjax(service.insertPlan(plan)); }

    @RequiresPermissions("fund:allocation:edit")
    @Log(title = "资金拨付计划", businessType = BusinessType.UPDATE)
    @PutMapping("/plan")
    public AjaxResult edit(@Validated @RequestBody FundAllocationPlan plan) { return toAjax(service.updatePlan(plan)); }

    @RequiresPermissions("fund:allocation:remove")
    @Log(title = "资金拨付计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/plan/{id}")
    public AjaxResult delete(@PathVariable Long id) { return toAjax(service.deletePlan(id)); }

    @RequiresPermissions("fund:allocation:assign")
    @PutMapping("/plan/{id}/assign")
    public AjaxResult assign(@PathVariable Long id, @RequestBody FundAssignRequest request)
    {
        return toAjax(service.assign(id, request.getResponsibleUserId()));
    }

    @GetMapping("/plan/{id}/records")
    public AjaxResult records(@PathVariable Long id) { return AjaxResult.success(service.selectRecords(id)); }

    @RequiresPermissions("fund:allocation:record")
    @Log(title = "资金拨付记录", businessType = BusinessType.INSERT)
    @PostMapping("/record")
    public AjaxResult addRecord(@Validated @RequestBody FundAllocationRecord record) { return toAjax(service.insertRecord(record)); }

    @RequiresPermissions("fund:allocation:record")
    @Log(title = "资金拨付记录", businessType = BusinessType.UPDATE)
    @PutMapping("/record")
    public AjaxResult editRecord(@Validated @RequestBody FundAllocationRecord record) { return toAjax(service.updateRecord(record)); }

    @RequiresPermissions("fund:allocation:record")
    @Log(title = "资金拨付记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/record/{id}")
    public AjaxResult deleteRecord(@PathVariable Long id) { return toAjax(service.deleteRecord(id)); }

    @RequiresPermissions("fund:allocation:finish")
    @GetMapping("/plan/{id}/finish-check")
    public AjaxResult finishCheck(@PathVariable Long id) { return AjaxResult.success(service.finishCheck(id)); }

    @RequiresPermissions("fund:allocation:finish")
    @PutMapping("/plan/{id}/finish")
    public AjaxResult finish(@PathVariable Long id, @RequestBody FundFinishRequest request)
    {
        service.finish(id, request);
        return AjaxResult.success();
    }
}
