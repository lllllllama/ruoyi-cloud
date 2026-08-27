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
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.research.domain.ResearchGroup;
import com.ruoyi.research.service.ResearchGroupService;

@RestController
@RequestMapping("/group")
public class ResearchGroupController extends BaseController
{
    @Autowired
    private ResearchGroupService groupService;

    @RequiresPermissions("research:group:list")
    @GetMapping("/list")
    public TableDataInfo list(ResearchGroup query)
    {
        startPage();
        List<ResearchGroup> list = groupService.selectList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("research:group:list")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable("id") Long id)
    {
        return AjaxResult.success(groupService.selectById(id));
    }

    @RequiresPermissions("research:group:add")
    @Log(title = "Research group", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ResearchGroup group)
    {
        return toAjax(groupService.insert(group));
    }

    @RequiresPermissions("research:group:edit")
    @Log(title = "Research group", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ResearchGroup group)
    {
        return toAjax(groupService.update(group));
    }

    @RequiresPermissions("research:group:edit")
    @Log(title = "Research group", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(groupService.deleteByIds(ids));
    }
}
