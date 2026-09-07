package com.tianye.hrsystem.modules.dashboard.bo;

import com.tianye.hrsystem.common.PageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 看板通用查询参数
 */
@Getter
@Setter
public class DashboardQueryBO extends PageEntity {

    @ApiModelProperty("统计年份")
    private Integer year;

    @ApiModelProperty("统计月份1-12，按年查看时可不传")
    private Integer month;

    @ApiModelProperty("统计周期模式 m:按月 y:按年")
    private String mode = "m";

    @ApiModelProperty("开始月份，格式YYYY-MM，优先于year/month")
    private String startDate;

    @ApiModelProperty("结束月份，格式YYYY-MM，优先于year/month")
    private String endDate;

    @ApiModelProperty("部门id，可选")
    private Long deptId;

    @ApiModelProperty("离职类型 1主动 2被动 3退休，可选")
    private Integer quitType;

    @ApiModelProperty("结构/分布维度：sex性别 edu学历 age年龄段 tenure司龄段 dept部门 employment聘用形式 type离职类型 reason离职原因")
    private String dim;

    @ApiModelProperty("交叉分析堆叠维度")
    private String stackDim;

    @ApiModelProperty("性别 1男 2女，人员明细筛选")
    private Integer sex;

    @ApiModelProperty("学历值，人员明细筛选")
    private Integer edu;

    @ApiModelProperty("年龄段：<25 26-35 36-45 46+")
    private String ageBand;

    @ApiModelProperty("司龄段：<1年 1-3年 3-5年 5-10年 10年以上")
    private String tenureBand;

    @ApiModelProperty("在职状态：1在职 2待入职 3待离职 4离职")
    private Integer entryStatus;

    @ApiModelProperty("绩效指标类型 1安全 2质量 3营收 4成本 5产能")
    private Integer indicatorType;

    @ApiModelProperty("趋势窗口起始年（可选，覆盖统计周期）")
    private Integer winStartYear;

    @ApiModelProperty("趋势窗口起始月")
    private Integer winStartMonth;

    @ApiModelProperty("趋势窗口结束年")
    private Integer winEndYear;

    @ApiModelProperty("趋势窗口结束月")
    private Integer winEndMonth;

    @ApiModelProperty("公司ID，可选，集团模式下传入指定分公司")
    private String companyId;

    @ApiModelProperty("公司名称，用于定位部门树根节点过滤子公司数据")
    private String companyName;

    @ApiModelProperty("看板元素唯一键（board:type:key），用于权限校验申明")
    private String dashEl;
}
