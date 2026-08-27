package com.ruoyi.fund.util;

import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.model.LoginUser;

public class FundSecurityUtils
{
    public static boolean isSystemAdmin()
    {
        if (SecurityUtils.isAdmin(SecurityUtils.getUserId())) return true;
        LoginUser user = SecurityUtils.getLoginUser();
        return user != null && user.getRoles() != null && user.getRoles().contains("admin");
    }

    private FundSecurityUtils() { }
}
