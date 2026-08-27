package com.ruoyi.fund.service.impl;
import java.util.ArrayList; import java.util.HashSet; import java.util.List; import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException; import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.fund.util.FundSecurityUtils; import com.ruoyi.fund.constant.FundConstants; import com.ruoyi.fund.domain.FundTopic; import com.ruoyi.fund.domain.FundTopicDept;
import com.ruoyi.fund.mapper.FundTopicDeptMapper; import com.ruoyi.fund.mapper.FundTopicMapper; import com.ruoyi.fund.mapper.FundProjectBudgetMapper;
import com.ruoyi.fund.service.IFundOrgService; import com.ruoyi.fund.service.IFundTopicService;
import com.ruoyi.system.api.domain.FundDeptOption; import com.ruoyi.system.api.domain.FundUserOption;
@Service
public class FundTopicServiceImpl implements IFundTopicService {
 @Autowired private FundTopicMapper topicMapper; @Autowired private FundTopicDeptMapper topicDeptMapper; @Autowired private FundProjectBudgetMapper budgetMapper; @Autowired private IFundOrgService org;
 public FundTopic selectById(Long id){FundTopic t=topicMapper.selectFundTopicById(id); if(t!=null)t.setParticipantDeptIds(topicDeptMapper.selectDeptIdsByTopicId(id)); return t;}
 public List<FundTopic> selectList(FundTopic q){List<FundTopic> list=topicMapper.selectFundTopicList(q); for(FundTopic t:list)t.setParticipantDeptIds(topicDeptMapper.selectDeptIdsByTopicId(t.getTopicId())); return list;}
 public List<FundTopic> selectAccessibleList(){Long uid=SecurityUtils.getUserId(); List<FundTopic> all=selectList(new FundTopic()); if(FundSecurityUtils.isSystemAdmin())return all; List<FundTopic> out=new ArrayList<>(); for(FundTopic t:all) if(isTopicMember(t.getTopicId(),uid))out.add(t); return out;}
 @Transactional public int insert(FundTopic t){assertAdmin(); enrich(t); t.setStatus(t.getStatus()==null?"0":t.getStatus()); t.setCreateBy(SecurityUtils.getUsername()); int n=topicMapper.insertFundTopic(t); saveDepts(t); return n;}
 @Transactional public int update(FundTopic t){assertAdmin(); if(topicMapper.selectFundTopicById(t.getTopicId())==null)throw new ServiceException("课题不存在"); enrich(t); t.setStatus(t.getStatus()==null?"0":t.getStatus()); t.setUpdateBy(SecurityUtils.getUsername()); int n=topicMapper.updateFundTopic(t); topicDeptMapper.deleteByTopicId(t.getTopicId()); saveDepts(t); return n;}
 @Transactional public int delete(Long id){assertAdmin(); if(budgetMapper.selectByTopicId(id)!=null)throw new ServiceException("课题已配置总资金，不能删除"); topicDeptMapper.deleteByTopicId(id); return topicMapper.deleteFundTopicById(id);}
 public boolean isTopicMember(Long topicId,Long uid){if(FundSecurityUtils.isSystemAdmin())return true; FundTopic t=topicMapper.selectFundTopicById(topicId); if(t==null)return false; if(uid.equals(t.getLeaderUserId()))return true; FundUserOption u=org.getUser(uid); return u!=null&&u.getDeptId()!=null&&topicDeptMapper.countTopicDept(topicId,u.getDeptId())>0;}
 public void assertTopicMember(Long topicId,Long uid){if(!isTopicMember(topicId,uid))throw new ServiceException("无权查看或操作该课题资金使用数据");}
 public void assertTopicLeader(Long topicId,Long uid){if(FundSecurityUtils.isSystemAdmin())return; FundTopic t=topicMapper.selectFundTopicById(topicId); if(t==null||!uid.equals(t.getLeaderUserId()))throw new ServiceException("仅课题负责人可执行此操作");}
 private void assertAdmin(){if(!FundSecurityUtils.isSystemAdmin())throw new ServiceException("仅系统管理员可执行此操作");}
 private void enrich(FundTopic t){FundDeptOption lead=org.getDept(t.getLeadDeptId()); FundUserOption leader=org.getUser(t.getLeaderUserId()); if(!t.getLeadDeptId().equals(leader.getDeptId()))throw new ServiceException("课题负责人必须属于负责单位"); t.setLeadDeptName(lead.getDeptName()); t.setLeaderUserName(leader.getNickName()==null?leader.getUserName():leader.getNickName());}
 private void saveDepts(FundTopic t){List<FundTopicDept> list=new ArrayList<>(); Set<Long> seen=new HashSet<>(); add(list,seen,t.getTopicId(),t.getLeadDeptId(),FundConstants.DEPT_LEAD); if(t.getParticipantDeptIds()!=null)for(Long id:t.getParticipantDeptIds()) if(id!=null)add(list,seen,t.getTopicId(),id,FundConstants.DEPT_PARTICIPANT); if(!list.isEmpty())topicDeptMapper.batchInsert(list);}
 private void add(List<FundTopicDept> list,Set<Long> seen,Long topicId,Long deptId,String type){if(!seen.add(deptId))return; FundDeptOption d=org.getDept(deptId); FundTopicDept x=new FundTopicDept();x.setTopicId(topicId);x.setDeptId(deptId);x.setDeptType(type);x.setDeptName(d.getDeptName());list.add(x);}
}
