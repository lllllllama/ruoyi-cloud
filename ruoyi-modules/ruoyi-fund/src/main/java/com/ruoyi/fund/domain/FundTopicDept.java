package com.ruoyi.fund.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class FundTopicDept extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    /** 主键 */
    private Long id;
    /** 课题ID */
    private Long topicId;
    /** 单位ID */
    private Long deptId;
    /** 单位类型 0负责 1参与 */
    private String deptType;
    /** 单位名称快照 */
    private String deptName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptType() { return deptType; }
    public void setDeptType(String deptType) { this.deptType = deptType; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
}
