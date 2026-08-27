package com.ruoyi.fund.service;
import java.util.List; import com.ruoyi.fund.domain.FundProjectBudget; import com.ruoyi.fund.domain.vo.FundAllocationOverviewVo; import com.ruoyi.fund.domain.vo.FundUseOverviewVo;
public interface IFundBudgetService { FundProjectBudget selectById(Long id); FundProjectBudget selectByTopicId(Long topicId); List<FundProjectBudget> selectList(FundProjectBudget q); int insert(FundProjectBudget b); int update(FundProjectBudget b); int delete(Long id); FundAllocationOverviewVo allocationOverview(Long groupId); FundUseOverviewVo useOverview(Long groupId); }
