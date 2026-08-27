package com.ruoyi.research.domain;

import java.util.Date;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class TaskDeliverable extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long deliverableId;

    @NotNull(message = "Research group is required")
    private Long groupId;

    @NotNull(message = "Task is required")
    private Long taskId;

    @NotBlank(message = "Deliverable name is required")
    @Size(max = 200, message = "Deliverable name must not exceed 200 characters")
    private String deliverableName;

    @Size(max = 2000, message = "Requirement must not exceed 2000 characters")
    private String requirement;

    @NotNull(message = "Required quantity is required")
    @Min(value = 1, message = "Required quantity must be at least 1")
    private Integer requiredNum;

    private Integer archivedNum;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date deadline;

    @Pattern(regexp = "0|1", message = "Required flag must be 0 or 1")
    private String isRequired;

    private String status;
    private Date finishTime;
    private Integer sort;
    private String taskName;

    public Long getDeliverableId() { return deliverableId; }
    public void setDeliverableId(Long deliverableId) { this.deliverableId = deliverableId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getDeliverableName() { return deliverableName; }
    public void setDeliverableName(String deliverableName) { this.deliverableName = deliverableName; }
    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }
    public Integer getRequiredNum() { return requiredNum; }
    public void setRequiredNum(Integer requiredNum) { this.requiredNum = requiredNum; }
    public Integer getArchivedNum() { return archivedNum; }
    public void setArchivedNum(Integer archivedNum) { this.archivedNum = archivedNum; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public String getIsRequired() { return isRequired; }
    public void setIsRequired(String isRequired) { this.isRequired = isRequired; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
}
