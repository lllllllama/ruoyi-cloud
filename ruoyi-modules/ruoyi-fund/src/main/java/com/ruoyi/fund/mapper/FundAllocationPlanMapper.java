package com.ruoyi.fund.mapper;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.fund.domain.FundAllocationPlan;
public interface FundAllocationPlanMapper {
    FundAllocationPlan selectById(Long planId);
    FundAllocationPlan selectForUpdate(Long planId);
    List<FundAllocationPlan> selectList(FundAllocationPlan query);
    BigDecimal sumPlanAmount(@Param("topicId") Long topicId, @Param("excludePlanId") Long excludePlanId);
    int countByTopicId(Long topicId);
    int insert(FundAllocationPlan plan);
    int update(FundAllocationPlan plan);
    int assign(@Param("planId")Long planId,@Param("userId")Long userId,@Param("userName")String userName,@Param("updateBy")String updateBy);
    int finish(@Param("planId")Long planId,@Param("actual")BigDecimal actual,@Param("difference")BigDecimal difference,@Param("finishType")String finishType,@Param("finishUserId")Long finishUserId,@Param("updateBy")String updateBy);
    int deleteById(Long planId);
}
