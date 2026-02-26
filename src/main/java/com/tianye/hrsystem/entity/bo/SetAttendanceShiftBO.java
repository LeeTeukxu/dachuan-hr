package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: SetAttendanceShiftVo
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月20日 23:01
 **/
@Data
public class SetAttendanceShiftBO {
    @ApiModelProperty(value = "班次id")
    private Long shiftId;

    @ApiModelProperty(value = "班次类型（0 休息 1早晚打卡 2 分段打卡）")
    private Integer shiftType;

    @ApiModelProperty(value = "班次名称")
    private String shiftName;

    @ApiModelProperty(value = "班次时长（分钟）")
    private Integer shiftHours;

    @ApiModelProperty(value = "上班时间1")
    private String start1;

    @ApiModelProperty(value = "下班时间1")
    private String end1;

    @ApiModelProperty(value = "上班时间2")
    private String start2;

    @ApiModelProperty(value = "下班时间2")
    private String end2;

    @ApiModelProperty(value = "上班时间3")
    private String start3;

    @ApiModelProperty(value = "下班时间3")
    private String end3;

    @ApiModelProperty(value = "上班最早打卡时间1")
    private String advanceCard1;

    @ApiModelProperty(value = "上班最晚打卡时间1")
    private String lateCard1;

    @ApiModelProperty(value = "上班最早打卡时间2")
    private String advanceCard2;

    @ApiModelProperty(value = "上班最晚打卡时间2")
    private String lateCard2;

    @ApiModelProperty(value = "上班最早打卡时间3")
    private String advanceCard3;

    @ApiModelProperty(value = "上班最晚打卡时间3")
    private String lateCard3;

    @ApiModelProperty(value = "下班最早打卡时间1")
    private String earlyCard1;

    @ApiModelProperty(value = "下班最晚打卡时间1")
    private String postponeCard1;

    @ApiModelProperty(value = "下班最早打卡时间2")
    private String earlyCard2;

    @ApiModelProperty(value = "下班最晚打卡时间2")
    private String postponeCard2;

    @ApiModelProperty(value = "下班最早打卡时间3")
    private String earlyCard3;

    @ApiModelProperty(value = "下班最晚打卡时间3")
    private String postponeCard3;

    @ApiModelProperty(value = "是否设置休息时间（0否 1是）")
    private Integer restTimeStatus;

    @ApiModelProperty(value = "休息开始时间")
    private String restStartTime;

    @ApiModelProperty(value = "休息结束时间")
    private String restEndTime;

    @ApiModelProperty(value = "是否是默认配置（0否 1是）")
    private Integer isDefaultSetting;

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public Integer getShiftType() {
        return shiftType;
    }

    public void setShiftType(Integer shiftType) {
        this.shiftType = shiftType;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public Integer getShiftHours() {
        return shiftHours;
    }

    public void setShiftHours(Integer shiftHours) {
        this.shiftHours = shiftHours;
    }

    public String getStart1() {
        return start1;
    }

    public void setStart1(String start1) {
        this.start1 = start1;
    }

    public String getEnd1() {
        return end1;
    }

    public void setEnd1(String end1) {
        this.end1 = end1;
    }

    public String getStart2() {
        return start2;
    }

    public void setStart2(String start2) {
        this.start2 = start2;
    }

    public String getEnd2() {
        return end2;
    }

    public void setEnd2(String end2) {
        this.end2 = end2;
    }

    public String getStart3() {
        return start3;
    }

    public void setStart3(String start3) {
        this.start3 = start3;
    }

    public String getEnd3() {
        return end3;
    }

    public void setEnd3(String end3) {
        this.end3 = end3;
    }

    public String getAdvanceCard1() {
        return advanceCard1;
    }

    public void setAdvanceCard1(String advanceCard1) {
        this.advanceCard1 = advanceCard1;
    }

    public String getLateCard1() {
        return lateCard1;
    }

    public void setLateCard1(String lateCard1) {
        this.lateCard1 = lateCard1;
    }

    public String getAdvanceCard2() {
        return advanceCard2;
    }

    public void setAdvanceCard2(String advanceCard2) {
        this.advanceCard2 = advanceCard2;
    }

    public String getLateCard2() {
        return lateCard2;
    }

    public void setLateCard2(String lateCard2) {
        this.lateCard2 = lateCard2;
    }

    public String getAdvanceCard3() {
        return advanceCard3;
    }

    public void setAdvanceCard3(String advanceCard3) {
        this.advanceCard3 = advanceCard3;
    }

    public String getLateCard3() {
        return lateCard3;
    }

    public void setLateCard3(String lateCard3) {
        this.lateCard3 = lateCard3;
    }

    public String getEarlyCard1() {
        return earlyCard1;
    }

    public void setEarlyCard1(String earlyCard1) {
        this.earlyCard1 = earlyCard1;
    }

    public String getPostponeCard1() {
        return postponeCard1;
    }

    public void setPostponeCard1(String postponeCard1) {
        this.postponeCard1 = postponeCard1;
    }

    public String getEarlyCard2() {
        return earlyCard2;
    }

    public void setEarlyCard2(String earlyCard2) {
        this.earlyCard2 = earlyCard2;
    }

    public String getPostponeCard2() {
        return postponeCard2;
    }

    public void setPostponeCard2(String postponeCard2) {
        this.postponeCard2 = postponeCard2;
    }

    public String getEarlyCard3() {
        return earlyCard3;
    }

    public void setEarlyCard3(String earlyCard3) {
        this.earlyCard3 = earlyCard3;
    }

    public String getPostponeCard3() {
        return postponeCard3;
    }

    public void setPostponeCard3(String postponeCard3) {
        this.postponeCard3 = postponeCard3;
    }

    public Integer getRestTimeStatus() {
        return restTimeStatus;
    }

    public void setRestTimeStatus(Integer restTimeStatus) {
        this.restTimeStatus = restTimeStatus;
    }

    public String getRestStartTime() {
        return restStartTime;
    }

    public void setRestStartTime(String restStartTime) {
        this.restStartTime = restStartTime;
    }

    public String getRestEndTime() {
        return restEndTime;
    }

    public void setRestEndTime(String restEndTime) {
        this.restEndTime = restEndTime;
    }

    public Integer getIsDefaultSetting() {
        return isDefaultSetting;
    }

    public void setIsDefaultSetting(Integer isDefaultSetting) {
        this.isDefaultSetting = isDefaultSetting;
    }
}
