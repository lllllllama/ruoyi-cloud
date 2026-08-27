package com.ruoyi.fund.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import com.ruoyi.fund.domain.FundAttachment;

public interface IFundAttachmentService
{
    FundAttachment selectById(Long attachmentId);

    List<FundAttachment> selectByBusiness(String businessType, Long businessId);

    void sync(Long groupId, String businessType, Long businessId, String voucherUrls);

    void deleteByBusiness(String businessType, Long businessId);

    ResponseEntity<byte[]> download(Long attachmentId);
}
