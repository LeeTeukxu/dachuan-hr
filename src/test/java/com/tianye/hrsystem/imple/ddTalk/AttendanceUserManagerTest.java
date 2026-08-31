package com.tianye.hrsystem.imple.ddTalk;

import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.tbattendanceuser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AttendanceUserManagerTest {

    private final AttendanceUserManager attendanceUserManager = new AttendanceUserManager();

    @Test
    @SuppressWarnings("unchecked")
    public void collectStaleMappingsToDelete_shouldDropRowsNotInCurrentSyncedResultForActiveEmployee() {
        Long empId = 1831601326890434567L;

        tbattendanceuser currentUser = buildAttendanceUser(204, empId, "02642841571826358761");
        tbattendanceuser staleUser = buildAttendanceUser(207, empId, "010013312008850281");
        HrmEmployee employee = buildEmployee(empId, 1, 0, "02642841571826358761");

        List<tbattendanceuser> staleUsers = (List<tbattendanceuser>) ReflectionTestUtils.invokeMethod(
                attendanceUserManager,
                "collectStaleMappingsToDelete",
                Arrays.asList(currentUser, staleUser),
                Collections.singletonList(currentUser),
                Collections.singletonMap(empId, employee),
                Collections.emptySet(),
                new LinkedHashSet<>(Collections.singletonList("02642841571826358761")),
                true
        );

        Assert.assertNotNull(staleUsers);
        Assert.assertEquals(1, staleUsers.size());
        Assert.assertEquals(Integer.valueOf(207), staleUsers.get(0).getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectStaleMappingsToDelete_shouldDropExplicitlyInvalidRowsEvenWithoutCurrentDingTalkUserId() {
        Long empId = 2033769831940079640L;

        tbattendanceuser firstInvalid = buildAttendanceUser(202, empId, "133930484236558157");
        tbattendanceuser secondInvalid = buildAttendanceUser(210, empId, "133930484266042");
        HrmEmployee employee = buildEmployee(empId, 1, 0, null);
        Set<String> invalidUserIds = new LinkedHashSet<>(Collections.singletonList("133930484236558157"));

        List<tbattendanceuser> staleUsers = (List<tbattendanceuser>) ReflectionTestUtils.invokeMethod(
                attendanceUserManager,
                "collectStaleMappingsToDelete",
                Arrays.asList(firstInvalid, secondInvalid),
                Collections.emptyList(),
                Collections.singletonMap(empId, employee),
                invalidUserIds,
                Collections.emptySet(),
                false
        );

        Assert.assertNotNull(staleUsers);
        Assert.assertEquals(1, staleUsers.size());
        Assert.assertEquals(Integer.valueOf(202), staleUsers.get(0).getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectStaleMappingsToDelete_shouldDropOrphanedRowsWhenEmployeeIsActiveButRemoteNoLongerListsThem() {
        Long empId = 2033769831940079640L;

        tbattendanceuser orphan = buildAttendanceUser(210, empId, "133930484266042");
        HrmEmployee employee = buildEmployee(empId, 1, 0, null);

        List<tbattendanceuser> staleUsers = (List<tbattendanceuser>) ReflectionTestUtils.invokeMethod(
                attendanceUserManager,
                "collectStaleMappingsToDelete",
                Collections.singletonList(orphan),
                Collections.emptyList(),
                Collections.singletonMap(empId, employee),
                Collections.emptySet(),
                new LinkedHashSet<>(Collections.singletonList("other-user")),
                false
        );

        Assert.assertNotNull(staleUsers);
        Assert.assertEquals(1, staleUsers.size());
        Assert.assertEquals(Integer.valueOf(210), staleUsers.get(0).getId());
    }

    private tbattendanceuser buildAttendanceUser(int id, Long empId, String userId) {
        tbattendanceuser user = new tbattendanceuser();
        user.setId(id);
        user.setEmpId(empId);
        user.setUserId(userId);
        user.setCreateTime(new Date());
        return user;
    }

    private HrmEmployee buildEmployee(Long employeeId, Integer entryStatus, Integer isDel, String dingtalkUserId) {
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeId(employeeId);
        employee.setEntryStatus(entryStatus);
        employee.setIsDel(isDel);
        ReflectionTestUtils.setField(employee, "dingtalkUserId", dingtalkUserId);
        return employee;
    }
}
