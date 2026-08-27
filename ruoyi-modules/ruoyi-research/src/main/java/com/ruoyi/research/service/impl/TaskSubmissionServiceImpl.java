package com.ruoyi.research.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.research.domain.TaskDeliverable;
import com.ruoyi.research.domain.TaskInfo;
import com.ruoyi.research.domain.TaskSubmission;
import com.ruoyi.research.mapper.TaskDeliverableMapper;
import com.ruoyi.research.mapper.TaskInfoMapper;
import com.ruoyi.research.mapper.TaskSubmissionMapper;
import com.ruoyi.research.service.ResearchOrgService;
import com.ruoyi.research.service.ResearchPermissionService;
import com.ruoyi.research.service.TaskPermissionService;
import com.ruoyi.research.service.TaskAttachmentService;
import com.ruoyi.research.service.TaskSubmissionService;
import com.ruoyi.research.util.ResearchSecurityUtils;
import com.ruoyi.system.api.domain.FundUserOption;

@Service
public class TaskSubmissionServiceImpl implements TaskSubmissionService
{
    private static final String STATUS_DRAFT = "0";
    private static final String STATUS_REJECTED = "2";

    @Autowired
    private TaskSubmissionMapper submissionMapper;

    @Autowired
    private TaskDeliverableMapper deliverableMapper;

    @Autowired
    private TaskInfoMapper taskMapper;

    @Autowired
    private TaskPermissionService taskPermissionService;

    @Autowired
    private ResearchPermissionService researchPermissionService;

    @Autowired
    private ResearchOrgService orgService;

    @Autowired
    private TaskAttachmentService attachmentService;

    @Override
    public TaskSubmission selectById(Long submissionId)
    {
        TaskSubmission submission = requireSubmission(submissionId);
        assertCanView(submission);
        return submission;
    }

    @Override
    public List<TaskSubmission> selectList(TaskSubmission query)
    {
        Long userId = SecurityUtils.getUserId();
        return submissionMapper.selectList(query, userId, ResearchSecurityUtils.isSystemAdmin());
    }

    @Override
    @Transactional
    public int insertDraft(TaskSubmission submission)
    {
        normalize(submission);
        Long userId = SecurityUtils.getUserId();
        taskPermissionService.assertCanSubmitDeliverable(submission.getDeliverableId(), userId);
        TaskDeliverable deliverable = requireDeliverable(submission.getDeliverableId());
        TaskInfo task = requireTask(deliverable.getTaskId());
        if (!deliverable.getGroupId().equals(task.getGroupId()))
        {
            throw new ServiceException("Invalid deliverable task relationship");
        }
        FundUserOption user = orgService.getUser(userId);
        if (user.getDeptId() == null)
        {
            throw new ServiceException("Current user has no department and cannot create a submission");
        }
        submission.setGroupId(deliverable.getGroupId());
        submission.setFrameworkId(task.getFrameworkId());
        submission.setTaskId(task.getTaskId());
        submission.setSubmitUserId(userId);
        submission.setSubmitDeptId(user.getDeptId());
        submission.setSubmitTime(null);
        submission.setStatus(STATUS_DRAFT);
        submission.setArchiveUserId(null);
        submission.setArchiveTime(null);
        submission.setVersion(0);
        submission.setCreateBy(SecurityUtils.getUsername());
        return submissionMapper.insert(submission);
    }

    @Override
    @Transactional
    public int updateDraft(TaskSubmission input)
    {
        if (input.getSubmissionId() == null)
        {
            throw new ServiceException("Submission ID is required");
        }
        TaskSubmission old = requireSubmission(input.getSubmissionId());
        assertOwner(old);
        assertEditable(old);
        taskPermissionService.assertCanSubmitDeliverable(old.getDeliverableId(), SecurityUtils.getUserId());
        normalize(input);
        if (input.getDeliverableId() != null && !old.getDeliverableId().equals(input.getDeliverableId()))
        {
            throw new ServiceException("A submission cannot be moved to another deliverable");
        }
        input.setDeliverableId(old.getDeliverableId());
        input.setVersion(old.getVersion());
        input.setUpdateBy(SecurityUtils.getUsername());
        int rows = submissionMapper.updateDraft(input);
        if (rows == 0)
        {
            throw new ServiceException("Submission was changed concurrently; refresh and retry");
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteDraft(Long submissionId)
    {
        TaskSubmission submission = requireSubmission(submissionId);
        assertOwner(submission);
        assertEditable(submission);
        int rows = submissionMapper.deleteDraft(submissionId, submission.getVersion(), SecurityUtils.getUsername());
        if (rows == 0)
        {
            throw new ServiceException("Submission was changed concurrently; refresh and retry");
        }
        attachmentService.deleteBySubmissionId(submissionId);
        return rows;
    }

    private void normalize(TaskSubmission submission)
    {
        if (submission.getSubmissionName() == null || submission.getSubmissionName().trim().isEmpty())
        {
            throw new ServiceException("Submission name is required");
        }
        submission.setSubmissionName(submission.getSubmissionName().trim());
    }

    private void assertEditable(TaskSubmission submission)
    {
        if (!STATUS_DRAFT.equals(submission.getStatus()) && !STATUS_REJECTED.equals(submission.getStatus()))
        {
            throw new ServiceException("Only draft or rejected submissions may be edited or deleted");
        }
    }

    private void assertOwner(TaskSubmission submission)
    {
        if (!SecurityUtils.getUserId().equals(submission.getSubmitUserId()))
        {
            throw new ServiceException("Only the submitter may modify this submission");
        }
    }

    private void assertCanView(TaskSubmission submission)
    {
        Long userId = SecurityUtils.getUserId();
        if (!userId.equals(submission.getSubmitUserId())
                && !researchPermissionService.isGroupLeader(submission.getGroupId(), userId))
        {
            throw new ServiceException("No permission to view this submission");
        }
    }

    private TaskDeliverable requireDeliverable(Long deliverableId)
    {
        TaskDeliverable deliverable = deliverableMapper.selectById(deliverableId);
        if (deliverable == null)
        {
            throw new ServiceException("Task deliverable does not exist");
        }
        return deliverable;
    }

    private TaskInfo requireTask(Long taskId)
    {
        TaskInfo task = taskMapper.selectById(taskId);
        if (task == null)
        {
            throw new ServiceException("Research task does not exist");
        }
        return task;
    }

    private TaskSubmission requireSubmission(Long submissionId)
    {
        TaskSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null)
        {
            throw new ServiceException("Deliverable submission does not exist");
        }
        return submission;
    }
}
