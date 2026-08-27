package com.ruoyi.research.domain;

import java.io.Serializable;
import java.util.Date;

public class TaskDeliverableUser implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;
    private Long deliverableId;
    private Long userId;
    private Long assignUserId;
    private Date assignTime;
    private String userName;
    private String nickName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getDeliverableId() { return deliverableId; }
    public void setDeliverableId(Long deliverableId) { this.deliverableId = deliverableId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAssignUserId() { return assignUserId; }
    public void setAssignUserId(Long assignUserId) { this.assignUserId = assignUserId; }
    public Date getAssignTime() { return assignTime; }
    public void setAssignTime(Date assignTime) { this.assignTime = assignTime; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
}
