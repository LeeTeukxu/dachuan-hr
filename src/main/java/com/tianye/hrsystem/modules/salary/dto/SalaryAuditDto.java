package com.tianye.hrsystem.modules.salary.dto;

import lombok.Data;

/**
 * 薪资审核请求体
 */
@Data
public class SalaryAuditDto
{
    /**
     * 操作类型   1财务审核  2总经理审核 3行政提交
     */
    private String operType;

    /**
     * 1通过 2未通过
     */
    private String auditStatus;

    /**
     * 每月薪资记录id
     */
    private long srecordId;
}
