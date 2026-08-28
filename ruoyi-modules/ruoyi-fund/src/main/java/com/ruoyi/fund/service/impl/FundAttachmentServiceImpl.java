package com.ruoyi.fund.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.file.FileTypeUtils;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.fund.domain.FundAttachment;
import com.ruoyi.fund.domain.FundUploadReceipt;
import com.ruoyi.fund.domain.vo.FundUploadReceiptVo;
import com.ruoyi.fund.mapper.FundAttachmentMapper;
import com.ruoyi.fund.mapper.FundUploadReceiptMapper;
import com.ruoyi.fund.service.FundPermissionService;
import com.ruoyi.fund.service.IFundAttachmentService;
import com.ruoyi.system.api.RemoteFileService;
import com.ruoyi.system.api.domain.SysFile;
import static com.ruoyi.fund.constant.FundAuditConstants.ALLOCATION_RECORD;
import static com.ruoyi.fund.constant.FundAuditConstants.USE_RECORD;

@Service
public class FundAttachmentServiceImpl implements IFundAttachmentService
{
    private static final int MAX_ATTACHMENTS = 20;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final long RECEIPT_VALID_MILLIS = 30L * 60L * 1000L;
    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png"));

    @Autowired
    private FundAttachmentMapper mapper;

    @Autowired
    private FundUploadReceiptMapper receiptMapper;

    @Autowired
    private FundPermissionService permissionService;

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
    public FundUploadReceiptVo upload(MultipartFile file)
    {
        validateUpload(file);
        R<SysFile> remote = remoteFileService.upload(file);
        if (remote == null || remote.getCode() != R.SUCCESS || remote.getData() == null
                || remote.getData().getUrl() == null || remote.getData().getUrl().trim().isEmpty())
        {
            throw new ServiceException(remote == null || remote.getMsg() == null
                    ? "文件服务上传失败" : remote.getMsg());
        }

        String originalName = FileUtils.getName(file.getOriginalFilename());
        String fileType = FileTypeUtils.getFileType(originalName);
        Date now = new Date();
        FundUploadReceipt receipt = new FundUploadReceipt();
        receipt.setUploadToken(UUID.randomUUID().toString().replace("-", ""));
        receipt.setFileName(safeName(remote.getData().getName()));
        receipt.setOriginalName(originalName);
        receipt.setFileUrl(remote.getData().getUrl());
        receipt.setFileSize(file.getSize());
        receipt.setFileType(fileType);
        receipt.setUploadUserId(SecurityUtils.getUserId());
        receipt.setUploadTime(now);
        receipt.setExpireTime(new Date(now.getTime() + RECEIPT_VALID_MILLIS));
        receipt.setUsedFlag("0");
        receiptMapper.insert(receipt);

        FundUploadReceiptVo result = new FundUploadReceiptVo();
        result.setToken(receipt.getUploadToken());
        result.setOriginalName(receipt.getOriginalName());
        result.setFileSize(receipt.getFileSize());
        result.setFileType(receipt.getFileType());
        return result;
    }

    @Override
    @Transactional
    public void consume(Long groupId, String businessType, Long businessId, String attachmentTokens)
    {
        validateBusinessType(businessType);
        List<String> tokens = parseTokens(attachmentTokens);
        if (tokens.isEmpty()) return;
        if (mapper.countByBusiness(businessType, businessId) + tokens.size() > MAX_ATTACHMENTS)
            throw new ServiceException("单条资金记录最多关联20个附件");

        Long userId = SecurityUtils.getUserId();
        Date now = new Date();
        for (String token : tokens)
        {
            FundUploadReceipt receipt = receiptMapper.selectForUpdate(token);
            if (receipt == null || !"0".equals(receipt.getUsedFlag())
                    || !userId.equals(receipt.getUploadUserId())
                    || receipt.getExpireTime() == null || receipt.getExpireTime().before(now))
                throw new ServiceException("附件上传凭证无效、已使用或已过期");

            FundAttachment attachment = new FundAttachment();
            attachment.setGroupId(groupId);
            attachment.setBusinessType(businessType);
            attachment.setBusinessId(businessId);
            attachment.setFileName(receipt.getFileName());
            attachment.setOriginalName(receipt.getOriginalName());
            attachment.setFileUrl(receipt.getFileUrl());
            attachment.setFileSize(receipt.getFileSize());
            attachment.setFileType(receipt.getFileType());
            attachment.setUploadUserId(userId);
            attachment.setUploadTime(receipt.getUploadTime());
            attachment.setDelFlag("0");
            mapper.insert(attachment);
            if (receiptMapper.markUsed(token, businessType, businessId) != 1)
                throw new ServiceException("附件上传凭证已被使用");
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
        permissionService.assertCanDownloadAttachment(
                attachment.getGroupId(), attachment.getBusinessType(), userId);

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

    private List<String> parseTokens(String value)
    {
        Set<String> unique = new LinkedHashSet<>();
        if (value != null)
        {
            for (String part : value.split(","))
            {
                String token = part == null ? "" : part.trim().toLowerCase();
                if (token.isEmpty())
                {
                    continue;
                }
                if (!token.matches("[0-9a-f]{32}"))
                    throw new ServiceException("附件上传凭证格式错误");
                unique.add(token);
            }
        }
        if (unique.size() > MAX_ATTACHMENTS)
        {
            throw new ServiceException("单条资金记录最多关联20个附件");
        }
        return Arrays.asList(unique.toArray(new String[unique.size()]));
    }

    private void validateUpload(MultipartFile file)
    {
        if (file == null || file.isEmpty()) throw new ServiceException("请选择要上传的附件");
        if (file.getSize() > MAX_FILE_SIZE) throw new ServiceException("附件大小不能超过5MB");
        String originalName = FileUtils.getName(file.getOriginalFilename());
        if (originalName == null || originalName.trim().isEmpty())
            throw new ServiceException("附件名称不能为空");
        if (originalName.length() > MAX_FILE_NAME_LENGTH)
            throw new ServiceException("附件名称不能超过255个字符");
        String fileType = FileTypeUtils.getFileType(originalName);
        if (!ALLOWED_FILE_TYPES.contains(fileType))
            throw new ServiceException("仅支持pdf/doc/docx/xls/xlsx/jpg/jpeg/png格式附件");
    }

    private void validateBusinessType(String businessType)
    {
        if (!ALLOCATION_RECORD.equals(businessType) && !USE_RECORD.equals(businessType))
            throw new ServiceException("不支持的资金附件业务类型");
    }

    private String safeName(String fileName)
    {
        String name = FileUtils.getName(fileName);
        if (name == null || name.trim().isEmpty()) name = "attachment";
        return name.length() > MAX_FILE_NAME_LENGTH
                ? name.substring(name.length() - MAX_FILE_NAME_LENGTH) : name;
    }
}
