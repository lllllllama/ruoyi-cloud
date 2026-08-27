package com.ruoyi.research.domain.dto;

import javax.validation.constraints.Size;

public class TaskAuditRequest
{
    @Size(max = 1000, message = "Audit opinion must not exceed 1000 characters")
    private String opinion;

    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
}
