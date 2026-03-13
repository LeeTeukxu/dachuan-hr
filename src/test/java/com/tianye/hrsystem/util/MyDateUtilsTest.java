package com.tianye.hrsystem.util;

import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MyDateUtilsTest {

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void getDateRangeByLimit_shouldCreateContinuousSevenDayWindows() throws Exception {
        MyDateUtils dateUtils = new MyDateUtils();
        Date begin = dateTimeFormat.parse("2026-02-01 00:00:00");
        Date end = dateTimeFormat.parse("2026-02-28 00:00:00");

        List<Date[]> ranges = dateUtils.getDateRangeByLimit(begin, end, 7);

        Assert.assertEquals(4, ranges.size());

        Assert.assertEquals("2026-02-01 00:00:00", dateTimeFormat.format(ranges.get(0)[0]));
        Assert.assertEquals("2026-02-07 23:59:59", dateTimeFormat.format(ranges.get(0)[1]));

        Assert.assertEquals("2026-02-08 00:00:00", dateTimeFormat.format(ranges.get(1)[0]));
        Assert.assertEquals("2026-02-14 23:59:59", dateTimeFormat.format(ranges.get(1)[1]));

        Assert.assertEquals("2026-02-15 00:00:00", dateTimeFormat.format(ranges.get(2)[0]));
        Assert.assertEquals("2026-02-21 23:59:59", dateTimeFormat.format(ranges.get(2)[1]));

        Assert.assertEquals("2026-02-22 00:00:00", dateTimeFormat.format(ranges.get(3)[0]));
        Assert.assertEquals("2026-02-28 23:59:59", dateTimeFormat.format(ranges.get(3)[1]));
    }

    @Test
    public void setItEnd_shouldSetTo235959() throws Exception {
        MyDateUtils dateUtils = new MyDateUtils();
        Date source = dateTimeFormat.parse("2026-02-28 00:00:00");

        Date endOfDay = dateUtils.setItEnd(source);

        Assert.assertEquals("2026-02-28 23:59:59", dateTimeFormat.format(endOfDay));
    }
}
