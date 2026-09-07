package com.tianye.hrsystem.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 跨月审批单按月拆分的展示结果（现算，不落库）。
 * 列表展示与后续统计模块统一调
 * IHrmAttendanceApprovalService#calculateMonthPortion 获取，保证口径唯一。
 */
@Getter
@Setter
public class AttendanceApprovalMonthPortionVO {

    @ApiModelProperty("该月展示的开始时间（截断到月初，若原单开始日在月内则保留原值）")
    private Date displayBeginTime;

    @ApiModelProperty("该月展示的结束时间（截断到月末23:59:59，若原单结束日在月内则保留原值）")
    private Date displayEndTime;

    @ApiModelProperty("该月时长（小时）：按天请假为整天累加（工作日数×8，封顶原单总时长）；按小时请假为工作日占比折算")
    private String duration;

    @ApiModelProperty("该月天数（= 该月时长/8）")
    private String durationDay;

    @ApiModelProperty("原单完整区间的工作日总数")
    private long totalWorkDays;

    @ApiModelProperty("该月部分的工作日数")
    private long monthWorkDays;
}
