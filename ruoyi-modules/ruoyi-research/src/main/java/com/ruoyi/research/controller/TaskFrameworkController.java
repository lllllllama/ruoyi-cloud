package com.ruoyi.research.controller;

import java.util.ArrayList;
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
import com.ruoyi.research.domain.TaskFramework;
import com.ruoyi.research.domain.vo.TaskFrameworkOptionVo;
import com.ruoyi.research.service.TaskFrameworkService;

@RestController
@RequestMapping("/framework")
public class TaskFrameworkController extends BaseController
{
    @Autowired
    private TaskFrameworkService frameworkService;

    @RequiresPermissions("task:info:list")
    @GetMapping("/options")
    public AjaxResult options()
    {
        TaskFramework query = new TaskFramework();
        query.setStatus("0");
        List<TaskFrameworkOptionVo> options = new ArrayList<>();
        for (TaskFramework framework : frameworkService.selectList(query))
        {
            TaskFrameworkOptionVo option = new TaskFrameworkOptionVo();
            option.setFrameworkId(framework.getFrameworkId());
            option.setGroupId(framework.getGroupId());
            option.setFrameworkName(framework.getFrameworkName());
            option.setYear(framework.getYear());
            option.setGroupName(framework.getGroupName());
            options.add(option);
        }
        return AjaxResult.success(options);
    }

    @RequiresPermissions("task:framework:list")
    @GetMapping("/list")
    public TableDataInfo list(TaskFramework query)
    {
        startPage();
        List<TaskFramework> list = frameworkService.selectList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("task:framework:list")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable("id") Long id)
    {
        return AjaxResult.success(frameworkService.selectById(id));
    }

    @RequiresPermissions("task:framework:add")
    @Log(title = "Annual task framework", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TaskFramework framework)
    {
        return toAjax(frameworkService.insert(framework));
    }

    @RequiresPermissions("task:framework:add")
    @Log(title = "Annual task framework", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TaskFramework framework)
    {
        return toAjax(frameworkService.update(framework));
    }

    @RequiresPermissions("task:framework:add")
    @Log(title = "Annual task framework", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(frameworkService.deleteByIds(ids));
    }
}
