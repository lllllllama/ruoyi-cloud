package com.ruoyi.research.domain.vo;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class MyTaskVo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String taskName;
    private Long deliverableId;
    private String deliverableName;
    private Long groupId;
    private String groupName;
    private Integer requiredNum;
    private Integer archivedNum;
    private Integer pendingNum;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date deadline;

    private String status;
    private String timeStatus;
    private Boolean canSubmit;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public Long getDeliverableId() { return deliverableId; }
    public void setDeliverableId(Long deliverableId) { this.deliverableId = deliverableId; }
    public String getDeliverableName() { return deliverableName; }
    public void setDeliverableName(String deliverableName) { this.deliverableName = deliverableName; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Integer getRequiredNum() { return requiredNum; }
    public void setRequiredNum(Integer requiredNum) { this.requiredNum = requiredNum; }
    public Integer getArchivedNum() { return archivedNum; }
    public void setArchivedNum(Integer archivedNum) { this.archivedNum = archivedNum; }
    public Integer getPendingNum() { return pendingNum; }
    public void setPendingNum(Integer pendingNum) { this.pendingNum = pendingNum; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTimeStatus() { return timeStatus; }
    public void setTimeStatus(String timeStatus) { this.timeStatus = timeStatus; }
    public Boolean getCanSubmit() { return canSubmit; }
    public void setCanSubmit(Boolean canSubmit) { this.canSubmit = canSubmit; }
}
