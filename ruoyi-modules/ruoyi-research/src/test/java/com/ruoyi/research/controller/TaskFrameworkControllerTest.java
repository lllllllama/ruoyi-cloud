package com.ruoyi.research.controller;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.research.domain.TaskFramework;
import com.ruoyi.research.domain.vo.TaskFrameworkGroupOptionVo;
import com.ruoyi.research.domain.vo.TaskFrameworkOptionVo;
import com.ruoyi.research.service.TaskFrameworkService;

public class TaskFrameworkControllerTest
{
    @Test
    public void optionsUsesTaskListPermissionAndReturnsOnlyOptionFields() throws Exception
    {
        Method method = TaskFrameworkController.class.getMethod("options");
        RequiresPermissions permissions = method.getAnnotation(RequiresPermissions.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertNotNull(permissions);
        assertArrayEquals(new String[] { "task:info:list" }, permissions.value());
        assertNotNull(mapping);
        assertArrayEquals(new String[] { "/options" }, mapping.value());

        TaskFramework framework = new TaskFramework();
        framework.setFrameworkId(11L);
        framework.setGroupId(22L);
        framework.setFrameworkName("2026 annual tasks");
        framework.setYear(2026);
        framework.setGroupName("Group A");
        framework.setOverallGoal("not exposed by options endpoint");

        TaskFrameworkService service = mock(TaskFrameworkService.class);
        when(service.selectOptions()).thenReturn(Collections.singletonList(framework));

        TaskFrameworkController controller = new TaskFrameworkController();
        ReflectionTestUtils.setField(controller, "frameworkService", service);
        AjaxResult result = controller.options();

        @SuppressWarnings("unchecked")
        List<TaskFrameworkOptionVo> options = (List<TaskFrameworkOptionVo>) result.get("data");
        assertEquals(1, options.size());
        TaskFrameworkOptionVo option = options.get(0);
        assertEquals(Long.valueOf(11L), option.getFrameworkId());
        assertEquals(Long.valueOf(22L), option.getGroupId());
        assertEquals("2026 annual tasks", option.getFrameworkName());
        assertEquals(Integer.valueOf(2026), option.getYear());
        assertEquals("Group A", option.getGroupName());
    }

    @Test
    public void groupOptionsUsesAnnualFrameworkListPermission() throws Exception
    {
        Method method = TaskFrameworkController.class.getMethod("groupOptions");
        RequiresPermissions permissions = method.getAnnotation(RequiresPermissions.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertNotNull(permissions);
        assertArrayEquals(new String[] { "task:framework:list" }, permissions.value());
        assertNotNull(mapping);
        assertArrayEquals(new String[] { "/group-options" }, mapping.value());

        TaskFrameworkGroupOptionVo option = new TaskFrameworkGroupOptionVo();
        option.setGroupId(22L);
        option.setGroupName("Group A");
        TaskFrameworkService service = mock(TaskFrameworkService.class);
        when(service.selectManagedGroupOptions()).thenReturn(Collections.singletonList(option));

        TaskFrameworkController controller = new TaskFrameworkController();
        ReflectionTestUtils.setField(controller, "frameworkService", service);
        AjaxResult result = controller.groupOptions();

        assertEquals(Collections.singletonList(option), result.get("data"));
    }
}
