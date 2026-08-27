package com.ruoyi.fund.service.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.ruoyi.fund.constant.FundAuditConstants;
import com.ruoyi.fund.domain.FundAttachment;
import com.ruoyi.fund.mapper.FundAttachmentMapper;
import com.ruoyi.fund.service.FundPermissionService;
import com.ruoyi.system.api.RemoteFileService;

@RunWith(MockitoJUnitRunner.class)
public class FundAttachmentServiceImplTest
{
    @InjectMocks private FundAttachmentServiceImpl service;
    @Mock private FundAttachmentMapper mapper;
    @Mock private FundPermissionService permissionService;
    @Mock private RemoteFileService remoteFileService;

    @Before
    public void setUp()
    {
        SecurityContextHolder.setUserId("999");
        SecurityContextHolder.setUserName("outsider");
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.remove();
    }

    @Test
    public void outsiderWithKnownUseAttachmentIdCannotReachFileService()
    {
        FundAttachment attachment = new FundAttachment();
        attachment.setAttachmentId(50L);
        attachment.setGroupId(100L);
        attachment.setBusinessType(FundAuditConstants.USE_RECORD);
        attachment.setBusinessId(60L);
        attachment.setFileUrl("/profile/upload/private.pdf");
        when(mapper.selectById(50L)).thenReturn(attachment);
        doThrow(new ServiceException("无课题访问权限"))
                .when(permissionService).assertCanDownloadAttachment(
                        100L, FundAuditConstants.USE_RECORD, 999L);

        try
        {
            service.download(50L);
            fail("Outsider must not download a fund use attachment by ID");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("权限"));
        }
        verify(remoteFileService, never()).download(anyString(), anyString());
    }
}
