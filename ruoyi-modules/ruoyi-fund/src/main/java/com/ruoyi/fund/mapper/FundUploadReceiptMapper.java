package com.ruoyi.fund.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.fund.domain.FundUploadReceipt;

public interface FundUploadReceiptMapper
{
    int insert(FundUploadReceipt receipt);

    FundUploadReceipt selectForUpdate(String uploadToken);

    int markUsed(@Param("uploadToken") String uploadToken,
            @Param("businessType") String businessType,
            @Param("businessId") Long businessId);
}
