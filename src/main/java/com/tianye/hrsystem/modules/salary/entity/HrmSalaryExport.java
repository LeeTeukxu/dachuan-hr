package com.tianye.hrsystem.modules.salary.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.io.Serializable;

/**
 * 工资导出表(HrmSalaryExport)实体类
 *
 * @author makejava
 * @since 2024-10-01 10:44:55
 */
@Data
public class HrmSalaryExport implements Serializable {
    private static final long serialVersionUID = 696786491456729042L;
    /**
     * 主键
     */
    private Integer id;
    /**
     * 序号
     */
    @ApiModelProperty(name = "xh",value = "序号")
    private String xh;
    /**
     * 年份
     */
    private Integer year;
    /**
     * 月份
     */
    private Integer month;
    /**
     * 年月
     */
    @ApiModelProperty(name = "years",value = "月份")
    private String years;
    /**
     * 姓名
     */
    private String empname;
    /**
     * 性别
     */
    private String sex;
    /**
     * 入职时间
     */
    private String entrytime;
    /**
     * 部门
     */
    private String dept;
    /**
     * 岗位
     */
    private String post;
    /**
     * 应出勤天数
     */
    private String normaldays;
    /**
     * 超缺勤天数
     */
    private BigDecimal absencehours = new BigDecimal(0);
    /**
     * 加班时长
     */
    private BigDecimal overtime = new BigDecimal(0);
    /**
     * 基本工资
     */
    private BigDecimal basicsalary = new BigDecimal(0);
    /**
     * 岗位工资
     */
    private BigDecimal postsalary = new BigDecimal(0);
    /**
     * 职务工资
     */
    private BigDecimal dutiessalary = new BigDecimal(0);
    /**
     * 高温工资
     */
    private BigDecimal hightempsalary = new BigDecimal(0);
    /**
     * 低温工资
     */
    private BigDecimal lowtempsalary = new BigDecimal(0);
    /**
     * 夜班津贴
     */
    private BigDecimal nightshiftsalary = new BigDecimal(0);
    /**
     * 其他津贴
     */
    private BigDecimal othersalary = new BigDecimal(0);
    /**
     * 全勤奖
     */
    private BigDecimal fullattendancesalary = new BigDecimal(0);
    /**
     *  加班工资
     */
    private BigDecimal overtimesalary = new BigDecimal(0);
    /**
     * 超缺勤工资
     */
    private BigDecimal absencesalary = new BigDecimal(0);
    /**
     * 工资合计
     */
    private BigDecimal totalsalary = new BigDecimal(0);
    /**
     * 个人所得税
     */
    private BigDecimal tax = new BigDecimal(0);
    /**
     * 社保
     */
    private BigDecimal social = new BigDecimal(0);
    /**
     * 公积金
     */
    private BigDecimal accumulation = new BigDecimal(0);
    /**
     * 其他扣款
     */
    private BigDecimal otherdeduction = new BigDecimal(0);
    /**
     * 工会费
     */
    private BigDecimal unionfees = new BigDecimal(0);
    /**
     * 实发工资
     */
    private BigDecimal actualitysalary = new BigDecimal(0);
    /**
     * 绩效工资
     */
    private BigDecimal performance = new BigDecimal(0);
    /**
     * 签领人
     */
    private String signuser;
    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 部门名称
     */
    private String deptname;
}

