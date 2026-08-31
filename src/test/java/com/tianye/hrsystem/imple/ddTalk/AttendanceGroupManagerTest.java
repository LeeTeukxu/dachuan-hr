package com.tianye.hrsystem.imple.ddTalk;

import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.model.HrmAttendanceGroup;
import com.tianye.hrsystem.model.HrmAttendanceGroupRelationDept;
import com.tianye.hrsystem.model.HrmAttendanceGroupRelationEmployee;
import com.tianye.hrsystem.model.HrmAttendanceShift;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRelationDeptRepository;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRelationEmployeeRepository;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRepository;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AttendanceGroupManagerTest {

    @Spy
    @InjectMocks
    private AttendanceGroupManager attendanceGroupManager;

    @Mock
    private hrmAttendanceGroupRepository groupRep;

    @Mock
    private hrmAttendanceGroupRelationDeptRepository depRelRep;

    @Mock
    private hrmAttendanceGroupRelationEmployeeRepository empRelRep;

    @Mock
    private hrmAttendanceShiftRepository shiftRep;

    static class NoopTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }

    @Test
    public void buildShiftSetting_shouldJoinSelectedClassIdsInOrder() {
        OapiAttendanceGetsimplegroupsResponse.AtClassVo classOne = new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        classOne.setClassId(101L);
        OapiAttendanceGetsimplegroupsResponse.AtClassVo classTwo = new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        classTwo.setClassId(202L);

        String shiftSetting = ReflectionTestUtils.invokeMethod(
                attendanceGroupManager,
                "buildShiftSetting",
                Arrays.asList(classOne, classTwo)
        );

        Assert.assertEquals("101,202", shiftSetting);
    }

    @Test
    public void buildAttendanceGroup_shouldPopulateLatestShiftSetting() {
        OapiAttendanceGetsimplegroupsResponse.AtClassVo classOne = new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        classOne.setClassId(301L);
        OapiAttendanceGetsimplegroupsResponse.AtClassVo classTwo = new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
        classTwo.setClassId(302L);

        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo groupVo = new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
        groupVo.setGroupId(9001L);
        groupVo.setGroupName("最新考勤组");
        groupVo.setIsDefault(Boolean.TRUE);
        groupVo.setSelectedClass(Arrays.asList(classOne, classTwo));
        groupVo.setDeptNameList(Collections.emptyList());

        HrmAttendanceGroup group = ReflectionTestUtils.invokeMethod(
                attendanceGroupManager,
                "buildAttendanceGroup",
                groupVo
        );

        Assert.assertEquals(Long.valueOf(9001L), group.getAttendanceGroupId());
        Assert.assertEquals("最新考勤组", group.getName());
        Assert.assertEquals(Integer.valueOf(1), group.getIsDefaultSetting());
        Assert.assertEquals("301,302", group.getShiftSetting());
    }

    @Test
    public void getAndSave_shouldNotReplaceLocalSnapshot_whenRemoteFetchFails() throws Exception {
        doThrow(new ApiException("您的企业本月api调用量已超过限制"))
                .when(attendanceGroupManager)
                .fetchCurrentSnapshot();

        try {
            attendanceGroupManager.GetAndSave();
            Assert.fail("Expected ApiException");
        } catch (ApiException expected) {
            Assert.assertEquals("您的企业本月api调用量已超过限制", expected.getMessage());
        }

        verify(groupRep, never()).deleteAll();
        verify(depRelRep, never()).deleteAll();
        verify(empRelRep, never()).deleteAll();
        verify(shiftRep, never()).deleteAll();
    }

    @Test
    public void getAndSave_shouldReplaceLocalSnapshot_afterSnapshotLoadedSuccessfully() throws Exception {
        AttendanceGroupManager.GroupSyncSnapshot snapshot = new AttendanceGroupManager.GroupSyncSnapshot();

        HrmAttendanceGroup group = new HrmAttendanceGroup();
        group.setAttendanceGroupId(1001L);
        snapshot.groups.add(group);

        HrmAttendanceShift shift = new HrmAttendanceShift();
        shift.setShiftId(2001L);
        shift.setGroupId(1001L);
        snapshot.shifts.add(shift);

        HrmAttendanceGroupRelationDept deptRelation = new HrmAttendanceGroupRelationDept();
        deptRelation.setAttendanceGroupId(1001L);
        snapshot.deptRelations.add(deptRelation);

        HrmAttendanceGroupRelationEmployee employeeRelation = new HrmAttendanceGroupRelationEmployee();
        employeeRelation.setAttendanceGroupId(1001L);
        snapshot.employeeRelations.add(employeeRelation);

        doReturn(snapshot).when(attendanceGroupManager).fetchCurrentSnapshot();
        // GetAndSave 经 TransactionTemplate 落库，测试注入空事务管理器
        ReflectionTestUtils.setField(attendanceGroupManager, "transactionTemplate",
                new TransactionTemplate(new NoopTransactionManager()));

        attendanceGroupManager.GetAndSave();

        InOrder inOrder = inOrder(groupRep, depRelRep, empRelRep, shiftRep);
        inOrder.verify(groupRep).deleteAll();
        inOrder.verify(depRelRep).deleteAll();
        inOrder.verify(empRelRep).deleteAll();
        inOrder.verify(shiftRep).deleteAll();
        inOrder.verify(groupRep).saveAll(snapshot.groups);
        inOrder.verify(depRelRep).saveAll(snapshot.deptRelations);
        inOrder.verify(empRelRep).saveAll(snapshot.employeeRelations);
        inOrder.verify(shiftRep).saveAll(snapshot.shifts);
    }
}
