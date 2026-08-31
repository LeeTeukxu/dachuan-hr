package com.tianye.hrsystem.imple;

import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import com.tianye.hrsystem.imple.workplan.WorkPlanCustomShiftResolver;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Collections;

public class WorkPlanCustomShiftResolverTest {

    private final WorkPlanCustomShiftResolver resolver = new WorkPlanCustomShiftResolver();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    @Test
    public void normalize_shouldDetectCrossDayWhenEndNotAfterStart() throws Exception {
        WorkPlanCustomShiftResolver.NormalizedCustomShift shift = resolver.normalize("22:15", "06:05");

        Assert.assertEquals("22:15", shift.getStartText());
        Assert.assertEquals("06:05", shift.getEndText());
        Assert.assertTrue(shift.getCrossDay());
    }

    @Test
    public void normalize_shouldRejectInvalidTimeText() {
        try {
            resolver.normalize("8:30", "17:30");
            Assert.fail("应拒绝非 HH:mm 格式时间");
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage().contains("HH:mm"));
        }
    }

    @Test
    public void normalize_shouldAllowMissingEndTimeForOpenEndedShift() throws Exception {
        WorkPlanCustomShiftResolver.NormalizedCustomShift shift = resolver.normalize("08:00", null);

        Assert.assertEquals("08:00", shift.getStartText());
        Assert.assertNull("无固定下班时间应保留空结束时间", shift.getEndText());
        Assert.assertFalse("空结束时间不应按跨天处理", shift.getCrossDay());
        Assert.assertEquals("08:00~结束", shift.buildDisplayText());
    }

    @Test
    public void findExactShiftId_shouldMatchSingleSectionShiftFromGroup() throws Exception {
        WorkPlanCustomShiftResolver.NormalizedCustomShift shift = resolver.normalize("08:30", "17:45");

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO onDuty = new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        onDuty.setCheckType("OnDuty");
        onDuty.setCheckTime(timeFormat.parse("08:30"));
        onDuty.setAcross(0L);

        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO offDuty = new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        offDuty.setCheckType("OffDuty");
        offDuty.setCheckTime(timeFormat.parse("17:45"));
        offDuty.setAcross(0L);

        OapiAttendanceGetsimplegroupsResponse.AtSectionVo section = new OapiAttendanceGetsimplegroupsResponse.AtSectionVo();
        section.setTimes(java.util.Arrays.asList(onDuty, offDuty));

        OapiAttendanceGetsimplegroupsResponse.AtClassVo atClassVo = new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        atClassVo.setClassId(9001L);
        atClassVo.setClassName("白班");
        atClassVo.setSections(Collections.singletonList(section));

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group = new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        group.setGroupId(1001L);
        group.setSelectedClass(Collections.singletonList(atClassVo));

        Long matchedShiftId = resolver.findExactShiftId(group, shift);

        Assert.assertEquals(Long.valueOf(9001L), matchedShiftId);
    }
}
