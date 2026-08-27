package com.ruoyi.fund.mapper;
import java.util.List;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.fund.domain.FundUsePlan;
public interface FundUsePlanMapper {
    FundUsePlan selectById(Long usePlanId);
    FundUsePlan selectForUpdate(Long usePlanId);
    List<FundUsePlan> selectList(FundUsePlan query);
    int countByTopicId(Long topicId);
    BigDecimal sumPlanAmount(@Param("topicId") Long topicId, @Param("excludeUsePlanId") Long excludeUsePlanId);
    int insert(FundUsePlan plan);
    int update(FundUsePlan plan);
    int finish(@Param("usePlanId")Long usePlanId,@Param("actual")BigDecimal actual,@Param("difference")BigDecimal difference,@Param("finishType")String finishType,@Param("reason")String reason,@Param("finishUserId")Long finishUserId,@Param("updateBy")String updateBy);
    int deleteById(Long usePlanId);
}
