package com.ruoyi.fund.mapper;
import java.util.List;
import com.ruoyi.fund.domain.FundProjectBudget;
public interface FundProjectBudgetMapper {
    FundProjectBudget selectById(Long budgetId);
    FundProjectBudget selectByTopicId(Long topicId);
    FundProjectBudget selectByTopicIdForUpdate(Long topicId);
    List<FundProjectBudget> selectList(FundProjectBudget query);
    int insert(FundProjectBudget budget);
    int update(FundProjectBudget budget);
    int deleteById(Long budgetId);
}
