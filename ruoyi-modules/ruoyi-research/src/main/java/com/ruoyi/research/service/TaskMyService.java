package com.ruoyi.research.service;

import java.util.List;
import com.ruoyi.research.domain.vo.MyTaskVo;

public interface TaskMyService
{
    List<MyTaskVo> selectMyTasks(Long userId);
}
