package com.ruoyi.research.api.domain;

import java.io.Serializable;
import java.util.Date;

public class ResearchGroupMemberDto implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;
    private Long userId;
    private String userName;
    private String nickName;
    private Long deptId;
    private String deptName;
    private String memberRole;
    private String status;
    private Date joinTime;
    private Date leaveTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getJoinTime() { return joinTime; }
    public void setJoinTime(Date joinTime) { this.joinTime = joinTime; }
    public Date getLeaveTime() { return leaveTime; }
    public void setLeaveTime(Date leaveTime) { this.leaveTime = leaveTime; }
}
