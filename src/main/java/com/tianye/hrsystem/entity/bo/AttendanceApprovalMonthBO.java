package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("审批数据月份操作BO")
public class AttendanceApprovalMonthBO {

    @ApiModelProperty("月份，格式yyyy-MM（用于展示/兼容老客户端；抓取窗口以 fetchStartTime/fetchEndTime 为准，不传则按该月 1 日~末日兜底）")
    private String month;

    @ApiModelProperty("员工ID列表，空表示全部员工")
    private List<Long> employeeIds;

    @ApiModelProperty("审批类型列表，可选值：all/overtime/misscard/leave/travel")
    private List<String> approvalTypes;

    @ApiModelProperty("发起时间窗口起点（毫秒时间戳，用户所选开始日期；代表拉取钉钉审批单的发起时间下限）")
    private Long fetchStartTime;

    @ApiModelProperty("发起时间窗口终点（毫秒时间戳，点击“确定”的当下时刻；代表拉取钉钉审批单的发起时间上限）")
    private Long fetchEndTime;
}
