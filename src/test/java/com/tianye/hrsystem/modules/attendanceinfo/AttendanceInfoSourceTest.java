package com.tianye.hrsystem.modules.attendanceinfo;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class AttendanceInfoSourceTest {

    @Test
    public void attendanceInfoSources_shouldNotExposeSystemDepartmentType() throws Exception {
        for (String sourcePath : Arrays.asList(
                "src/main/java/com/tianye/hrsystem/modules/attendanceinfo/bo/QueryAttendanceInfoBO.java",
                "src/main/java/com/tianye/hrsystem/modules/attendanceinfo/entity/HrmAttendanceInfo.java",
                "src/main/java/com/tianye/hrsystem/modules/attendanceinfo/vo/QueryAttendanceInfoVO.java",
                "src/main/java/com/tianye/hrsystem/modules/attendanceinfo/mapper/HrmAttendanceInfoMapper.java",
                "src/main/resources/mapper/HrmAttendanceInfoMapper.xml")) {
            String source = new String(Files.readAllBytes(Paths.get(sourcePath)), StandardCharsets.UTF_8);

            Assert.assertFalse("考勤天数配置不得再暴露行政/生产体系类型字段: " + sourcePath,
                    source.contains("dept" + "Type"));
            Assert.assertFalse("考勤天数配置不得再读取行政/生产体系类型列: " + sourcePath,
                    source.contains("dept_" + "type"));
        }
    }
}
