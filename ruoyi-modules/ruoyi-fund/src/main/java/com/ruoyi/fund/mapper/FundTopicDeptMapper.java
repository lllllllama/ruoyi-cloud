package com.ruoyi.fund.mapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.fund.domain.FundTopicDept;
public interface FundTopicDeptMapper {
    List<FundTopicDept> selectByTopicId(Long topicId);
    List<Long> selectDeptIdsByTopicId(Long topicId);
    int countTopicDept(@Param("topicId") Long topicId, @Param("deptId") Long deptId);
    int deleteByTopicId(Long topicId);
    int batchInsert(@Param("list") List<FundTopicDept> list);
}
