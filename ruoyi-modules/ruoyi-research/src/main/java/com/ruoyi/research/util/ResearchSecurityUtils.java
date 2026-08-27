package com.ruoyi.research.util;

import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.model.LoginUser;

public final class ResearchSecurityUtils
{
    private ResearchSecurityUtils()
    {
    }

    public static boolean isSystemAdmin()
    {
        if (SecurityUtils.isAdmin(SecurityUtils.getUserId()))
        {
            return true;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser != null && loginUser.getRoles() != null && loginUser.getRoles().contains("admin");
    }
}
