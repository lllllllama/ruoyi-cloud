package com.ruoyi.research.controller;

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
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.dto.TaskDeliverableAssignRequest;
import com.ruoyi.research.service.TaskDeliverableService;
import com.ruoyi.research.service.TaskDeliverableUserService;
import com.ruoyi.research.service.TaskPermissionService;

@RestController
@RequestMapping("/deliverable")
public class TaskDeliverableController extends BaseController
{
    @Autowired
    private TaskDeliverableService deliverableService;

    @Autowired
    private TaskDeliverableUserService deliverableUserService;

    @Autowired
    private TaskPermissionService taskPermissionService;

    @RequiresPermissions("task:info:list")
    @GetMapping("/list")
    public AjaxResult list(TaskDeliverable query)
    {
        List<TaskDeliverable> list = deliverableService.selectList(query);
        return AjaxResult.success(list);
    }

    @RequiresPermissions("task:info:list")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable("id") Long id)
    {
        return AjaxResult.success(deliverableService.selectById(id));
    }

    @RequiresPermissions("task:info:list")
    @GetMapping("/{id}/assignees")
    public AjaxResult assignees(@PathVariable("id") Long id)
    {
        return AjaxResult.success(deliverableUserService.selectByDeliverableId(id));
    }

    @RequiresPermissions("task:info:list")
    @GetMapping("/{id}/can-submit")
    public AjaxResult canSubmit(@PathVariable("id") Long id)
    {
        return AjaxResult.success(taskPermissionService.canCreateSubmission(id, SecurityUtils.getUserId()));
    }

    @RequiresPermissions("task:deliverable:assign")
    @Log(title = "Deliverable assignees", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/assignees")
    public AjaxResult assign(@PathVariable("id") Long id,
            @Validated @RequestBody TaskDeliverableAssignRequest request)
    {
        return toAjax(deliverableUserService.assign(id, request.getUserIds()));
    }

    @RequiresPermissions("task:deliverable:add")
    @Log(title = "Task deliverable", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TaskDeliverable deliverable)
    {
        return deliverableService.insert(deliverable) > 0
                ? AjaxResult.success(deliverable) : AjaxResult.error();
    }

    @RequiresPermissions("task:deliverable:add")
    @Log(title = "Task deliverable", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TaskDeliverable deliverable)
    {
        return toAjax(deliverableService.update(deliverable));
    }

    @RequiresPermissions("task:deliverable:add")
    @Log(title = "Task deliverable", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") Long id)
    {
        return toAjax(deliverableService.delete(id));
    }
}
