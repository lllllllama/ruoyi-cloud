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
import com.ruoyi.research.service.TaskCompletionService;
import com.ruoyi.research.service.TaskSubmissionAuditService;
import com.ruoyi.research.service.TaskSubmissionService;
import com.ruoyi.research.util.ResearchSecurityUtils;
import com.ruoyi.system.api.domain.FundUserOption;

@Service
public class TaskSubmissionServiceImpl implements TaskSubmissionService
{
    private static final String STATUS_DRAFT = "0";
    private static final String STATUS_PENDING = "1";
    private static final String STATUS_REJECTED = "2";
    private static final String STATUS_ARCHIVED = "3";

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

    @Autowired
    private TaskSubmissionAuditService auditService;

    @Autowired
    private TaskCompletionService taskCompletionService;

    @Override
    public TaskSubmission selectById(Long submissionId)
    {
        TaskSubmission submission = requireSubmission(submissionId);
        assertCanView(submission);
        enrichUserNames(submission);
        return submission;
    }

    @Override
    public List<TaskSubmission> selectList(TaskSubmission query)
    {
        Long userId = SecurityUtils.getUserId();
        List<TaskSubmission> submissions = submissionMapper.selectList(query, userId,
                ResearchSecurityUtils.isSystemAdmin());
        for (TaskSubmission submission : submissions)
        {
            enrichUserNames(submission);
        }
        return submissions;
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

    @Override
    @Transactional
    public void submit(Long submissionId)
    {
        TaskSubmission submission = requireSubmissionForUpdate(submissionId);
        assertOwner(submission);
        requireStatus(submission, STATUS_DRAFT, "Only a draft submission may be submitted");
        taskPermissionService.assertCanSubmitDeliverable(submission.getDeliverableId(), SecurityUtils.getUserId());
        assertTransition(submissionMapper.submit(submissionId, submission.getVersion(), SecurityUtils.getUsername()));
        auditService.record(submission, "SUBMIT", STATUS_DRAFT, STATUS_PENDING, null);
    }

    @Override
    @Transactional
    public void approve(Long submissionId, String opinion)
    {
        TaskSubmission submission = requireSubmissionForUpdate(submissionId);
        assertAuditor(submission);
        requireStatus(submission, STATUS_PENDING, "Only a pending submission may be approved");
        assertTransition(submissionMapper.approve(submissionId, submission.getVersion(),
                SecurityUtils.getUserId(), SecurityUtils.getUsername()));
        recalculateDeliverable(submission.getDeliverableId());
        auditService.record(submission, "APPROVE", STATUS_PENDING, STATUS_ARCHIVED, trimOpinion(opinion));
    }

    @Override
    @Transactional
    public void reject(Long submissionId, String opinion)
    {
        TaskSubmission submission = requireSubmissionForUpdate(submissionId);
        assertAuditor(submission);
        requireStatus(submission, STATUS_PENDING, "Only a pending submission may be rejected");
        String requiredOpinion = trimOpinion(opinion);
        if (requiredOpinion == null || requiredOpinion.isEmpty())
        {
            throw new ServiceException("Rejection opinion is required");
        }
        assertTransition(submissionMapper.reject(submissionId, submission.getVersion(), SecurityUtils.getUsername()));
        auditService.record(submission, "REJECT", STATUS_PENDING, STATUS_REJECTED, requiredOpinion);
    }

    @Override
    @Transactional
    public void resubmit(Long submissionId, String opinion)
    {
        TaskSubmission submission = requireSubmissionForUpdate(submissionId);
        assertOwner(submission);
        requireStatus(submission, STATUS_REJECTED, "Only a rejected submission may be resubmitted");
        taskPermissionService.assertCanSubmitDeliverable(submission.getDeliverableId(), SecurityUtils.getUserId());
        assertTransition(submissionMapper.resubmit(submissionId, submission.getVersion(), SecurityUtils.getUsername()));
        auditService.record(submission, "RESUBMIT", STATUS_REJECTED, STATUS_PENDING, trimOpinion(opinion));
    }

    @Override
    @Transactional
    public void cancelApprove(Long submissionId, String opinion)
    {
        TaskSubmission submission = requireSubmissionForUpdate(submissionId);
        assertAuditor(submission);
        requireStatus(submission, STATUS_ARCHIVED, "Only an archived submission may have approval cancelled");
        assertTransition(submissionMapper.cancelApprove(submissionId, submission.getVersion(), SecurityUtils.getUsername()));
        recalculateDeliverable(submission.getDeliverableId());
        auditService.record(submission, "CANCEL_APPROVE", STATUS_ARCHIVED, STATUS_PENDING, trimOpinion(opinion));
    }

    private void recalculateDeliverable(Long deliverableId)
    {
        TaskDeliverable deliverable = deliverableMapper.selectForUpdate(deliverableId);
        if (deliverable == null)
        {
            throw new ServiceException("Task deliverable does not exist");
        }
        int archivedNum = deliverableMapper.countArchivedSubmissions(deliverableId);
        String status = archivedNum >= deliverable.getRequiredNum() ? "2" : (archivedNum > 0 ? "1" : "0");
        if (deliverableMapper.updateArchiveProgress(deliverableId, archivedNum, status,
                SecurityUtils.getUsername()) == 0)
        {
            throw new ServiceException("Failed to update deliverable archive progress");
        }
        taskCompletionService.recalculateFromTask(deliverable.getTaskId());
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

    private void assertAuditor(TaskSubmission submission)
    {
        if (!researchPermissionService.isGroupLeader(submission.getGroupId(), SecurityUtils.getUserId()))
        {
            throw new ServiceException("Only administrators or research group leaders may audit submissions");
        }
    }

    private void requireStatus(TaskSubmission submission, String expected, String message)
    {
        if (!expected.equals(submission.getStatus()))
        {
            throw new ServiceException(message);
        }
    }

    private void assertTransition(int rows)
    {
        if (rows == 0)
        {
            throw new ServiceException("Submission state changed concurrently; refresh and retry");
        }
    }

    private String trimOpinion(String opinion)
    {
        return opinion == null ? null : opinion.trim();
    }

    private void assertCanView(TaskSubmission submission)
    {
        Long userId = SecurityUtils.getUserId();
        if (!userId.equals(submission.getSubmitUserId())
                && !researchPermissionService.isGroupLeader(submission.getGroupId(), userId)
                && !(STATUS_ARCHIVED.equals(submission.getStatus())
                        && researchPermissionService.isGroupMember(submission.getGroupId(), userId)))
        {
            throw new ServiceException("No permission to view this submission");
        }
    }

    private void enrichUserNames(TaskSubmission submission)
    {
        if (submission.getSubmitUserId() != null)
        {
            FundUserOption user = orgService.getUser(submission.getSubmitUserId());
            if (user != null)
            {
                submission.setSubmitUserName(user.getNickName());
            }
        }
        if (submission.getArchiveUserId() != null)
        {
            FundUserOption user = orgService.getUser(submission.getArchiveUserId());
            if (user != null)
            {
                submission.setArchiveUserName(user.getNickName());
            }
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

    private TaskSubmission requireSubmissionForUpdate(Long submissionId)
    {
        TaskSubmission submission = submissionMapper.selectForUpdate(submissionId);
        if (submission == null)
        {
            throw new ServiceException("Deliverable submission does not exist");
        }
        return submission;
    }
}
