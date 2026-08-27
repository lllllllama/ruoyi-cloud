package com.ruoyi.research.domain;

import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class TaskInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long taskId;

    @NotNull(message = "Annual framework is required")
    private Long frameworkId;

    @NotNull(message = "Research group is required")
    private Long groupId;

    private Long parentId;
    private Integer level;

    @NotBlank(message = "Task name is required")
    @Size(max = 200, message = "Task name must not exceed 200 characters")
    private String taskName;

    @Size(max = 32, message = "Task type must not exceed 32 characters")
    private String taskType;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date deadline;

    private String status;
    private Date finishTime;
    private Integer sort;
    private String frameworkName;
    private String groupName;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getFrameworkId() { return frameworkId; }
    public void setFrameworkId(Long frameworkId) { this.frameworkId = frameworkId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getFrameworkName() { return frameworkName; }
    public void setFrameworkName(String frameworkName) { this.frameworkName = frameworkName; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
