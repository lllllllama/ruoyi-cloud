package com.ruoyi.research.domain;

import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class ResearchGroupMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;

    @NotNull(message = "User is required")
    private Long userId;

    @NotNull(message = "Department is required")
    private Long deptId;

    @NotBlank(message = "Member role is required")
    @Pattern(regexp = "LEADER|CORE|MEMBER|EXPERT",
            message = "Member role must be LEADER, CORE, MEMBER or EXPERT")
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
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getJoinTime() { return joinTime; }
    public void setJoinTime(Date joinTime) { this.joinTime = joinTime; }
    public Date getLeaveTime() { return leaveTime; }
    public void setLeaveTime(Date leaveTime) { this.leaveTime = leaveTime; }
}
