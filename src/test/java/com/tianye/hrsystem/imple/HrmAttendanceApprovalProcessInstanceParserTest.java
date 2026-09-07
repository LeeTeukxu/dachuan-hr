package com.tianye.hrsystem.imple;

import com.dingtalk.api.response.OapiProcessinstanceGetResponse;
import com.tianye.hrsystem.model.tbattendanceapprove;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

public class HrmAttendanceApprovalProcessInstanceParserTest {

    private final HrmAttendanceApprovalProcessInstanceParser parser = new HrmAttendanceApprovalProcessInstanceParser();
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Test
    public void parse_shouldBuildLeaveApprovalEntityFromProcessInstance() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("请假审批");
        process.setOriginatorUserid("ding-leave");
        process.setFormComponentValues(Arrays.asList(
                component("请假类型", "调休"),
                component("开始时间", "2026-05-02 09:00"),
                component("结束时间", "2026-05-02 18:00"),
                component("时长", "1天")
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-LEAVE", process);

        Assert.assertTrue("请假审批应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals("PROC-LEAVE", entity.getId());
        Assert.assertEquals("ding-leave", entity.getUserId());
        Assert.assertEquals(Long.valueOf(3L), entity.getBizType());
        Assert.assertEquals("请假", entity.getTagName());
        Assert.assertEquals("调休", entity.getSubType());
        Assert.assertEquals("1", entity.getDuration());
        Assert.assertEquals("天", entity.getDurationUnit());
        Assert.assertEquals(format.parse("2026-05-02 09:00"), entity.getBeginTime());
        Assert.assertEquals(format.parse("2026-05-02 18:00"), entity.getEndTime());
    }

    @Test
    public void parse_shouldReadDingTalkComplexLeaveHourDurationFromExtValue() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("李明明提交的请假申请");
        process.setOriginatorUserid("024662561026250638");
        process.setCreateTime(format.parse("2026-06-14 08:14"));
        process.setFinishTime(format.parse("2026-06-23 07:38"));
        process.setFormComponentValues(Arrays.asList(
                component(
                        "[\"开始时间\",\"结束时间\"]",
                        "[\"2026-06-14 上午\",\"2026-06-18 下午\",4,\"halfDay\",\"年假\",\"请假类型\"]",
                        "{\"unit\":\"DAY\",\"durationInDay\":4,\"durationInHour\":32,\"extension\":\"{\\\"tag\\\":\\\"年假\\\"}\"}"
                )
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-LEAVE-COMPLEX", process);

        Assert.assertTrue("复杂请假组件应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals("请假", entity.getTagName());
        Assert.assertEquals("年假", entity.getSubType());
        Assert.assertEquals("32", entity.getDuration());
        Assert.assertEquals("小时", entity.getDurationUnit());
        Assert.assertEquals(format.parse("2026-06-14 00:00"), entity.getBeginTime());
        Assert.assertEquals(format.parse("2026-06-18 00:00"), entity.getEndTime());
    }

    @Test
    public void parse_shouldPreferHourDurationForFractionalComplexLeave() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("闫倩提交的请假申请");
        process.setOriginatorUserid("30352228121210270");
        process.setCreateTime(format.parse("2026-06-26 17:30"));
        process.setFinishTime(format.parse("2026-06-26 18:00"));
        process.setFormComponentValues(Arrays.asList(
                component(
                        "[\"开始时间\",\"结束时间\"]",
                        "[\"2026-06-26 17:30\",\"2026-06-26 18:00\",0.07,\"hour\",\"调休\",\"请假类型\"]",
                        "{\"unit\":\"DAY\",\"durationInDay\":0.07,\"durationInHour\":0.5,\"extension\":\"{\\\"tag\\\":\\\"调休\\\"}\"}"
                )
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-LEAVE-HALF-HOUR", process);

        Assert.assertTrue("复杂请假组件应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals("请假", entity.getTagName());
        Assert.assertEquals("调休", entity.getSubType());
        Assert.assertEquals("0.5", entity.getDuration());
        Assert.assertEquals("小时", entity.getDurationUnit());
        Assert.assertEquals(format.parse("2026-06-26 17:30"), entity.getBeginTime());
        Assert.assertEquals(format.parse("2026-06-26 18:00"), entity.getEndTime());
    }

    @Test
    public void parse_shouldIgnoreGenericApprovalThatOnlyMentionsCompensatoryLeaveInDescription() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("闫倩提交的通用审批");
        process.setOriginatorUserid("30352228121210270");
        process.setCreateTime(format.parse("2026-06-04 16:02"));
        process.setFinishTime(format.parse("2026-06-04 20:19"));
        process.setFormComponentValues(Arrays.asList(
                component("申请内容", "关于王志兰5月工时预留的申请"),
                component("日期", "2026-06-04"),
                component("审批详情", "累计工时36.5小时，现申请将该部分工时做5月工时预留，用于后续统筹调休。")
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-GENERIC", process);

        Assert.assertFalse("通用审批正文提到调休，不应当被当成请假审批入库", result.isPresent());
    }

    @Test
    public void parse_shouldBuildOvertimeApprovalEntityFromProcessInstance() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("加班申请");
        process.setOriginatorUserid("ding-overtime");
        process.setFormComponentValues(Arrays.asList(
                component("加班类型", "休息日加班"),
                component("开始时间", "2026-05-03 18:00"),
                component("结束时间", "2026-05-03 22:00"),
                component("时长", "4小时")
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-OT", process);

        Assert.assertTrue("加班审批应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals(Long.valueOf(1L), entity.getBizType());
        Assert.assertEquals("加班", entity.getTagName());
        Assert.assertEquals("休息日加班", entity.getSubType());
        Assert.assertEquals("4", entity.getDuration());
        Assert.assertEquals("小时", entity.getDurationUnit());
    }

    @Test
    public void parse_shouldResolveDateRangeFromJsonArrayValueInsteadOfFallingBackToCreateTime() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("加班申请");
        process.setOriginatorUserid("ding-overtime");
        process.setCreateTime(format.parse("2026-04-24 10:46"));
        process.setFinishTime(format.parse("2026-04-24 10:47"));
        process.setFormComponentValues(Arrays.asList(
                component("开始时间", "[\"2026-04-17 17:30\",\"2026-04-17 18:00\"]"),
                component("时长", "0.5")
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-OT-RANGE", process);

        Assert.assertTrue("日期区间型加班审批应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals(format.parse("2026-04-17 17:30"), entity.getBeginTime());
        Assert.assertEquals(format.parse("2026-04-17 18:00"), entity.getEndTime());
        Assert.assertEquals(format.parse("2026-04-17 17:30"), entity.getWorkDate());
        Assert.assertEquals("0.5", entity.getDuration());
    }

    @Test
    public void parse_shouldRepairOvertimeBeginTimeWhenRawStartAndDurationConflict() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("加班申请");
        process.setOriginatorUserid("ding-overtime");
        process.setCreateTime(format.parse("2026-04-06 10:50"));
        process.setFinishTime(format.parse("2026-04-06 14:52"));
        process.setFormComponentValues(Arrays.asList(
                component("加班日期", "2026-03-02 08:00"),
                component("结束日期", "2026-03-31 17:30"),
                component("预计加班时长", "4")
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-OT-INCONSISTENT", process);

        Assert.assertTrue("加班审批应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals(format.parse("2026-03-31 13:30"), entity.getBeginTime());
        Assert.assertEquals(format.parse("2026-03-31 17:30"), entity.getEndTime());
        Assert.assertEquals("4", entity.getDuration());
        Assert.assertEquals("小时", entity.getDurationUnit());
    }

    @Test
    public void parse_shouldStoreHalfDayAnnualLeaveAsDayTimes8Hours_notCalendarArtifact() throws Exception {
        // 复现：孙蕃荣 2026-08-24 下午半天年假，钉钉 时长(天)=0.5；
        // ext_value.durationInHour=12 是"下午"的日历小时伪值，不得直接作为工作小时。
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("孙蕃荣提交的请假申请");
        process.setOriginatorUserid("170046110423564185");
        process.setFormComponentValues(Arrays.asList(
                component(
                        "[\"开始时间\",\"结束时间\"]",
                        "[\"2026-08-24 下午\",\"2026-08-24 下午\",0.5,\"halfDay\",\"年假\",\"请假类型\"]",
                        "{\"unit\":\"DAY\",\"durationInDay\":0.5,\"durationInHour\":12,\"extension\":\"{\\\"tag\\\":\\\"年假\\\"}\"}"
                )
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-ANNUAL-HALF-DAY", process);

        Assert.assertTrue("半天年假应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals("请假", entity.getTagName());
        Assert.assertEquals("年假", entity.getSubType());
        // 半天=0.5 天 ⇒ 按 8 工作小时/天折算为 4 小时，而非钉钉日历伪值 12 小时
        Assert.assertEquals("4", entity.getDuration());
        Assert.assertEquals("小时", entity.getDurationUnit());
        Assert.assertEquals("0.5", entity.getDurationDay());
    }

    @Test
    public void parse_shouldComputeDurationDayConsistentlyForWholeAndFractionalDayLeaves() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo fullDay = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        fullDay.setTitle("王武提交的请假申请");
        fullDay.setOriginatorUserid("user-full-day");
        fullDay.setFormComponentValues(Arrays.asList(
                component(
                        "[\"开始时间\",\"结束时间\"]",
                        "[\"2026-08-24 上午\",\"2026-08-25 下午\",1.5,\"halfDay\",\"事假\",\"请假类型\"]",
                        "{\"unit\":\"DAY\",\"durationInDay\":1.5,\"durationInHour\":36,\"extension\":\"{\\\"tag\\\":\\\"事假\\\"}\"}"
                )
        ));
        Optional<tbattendanceapprove> fullDayEntity = parser.parse("PROC-LEAVE-1.5DAY", fullDay);
        Assert.assertTrue(fullDayEntity.isPresent());
        // 1.5 天 ⇒ 12 小时（1.5*8），durationInHour=36 为日历小时伪值忽略；durationDay=1.5
        Assert.assertEquals("12", fullDayEntity.get().getDuration());
        Assert.assertEquals("1.5", fullDayEntity.get().getDurationDay());
    }

    @Test
    public void parse_shouldBuildMisscardApprovalEntityFromSinglePunchTime() throws Exception {
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("补卡申请");
        process.setOriginatorUserid("ding-misscard");
        process.setFormComponentValues(Arrays.asList(
                component("补卡时间", "2026-05-04 08:55")
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-MISS", process);

        Assert.assertTrue("补卡审批应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertNull("补卡不应强制映射到旧bizType枚举", entity.getBizType());
        Assert.assertEquals("补卡", entity.getTagName());
        Assert.assertEquals(format.parse("2026-05-04 08:55"), entity.getBeginTime());
        Assert.assertEquals(format.parse("2026-05-04 08:55"), entity.getEndTime());
    }

    @Test
    public void parse_shouldResolveCrossMonthLeaveRangeFromLeaveNamedComponent() throws Exception {
        // 跨月调休：7/24 申请、业务 7/24~8/4。组件名为"请假时间"（不在通用候选名单），
        // 修复前 beginTime/endTime 兜底为发起时间/审批完成时间（全落 7 月），8 月列表查不到。
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("张三提交的请假申请");
        process.setOriginatorUserid("ding-cross-month");
        process.setCreateTime(format.parse("2026-07-24 10:00"));
        process.setFinishTime(format.parse("2026-07-25 09:00"));
        process.setFormComponentValues(Arrays.asList(
                component("请假类型", "调休"),
                component("请假时间", "2026-07-24 09:00 至 2026-08-04 18:00"),
                component(
                        "请假时长",
                        "8天",
                        "{\"unit\":\"DAY\",\"durationInDay\":8,\"durationInHour\":64,\"extension\":\"{\\\"tag\\\":\\\"调休\\\"}\"}"
                )
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-LEAVE-CROSS-MONTH", process);

        Assert.assertTrue("跨月调休应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals("调休", entity.getSubType());
        Assert.assertEquals(format.parse("2026-07-24 09:00"), entity.getBeginTime());
        Assert.assertEquals(format.parse("2026-08-04 18:00"), entity.getEndTime());
        Assert.assertEquals(format.parse("2026-07-24 09:00"), entity.getWorkDate());
    }

    @Test
    public void parse_shouldResolveCrossMonthLeaveRangeFromExtValueStartEndTime() throws Exception {
        // 组件名不含任何已知候选词时，从请假控件 ext_value 的权威 start_time/end_time（毫秒）取业务区间。
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo process = new OapiProcessinstanceGetResponse.ProcessInstanceTopVo();
        process.setTitle("李四提交的请假申请");
        process.setOriginatorUserid("ding-ext-range");
        process.setCreateTime(format.parse("2026-07-24 10:00"));
        process.setFinishTime(format.parse("2026-07-25 09:00"));
        Date begin = format.parse("2026-07-24 09:00");
        Date end = format.parse("2026-08-04 18:00");
        process.setFormComponentValues(Arrays.asList(
                component(
                        "假期组件",
                        "",
                        "{\"start_time\":\"" + begin.getTime() + "\",\"end_time\":\"" + end.getTime()
                                + "\",\"durationInDay\":8,\"extension\":\"{\\\"tag\\\":\\\"调休\\\"}\"}"
                )
        ));

        Optional<tbattendanceapprove> result = parser.parse("PROC-LEAVE-EXT-RANGE", process);

        Assert.assertTrue("ext_value 区间请假应被识别", result.isPresent());
        tbattendanceapprove entity = result.get();
        Assert.assertEquals(begin, entity.getBeginTime());
        Assert.assertEquals(end, entity.getEndTime());
    }

    private OapiProcessinstanceGetResponse.FormComponentValueVo component(String name, String value) {
        OapiProcessinstanceGetResponse.FormComponentValueVo component = new OapiProcessinstanceGetResponse.FormComponentValueVo();
        component.setName(name);
        component.setValue(value);
        return component;
    }

    private OapiProcessinstanceGetResponse.FormComponentValueVo component(String name, String value, String extValue) {
        OapiProcessinstanceGetResponse.FormComponentValueVo component = component(name, value);
        component.setExtValue(extValue);
        return component;
    }
}
