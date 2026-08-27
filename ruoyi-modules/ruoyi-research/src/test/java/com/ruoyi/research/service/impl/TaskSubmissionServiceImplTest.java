package com.ruoyi.research.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskInfoMapper;
import com.ruoyi.research.mapper.TaskSubmissionMapper;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.TaskPermissionService;
import com.ruoyi.system.api.domain.FundUserOption;

@RunWith(MockitoJUnitRunner.class)
public class TaskSubmissionServiceImplTest
{
    @InjectMocks private TaskSubmissionServiceImpl service;
    @Mock private TaskSubmissionMapper submissionMapper;
    @Mock private TaskDeliverableMapper deliverableMapper;
    @Mock private TaskInfoMapper taskMapper;
    @Mock private TaskPermissionService taskPermissionService;
    @Mock private ResearchPermissionService researchPermissionService;
    @Mock private ResearchOrgService orgService;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserId("10");
        SecurityContextHolder.setUserName("submitter");
        TaskDeliverable deliverable = new TaskDeliverable();
        deliverable.setDeliverableId(30L);
        deliverable.setTaskId(20L);
        deliverable.setGroupId(1L);
        when(deliverableMapper.selectById(30L)).thenReturn(deliverable);
        TaskInfo task = new TaskInfo();
        task.setTaskId(20L);
        task.setGroupId(1L);
        task.setFrameworkId(100L);
        when(taskMapper.selectById(20L)).thenReturn(task);
        FundUserOption user = new FundUserOption();
        user.setUserId(10L);
        user.setDeptId(5L);
        when(orgService.getUser(10L)).thenReturn(user);
        when(submissionMapper.insert(any(TaskSubmission.class))).thenReturn(1);
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void createDraftDerivesAllRelationshipAndIdentityFields()
    {
        TaskSubmission submission = input();
        submission.setGroupId(999L);
        submission.setTaskId(999L);
        submission.setSubmitUserId(999L);
        submission.setStatus("3");
        assertEquals(1, service.insertDraft(submission));
        assertEquals(Long.valueOf(1L), submission.getGroupId());
        assertEquals(Long.valueOf(100L), submission.getFrameworkId());
        assertEquals(Long.valueOf(20L), submission.getTaskId());
        assertEquals(Long.valueOf(10L), submission.getSubmitUserId());
        assertEquals(Long.valueOf(5L), submission.getSubmitDeptId());
        assertEquals("0", submission.getStatus());
        assertEquals(Integer.valueOf(0), submission.getVersion());
        assertNull(submission.getSubmitTime());
    }

    @Test
    public void pendingSubmissionCannotUseDraftUpdateEndpoint()
    {
        TaskSubmission old = input();
        old.setSubmissionId(40L);
        old.setSubmitUserId(10L);
        old.setStatus("1");
        old.setVersion(0);
        when(submissionMapper.selectById(40L)).thenReturn(old);
        TaskSubmission update = input();
        update.setSubmissionId(40L);
        try
        {
            service.updateDraft(update);
            fail("Pending submission must be locked");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("draft or rejected"));
        }
    }

    private TaskSubmission input()
    {
        TaskSubmission submission = new TaskSubmission();
        submission.setDeliverableId(30L);
        submission.setSubmissionName("Submission");
        return submission;
    }
}
