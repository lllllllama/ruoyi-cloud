package com.ruoyi.fund.service.impl;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.fund.service.IFundOrgService;
import com.ruoyi.system.api.RemoteFundSupportService;
import com.ruoyi.system.api.domain.FundDeptOption;
import com.ruoyi.system.api.domain.FundUserOption;
@Service
public class FundOrgServiceImpl implements IFundOrgService {
 @Autowired private RemoteFundSupportService remote;
 private <T> T data(R<T> r){ if(r==null||r.getCode()!=R.SUCCESS) throw new ServiceException(r==null?"系统服务不可用":r.getMsg()); return r.getData(); }
 public List<FundDeptOption> getDepts(){return data(remote.getDepts(SecurityConstants.INNER));}
 public FundDeptOption getDept(Long id){return data(remote.getDept(id,SecurityConstants.INNER));}
 public List<FundUserOption> getUsersByDept(Long id){return data(remote.getUsersByDept(id,SecurityConstants.INNER));}
 public FundUserOption getUser(Long id){return data(remote.getUser(id,SecurityConstants.INNER));}
 public boolean isDeptMember(Long deptId,Long userId){FundUserOption u=getUser(userId);return u!=null&&deptId!=null&&deptId.equals(u.getDeptId());}
 public boolean isDeptLeader(Long deptId,Long userId){FundDeptOption d=getDept(deptId);FundUserOption u=getUser(userId);return d!=null&&u!=null&&d.getLeader()!=null&&d.getLeader().equals(u.getUserName());}
}
