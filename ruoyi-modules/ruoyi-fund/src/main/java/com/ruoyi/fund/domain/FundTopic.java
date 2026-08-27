package com.ruoyi.fund.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class FundTopic extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 课题ID */
    private Long topicId;
    /** 课题名称 */
    @NotBlank(message = "课题名称不能为空")
    private String topicName;
    /** 负责单位ID */
    @NotNull(message = "负责单位不能为空")
    private Long leadDeptId;
    /** 负责单位名称快照 */
    private String leadDeptName;
    /** 课题负责人ID */
    @NotNull(message = "课题负责人不能为空")
    private Long leaderUserId;
    /** 课题负责人名称快照 */
    private String leaderUserName;
    /** 状态 0正常 1停用 */
    private String status;
    /** 参与单位ID集合 */
    private List<Long> participantDeptIds;

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public Long getLeadDeptId() { return leadDeptId; }
    public void setLeadDeptId(Long leadDeptId) { this.leadDeptId = leadDeptId; }
    public String getLeadDeptName() { return leadDeptName; }
    public void setLeadDeptName(String leadDeptName) { this.leadDeptName = leadDeptName; }
    public Long getLeaderUserId() { return leaderUserId; }
    public void setLeaderUserId(Long leaderUserId) { this.leaderUserId = leaderUserId; }
    public String getLeaderUserName() { return leaderUserName; }
    public void setLeaderUserName(String leaderUserName) { this.leaderUserName = leaderUserName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Long> getParticipantDeptIds() { return participantDeptIds; }
    public void setParticipantDeptIds(List<Long> participantDeptIds) { this.participantDeptIds = participantDeptIds; }
}
