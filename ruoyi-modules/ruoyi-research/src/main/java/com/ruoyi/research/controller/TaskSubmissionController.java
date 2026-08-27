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
import org.springframework.http.ResponseEntity;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.domain.TaskAttachment;
import com.ruoyi.research.service.TaskAttachmentService;
import com.ruoyi.research.service.TaskSubmissionService;

@RestController
@RequestMapping("/submission")
public class TaskSubmissionController extends BaseController
{
    @Autowired
    private TaskSubmissionService submissionService;

    @Autowired
    private TaskAttachmentService attachmentService;

    @RequiresPermissions("task:submission:add")
    @GetMapping("/list")
    public TableDataInfo list(TaskSubmission query)
    {
        startPage();
        List<TaskSubmission> list = submissionService.selectList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("task:submission:add")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable("id") Long id)
    {
        return AjaxResult.success(submissionService.selectById(id));
    }

    @RequiresPermissions("task:submission:add")
    @GetMapping("/{id}/attachments")
    public AjaxResult attachments(@PathVariable("id") Long id)
    {
        return AjaxResult.success(attachmentService.selectBySubmissionId(id));
    }

    @RequiresPermissions("task:submission:edit")
    @PostMapping("/{id}/attachments")
    public AjaxResult addAttachment(@PathVariable("id") Long id,
            @Validated @RequestBody TaskAttachment attachment)
    {
        return toAjax(attachmentService.insert(id, attachment));
    }

    @RequiresPermissions("task:submission:edit")
    @DeleteMapping("/attachment/{attachmentId}")
    public AjaxResult removeAttachment(@PathVariable Long attachmentId)
    {
        return toAjax(attachmentService.delete(attachmentId));
    }

    @RequiresPermissions("task:submission:add")
    @GetMapping("/attachment/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId)
    {
        return attachmentService.download(attachmentId);
    }

    @RequiresPermissions("task:submission:add")
    @Log(title = "Deliverable submission", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TaskSubmission submission)
    {
        return toAjax(submissionService.insertDraft(submission));
    }

    @RequiresPermissions("task:submission:edit")
    @Log(title = "Deliverable submission", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TaskSubmission submission)
    {
        return toAjax(submissionService.updateDraft(submission));
    }

    @RequiresPermissions("task:submission:edit")
    @Log(title = "Deliverable submission", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") Long id)
    {
        return toAjax(submissionService.deleteDraft(id));
    }
}
