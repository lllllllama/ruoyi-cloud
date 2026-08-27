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
        return isSystemAdmin(SecurityUtils.getUserId());
    }

    public static boolean isSystemAdmin(Long userId)
    {
        if (SecurityUtils.isAdmin(userId))
        {
            return true;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return userId != null && userId.equals(SecurityUtils.getUserId()) && loginUser != null
                && loginUser.getRoles() != null && loginUser.getRoles().contains("admin");
    }
}
