package com.ruoyi.fund.domain.vo;

import java.io.Serializable;

public class FundUploadReceiptVo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String token;
    private String originalName;
    private Long fileSize;
    private String fileType;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
}
