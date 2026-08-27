package com.ruoyi.fund.service;
import java.util.List; import com.ruoyi.fund.domain.FundProjectBudget; import com.ruoyi.fund.domain.vo.FundOverviewVo;
public interface IFundBudgetService { FundProjectBudget selectById(Long id); FundProjectBudget selectByTopicId(Long topicId); List<FundProjectBudget> selectList(FundProjectBudget q); int insert(FundProjectBudget b); int update(FundProjectBudget b); int delete(Long id); FundOverviewVo overview(Long topicId); }
