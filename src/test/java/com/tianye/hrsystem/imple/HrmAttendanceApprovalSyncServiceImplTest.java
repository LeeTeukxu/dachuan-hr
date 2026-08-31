package com.tianye.hrsystem.imple;

import com.dingtalk.api.response.OapiAttendanceGetupdatedataResponse;
import com.tianye.hrsystem.model.tbattendanceuser;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HrmAttendanceApprovalSyncServiceImplTest {

    @Test
    public void matchesApprovalTypes_shouldMatchDingTalkTagNameVariants() throws Exception {
        HrmAttendanceApprovalSyncServiceImpl service = new HrmAttendanceApprovalSyncServiceImpl();

        Assert.assertTrue(invokeMatches(service, approval(0L, "补卡申请", null), "misscard"));
        Assert.assertTrue(invokeMatches(service, approval(0L, "加班申请", null), "overtime"));
        Assert.assertTrue(invokeMatches(service, approval(0L, "请假审批", "病假"), "leave"));
        Assert.assertTrue(invokeMatches(service, approval(0L, "外出审批", null), "travel"));
        Assert.assertTrue(invokeMatches(service, approval(0L, "出差申请", null), "travel"));
    }

    @Test
    public void matchesApprovalTypes_shouldMatchSubtypeKeywordsWhenTagNameMissing() throws Exception {
        HrmAttendanceApprovalSyncServiceImpl service = new HrmAttendanceApprovalSyncServiceImpl();

        Assert.assertTrue(invokeMatches(service, approval(0L, null, "补卡"), "misscard"));
        Assert.assertTrue(invokeMatches(service, approval(null, "", "事假"), "leave"));
        Assert.assertTrue(invokeMatches(service, approval(null, "", "调休"), "leave"));
    }

    @Test
    public void matchesApprovalTypes_shouldStillRespectUnknownApprovalTypes() throws Exception {
        HrmAttendanceApprovalSyncServiceImpl service = new HrmAttendanceApprovalSyncServiceImpl();

        Assert.assertFalse(invokeMatches(service, approval(9L, "通用审批", "自定义"), "leave"));
        Assert.assertFalse(invokeMatches(service, approval(9L, "通用审批", "自定义"), "travel"));
        Assert.assertTrue(invokeMatches(service, approval(9L, "通用审批", "自定义"), "all"));
        Assert.assertFalse(invokeMatchesWithList(service, approval(1L, "加班申请", null), Collections.emptyList()));
    }

    @Test
    public void resolveProcessCodes_shouldUseEmployeeVisibleTemplatesWithoutManageableTemplatePrequery() throws Exception {
        ProcessCodeResolutionTestService service = new ProcessCodeResolutionTestService();
        service.userVisibleTemplates = Collections.singletonList(template("加班申请", "PROC-OT-USER"));

        Assert.assertEquals(Collections.singletonList("PROC-OT-USER"),
                service.resolveProcessCodes("mock-token", "ding-101", Collections.singletonList("overtime")));
        Assert.assertEquals(1, service.userVisibleCallCount);
        Assert.assertEquals(Collections.singletonList("ding-101"), service.userVisibleUserIds);
    }

    @Test
    public void resolveProcessCodes_shouldNotFailWhenManageableTemplateUserWouldFail() throws Exception {
        ProcessCodeResolutionTestService service = new ProcessCodeResolutionTestService();
        service.userVisibleTemplates = Collections.singletonList(template("请假审批", "PROC-LEAVE-USER"));

        Assert.assertEquals(Collections.singletonList("PROC-LEAVE-USER"),
                service.resolveProcessCodes("mock-token", "ding-101", Collections.singletonList("leave")));
        Assert.assertEquals(1, service.userVisibleCallCount);
    }

    @Test
    public void resolveProcessCodes_shouldIgnoreManageableTemplateQueryFailures() throws Exception {
        ProcessCodeResolutionTestService service = new ProcessCodeResolutionTestService();
        service.userVisibleTemplates = Collections.singletonList(template("加班审批", "PROC-OT-USER"));

        Assert.assertEquals(Collections.singletonList("PROC-OT-USER"),
                service.resolveProcessCodes("mock-token", "ding-101", Collections.singletonList("overtime")));
        Assert.assertEquals(Collections.singletonList("ding-101"), service.userVisibleUserIds);
    }

    @Test
    public void summarizeResolvedUsers_shouldIncludeEmployeeIdUserNameAndDingTalkUserId() throws Exception {
        HrmAttendanceApprovalSyncServiceImpl service = new HrmAttendanceApprovalSyncServiceImpl();
        List<tbattendanceuser> users = new ArrayList<>();
        users.add(attendanceUser(1831601326890434651L, "185339132332994951", "范小艳"));
        users.add(attendanceUser(1831601326890434600L, "185339132832825925", "苏中心"));

        Assert.assertEquals(
                "[1831601326890434651/范小艳/185339132332994951, 1831601326890434600/苏中心/185339132832825925]",
                invokeSummarizeResolvedUsers(service, users, 20)
        );
    }

    @Test
    public void translateWorkflowErrorMessage_shouldExposeDingTalkFrequencyLimit() throws Exception {
        HrmAttendanceApprovalSyncServiceImpl service = new HrmAttendanceApprovalSyncServiceImpl();

        Assert.assertEquals(
                "钉钉审批接口触发限流，请稍后重试或缩小员工范围",
                invokeTranslateWorkflowErrorMessage(service,
                        "topapi/processinstance/listids调用失败, errcode=88, errmsg=isv.limitedFrequency")
        );
    }

    private boolean invokeMatches(HrmAttendanceApprovalSyncServiceImpl service,
                                  OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo approval,
                                  String... approvalTypes) throws Exception {
        return invokeMatchesWithList(service, approval, Arrays.asList(approvalTypes));
    }

    private boolean invokeMatchesWithList(HrmAttendanceApprovalSyncServiceImpl service,
                                          OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo approval,
                                          java.util.List<String> approvalTypes) throws Exception {
        Method method = HrmAttendanceApprovalSyncServiceImpl.class.getDeclaredMethod(
                "matchesApprovalTypes",
                OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo.class,
                java.util.List.class
        );
        method.setAccessible(true);
        return (Boolean) method.invoke(service, approval, approvalTypes);
    }

    private OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo approval(Long bizType, String tagName, String subType) {
        OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo approval =
                new OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo();
        approval.setBizType(bizType);
        approval.setTagName(tagName);
        approval.setSubType(subType);
        return approval;
    }

    private ProcessCodeResolutionTestService.ProcessTemplateMetaEx template(String name, String code) {
        return new ProcessCodeResolutionTestService.ProcessTemplateMetaEx(name, code);
    }

    private String invokeSummarizeResolvedUsers(HrmAttendanceApprovalSyncServiceImpl service,
                                                List<tbattendanceuser> users,
                                                int limit) throws Exception {
        Method method = HrmAttendanceApprovalSyncServiceImpl.class.getDeclaredMethod(
                "summarizeResolvedUsers",
                List.class,
                int.class
        );
        method.setAccessible(true);
        return (String) method.invoke(service, users, limit);
    }

    private String invokeTranslateWorkflowErrorMessage(HrmAttendanceApprovalSyncServiceImpl service,
                                                       String rawMessage) throws Exception {
        Method method = HrmAttendanceApprovalSyncServiceImpl.class.getDeclaredMethod(
                "translateWorkflowErrorMessage",
                String.class
        );
        method.setAccessible(true);
        return (String) method.invoke(service, rawMessage);
    }

    private tbattendanceuser attendanceUser(Long empId, String userId, String userName) {
        tbattendanceuser user = new tbattendanceuser();
        user.setEmpId(empId);
        user.setUserId(userId);
        user.setUserName(userName);
        return user;
    }

    private static class ProcessCodeResolutionTestService extends HrmAttendanceApprovalSyncServiceImpl {
        private int userVisibleCallCount;
        private java.util.List<ProcessTemplateMetaEx> userVisibleTemplates = Collections.emptyList();
        private java.util.List<String> userVisibleUserIds = new java.util.ArrayList<>();

        @Override
        protected java.util.List<ProcessTemplateMeta> fetchUserVisibleProcessTemplates(String token, String userId) {
            userVisibleCallCount++;
            userVisibleUserIds.add(userId);
            return new java.util.ArrayList<>(userVisibleTemplates);
        }

        private static class ProcessTemplateMetaEx extends ProcessTemplateMeta {
            private ProcessTemplateMetaEx(String templateName, String processCode) {
                super(templateName, processCode);
            }
        }
    }
}
