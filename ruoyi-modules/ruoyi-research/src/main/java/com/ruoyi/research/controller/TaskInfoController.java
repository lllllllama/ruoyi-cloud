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
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.service.TaskInfoService;

@RestController
@RequestMapping("/task")
public class TaskInfoController extends BaseController
{
    @Autowired
    private TaskInfoService taskService;

    @RequiresPermissions("task:info:list")
    @GetMapping("/list")
    public AjaxResult list(TaskInfo query)
    {
        List<TaskInfo> list = taskService.selectList(query);
        return AjaxResult.success(list);
    }

    @RequiresPermissions("task:info:list")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable("id") Long id)
    {
        return AjaxResult.success(taskService.selectById(id));
    }

    @RequiresPermissions("task:info:add")
    @Log(title = "Research task", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TaskInfo task)
    {
        return toAjax(taskService.insert(task));
    }

    @RequiresPermissions("task:info:edit")
    @Log(title = "Research task", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TaskInfo task)
    {
        return toAjax(taskService.update(task));
    }

    @RequiresPermissions("task:info:remove")
    @Log(title = "Research task", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(taskService.deleteByIds(ids));
    }
}
