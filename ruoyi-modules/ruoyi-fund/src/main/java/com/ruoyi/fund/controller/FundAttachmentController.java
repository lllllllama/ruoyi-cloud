package com.ruoyi.fund.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.Logical;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.fund.service.IFundAttachmentService;

@RestController
@RequestMapping("/fund/attachment")
public class FundAttachmentController
{
    @Autowired
    private IFundAttachmentService attachmentService;

    @RequiresPermissions(value = { "fund:allocation:record", "fund:use:record" }, logical = Logical.OR)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file)
    {
        return AjaxResult.success(attachmentService.upload(file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id)
    {
        return attachmentService.download(id);
    }
}
