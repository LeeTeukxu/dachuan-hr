package com.tianye.hrsystem.modules.insurance.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Date;
import java.io.Serializable;

/**
 * 参保项目表(HrmInsuranceProject)实体类
 *
 * @author makejava
 * @since 2024-03-25 15:29:53
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_insurance_project")
@ApiModel(value = "HrmInsuranceProject对象", description = "参保项目")
public class HrmInsuranceProject implements Serializable {
    private static final long serialVersionUID = 950505052429220595L;
    /**
     * 项目id
     */
    @TableId(value = "project_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;
    /**
     * 参保方案id
     */
    private Long schemeId;
    /**
     * 1 养老保险基数 2 医疗保险基数 3 失业保险基数 4 工伤保险基数 5 生育保险基数 6 补充大病医疗保险 7 补充养老保险 8 残保险 9 社保自定义 10 公积金 11 公积金自定义
     */
    private Integer type;
    /**
     * 项目名称
     */
    private String projectName;
    /**
     * 默认基数
     */
    private Double defaultAmount;
    /**
     * 公司比例
     */
    private Double corporateProportion;
    /**
     * 个人比例
     */
    private Double personalProportion;
    /**
     * 公司缴纳金额
     */
    private Double corporateAmount;
    /**
     * 个人缴纳金额
     */
    private Double personalAmount;
    /**
     * 创建人id
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private String createUserId;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 更新人id
     */
    @TableField(fill = FieldFill.UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private String updateUserId;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;




}

