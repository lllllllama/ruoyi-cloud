package com.ruoyi.fund.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.fund.domain.FundAttachment;
import com.ruoyi.fund.domain.vo.FundUploadReceiptVo;

public interface IFundAttachmentService
{
    FundAttachment selectById(Long attachmentId);

    List<FundAttachment> selectByBusiness(String businessType, Long businessId);

    FundUploadReceiptVo upload(MultipartFile file);

    void consume(Long groupId, String businessType, Long businessId, String attachmentTokens);

    void deleteByBusiness(String businessType, Long businessId);

    ResponseEntity<byte[]> download(Long attachmentId);
}
