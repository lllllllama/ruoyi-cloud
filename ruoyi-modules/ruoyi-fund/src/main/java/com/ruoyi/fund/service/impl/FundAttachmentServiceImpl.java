package com.ruoyi.fund.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.fund.constant.FundAuditConstants;
import com.ruoyi.fund.domain.FundAttachment;
import com.ruoyi.fund.mapper.FundAttachmentMapper;
import com.ruoyi.fund.service.IFundAttachmentService;
import com.ruoyi.fund.service.IFundResearchService;
import com.ruoyi.fund.util.FundSecurityUtils;
import com.ruoyi.system.api.RemoteFileService;

@Service
public class FundAttachmentServiceImpl implements IFundAttachmentService
{
    private static final int MAX_ATTACHMENTS = 20;
    private static final int MAX_URL_LENGTH = 1000;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_FILE_TYPE_LENGTH = 64;

    @Autowired
    private FundAttachmentMapper mapper;

    @Autowired
    private IFundResearchService researchService;

    @Autowired
    private RemoteFileService remoteFileService;

    @Override
    public FundAttachment selectById(Long attachmentId)
    {
        return mapper.selectById(attachmentId);
    }

    @Override
    public List<FundAttachment> selectByBusiness(String businessType, Long businessId)
    {
        return mapper.selectByBusiness(businessType, businessId);
    }

    @Override
    public void sync(Long groupId, String businessType, Long businessId, String voucherUrls)
    {
        mapper.deleteByBusiness(businessType, businessId);
        List<String> urls = parseUrls(voucherUrls);
        for (String url : urls)
        {
            FundAttachment attachment = new FundAttachment();
            attachment.setGroupId(groupId);
            attachment.setBusinessType(businessType);
            attachment.setBusinessId(businessId);
            attachment.setFileUrl(url);
            String fileName = fileName(url);
            attachment.setFileName(fileName);
            attachment.setOriginalName(fileName);
            attachment.setFileType(fileType(fileName));
            attachment.setUploadUserId(SecurityUtils.getUserId());
            attachment.setUploadTime(new Date());
            attachment.setDelFlag("0");
            mapper.insert(attachment);
        }
    }

    @Override
    public void deleteByBusiness(String businessType, Long businessId)
    {
        mapper.deleteByBusiness(businessType, businessId);
    }

    @Override
    public ResponseEntity<byte[]> download(Long attachmentId)
    {
        FundAttachment attachment = mapper.selectById(attachmentId);
        if (attachment == null)
        {
            throw new ServiceException("附件不存在");
        }
        Long userId = SecurityUtils.getUserId();
        if (FundAuditConstants.USE_RECORD.equals(attachment.getBusinessType())
                && !FundSecurityUtils.isSystemAdmin())
        {
            researchService.assertGroupMember(attachment.getGroupId(), userId);
        }
        else if (!FundAuditConstants.ALLOCATION_RECORD.equals(attachment.getBusinessType())
                && !FundAuditConstants.USE_RECORD.equals(attachment.getBusinessType()))
        {
            throw new ServiceException("不支持的附件业务类型");
        }

        ResponseEntity<byte[]> remote = remoteFileService.download(
                attachment.getFileUrl(), SecurityConstants.INNER);
        if (remote == null || !remote.getStatusCode().is2xxSuccessful() || remote.getBody() == null)
        {
            throw new ServiceException("文件服务下载失败");
        }

        byte[] content = remote.getBody();
        HttpHeaders headers = new HttpHeaders();
        MediaType contentType = remote.getHeaders().getContentType();
        headers.setContentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : contentType);
        headers.setContentLength(content.length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition(attachment.getOriginalName()));
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private String disposition(String fileName)
    {
        String safeName = fileName == null || fileName.trim().isEmpty() ? "attachment" : fileName;
        try
        {
            return "attachment; filename*=UTF-8''" + FileUtils.percentEncode(safeName);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new ServiceException("附件名称编码失败");
        }
    }

    private List<String> parseUrls(String value)
    {
        Set<String> unique = new LinkedHashSet<>();
        if (value != null)
        {
            for (String part : value.split(","))
            {
                String url = part == null ? "" : part.trim();
                if (url.isEmpty())
                {
                    continue;
                }
                if (url.length() > MAX_URL_LENGTH)
                {
                    throw new ServiceException("附件地址不能超过1000个字符");
                }
                unique.add(url);
            }
        }
        if (unique.size() > MAX_ATTACHMENTS)
        {
            throw new ServiceException("单条资金记录最多关联20个附件");
        }
        return new ArrayList<>(unique);
    }

    private String fileName(String url)
    {
        String clean = url;
        int query = clean.indexOf('?');
        if (query >= 0)
        {
            clean = clean.substring(0, query);
        }
        clean = clean.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        String name = slash >= 0 ? clean.substring(slash + 1) : clean;
        name = name.isEmpty() ? "attachment" : name;
        return name.length() > MAX_FILE_NAME_LENGTH
                ? name.substring(name.length() - MAX_FILE_NAME_LENGTH) : name;
    }

    private String fileType(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1)
        {
            return null;
        }
        String type = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return type.length() > MAX_FILE_TYPE_LENGTH ? type.substring(0, MAX_FILE_TYPE_LENGTH) : type;
    }
}
