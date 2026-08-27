package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.research.domain.vo.MyTaskVo;
import com.ruoyi.research.mapper.TaskMyMapper;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskMyServiceImplTest
{
    @InjectMocks private TaskMyServiceImpl service;
    @Mock private TaskMyMapper mapper;

    @Test
    public void timeStatusIsIndependentFromBusinessStatus()
    {
        MyTaskVo overdue = task(LocalDate.now().minusDays(1), "1");
        MyTaskVo nearDue = task(LocalDate.now().plusDays(7), "1");
        MyTaskVo normal = task(LocalDate.now().plusDays(8), "1");
        MyTaskVo finishedPastDue = task(LocalDate.now().minusDays(10), "2");
        when(mapper.selectMyTasks(10L)).thenReturn(Arrays.asList(overdue, nearDue, normal, finishedPastDue));

        List<MyTaskVo> tasks = service.selectMyTasks(10L);

        assertEquals("OVERDUE", tasks.get(0).getTimeStatus());
        assertEquals("NEAR_DUE", tasks.get(1).getTimeStatus());
        assertEquals("NORMAL", tasks.get(2).getTimeStatus());
        assertEquals("NORMAL", tasks.get(3).getTimeStatus());
        assertEquals(Boolean.TRUE, tasks.get(0).getCanSubmit());
    }

    private MyTaskVo task(LocalDate deadline, String status)
    {
        MyTaskVo task = new MyTaskVo();
        task.setDeadline(Date.valueOf(deadline));
        task.setStatus(status);
        return task;
    }
}
