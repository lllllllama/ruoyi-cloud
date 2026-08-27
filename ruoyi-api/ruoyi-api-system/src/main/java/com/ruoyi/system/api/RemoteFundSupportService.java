package com.ruoyi.system.api;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;

@FeignClient(contextId = "remoteFundSupportService", value = ServiceNameConstants.SYSTEM_SERVICE)
public interface RemoteFundSupportService
{
    @GetMapping("/fund-support/depts")
    R<List<FundDeptOption>> getDepts(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/fund-support/dept/{deptId}")
    R<FundDeptOption> getDept(@PathVariable("deptId") Long deptId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/fund-support/dept/{deptId}/users")
    R<List<FundUserOption>> getUsersByDept(@PathVariable("deptId") Long deptId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/fund-support/user/{userId}")
    R<FundUserOption> getUser(@PathVariable("userId") Long userId,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
