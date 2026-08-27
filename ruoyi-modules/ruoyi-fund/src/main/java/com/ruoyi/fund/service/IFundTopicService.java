package com.ruoyi.fund.service;
import java.util.List;
import com.ruoyi.fund.domain.FundTopic;
public interface IFundTopicService {
    FundTopic selectById(Long topicId); List<FundTopic> selectList(FundTopic query); List<FundTopic> selectAccessibleList();
    int insert(FundTopic topic); int update(FundTopic topic); int delete(Long topicId);
    boolean isTopicMember(Long topicId,Long userId); void assertTopicMember(Long topicId,Long userId); void assertTopicLeader(Long topicId,Long userId);
}
