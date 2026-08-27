package com.ruoyi.system.api.domain;

import java.io.Serializable;

public class FundUserOption implements Serializable
{
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Long deptId;
    private String userName;
    private String nickName;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
}
