package com.ruoyi.fund.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.fund.service.IFundAttachmentService;

@RestController
@RequestMapping("/fund/attachment")
public class FundAttachmentController
{
    @Autowired
    private IFundAttachmentService attachmentService;

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id)
    {
        return attachmentService.download(id);
    }
}
