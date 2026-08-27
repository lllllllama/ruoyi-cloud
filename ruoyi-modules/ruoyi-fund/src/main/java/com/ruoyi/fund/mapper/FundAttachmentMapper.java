package com.ruoyi.fund.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.fund.domain.FundAttachment;

public interface FundAttachmentMapper
{
    FundAttachment selectById(Long attachmentId);

    List<FundAttachment> selectByBusiness(@Param("businessType") String businessType,
            @Param("businessId") Long businessId);

    int insert(FundAttachment attachment);

    int deleteByBusiness(@Param("businessType") String businessType,
            @Param("businessId") Long businessId);
}
