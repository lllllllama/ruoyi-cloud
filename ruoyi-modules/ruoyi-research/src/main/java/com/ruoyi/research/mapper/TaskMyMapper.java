package com.ruoyi.research.mapper;

import java.util.List;
import com.ruoyi.research.domain.vo.MyTaskVo;

public interface TaskMyMapper
{
    List<MyTaskVo> selectMyTasks(Long userId);
}
