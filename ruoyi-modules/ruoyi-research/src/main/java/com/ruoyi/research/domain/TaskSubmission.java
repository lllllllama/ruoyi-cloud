package com.ruoyi.research.domain;

import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class TaskSubmission extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long groupId;
    private Long frameworkId;
    private Long taskId;

    @NotNull(message = "Deliverable is required")
    private Long deliverableId;

    @NotBlank(message = "Submission name is required")
    @Size(max = 200, message = "Submission name must not exceed 200 characters")
    private String submissionName;

    @Size(max = 2000, message = "Submission description must not exceed 2000 characters")
    private String submissionDesc;

    private Long submitUserId;
    private Long submitDeptId;
    private Date submitTime;
    private String status;
    private Long archiveUserId;
    private Date archiveTime;
    private Integer version;
    private String deliverableName;
    private String taskName;
    private String groupName;

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getFrameworkId() { return frameworkId; }
    public void setFrameworkId(Long frameworkId) { this.frameworkId = frameworkId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getDeliverableId() { return deliverableId; }
    public void setDeliverableId(Long deliverableId) { this.deliverableId = deliverableId; }
    public String getSubmissionName() { return submissionName; }
    public void setSubmissionName(String submissionName) { this.submissionName = submissionName; }
    public String getSubmissionDesc() { return submissionDesc; }
    public void setSubmissionDesc(String submissionDesc) { this.submissionDesc = submissionDesc; }
    public Long getSubmitUserId() { return submitUserId; }
    public void setSubmitUserId(Long submitUserId) { this.submitUserId = submitUserId; }
    public Long getSubmitDeptId() { return submitDeptId; }
    public void setSubmitDeptId(Long submitDeptId) { this.submitDeptId = submitDeptId; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date submitTime) { this.submitTime = submitTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getArchiveUserId() { return archiveUserId; }
    public void setArchiveUserId(Long archiveUserId) { this.archiveUserId = archiveUserId; }
    public Date getArchiveTime() { return archiveTime; }
    public void setArchiveTime(Date archiveTime) { this.archiveTime = archiveTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDeliverableName() { return deliverableName; }
    public void setDeliverableName(String deliverableName) { this.deliverableName = deliverableName; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
