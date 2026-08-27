package com.ruoyi.fund.mapper;
import java.util.List;
import com.ruoyi.fund.domain.FundTopic;
public interface FundTopicMapper {
    FundTopic selectFundTopicById(Long topicId);
    List<FundTopic> selectFundTopicList(FundTopic topic);
    int insertFundTopic(FundTopic topic);
    int updateFundTopic(FundTopic topic);
    int deleteFundTopicById(Long topicId);
}
