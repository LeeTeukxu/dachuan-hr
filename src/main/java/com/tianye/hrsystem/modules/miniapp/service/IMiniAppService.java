package com.tianye.hrsystem.modules.miniapp.service;

import com.tianye.hrsystem.modules.miniapp.vo.MiniAppLoginVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppMonthScheduleVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppDayShiftVO;
import com.tianye.hrsystem.modules.miniapp.vo.CompanyOptionVO;

import java.util.Date;

public interface IMiniAppService {

    /** 微信 code 登录；未绑定 openid 时返回首次绑定 ticket 和公司列表。 */
    MiniAppLoginVO login(String code) throws Exception;

    /** 首次登录按公司、姓名、证件号码核验员工并绑定 openid。 */
    MiniAppLoginVO bindEmployee(String ticket, String companyId,
                                String employeeName, String idNumber) throws Exception;

    /** openid 跨公司多条时，用户选定公司后返回对应员工 token */
    MiniAppLoginVO bindCompany(String ticket, String companyId) throws Exception;

    /** 月度排班（月历数据），每员工视角，含待审批标记 */
    MiniAppMonthScheduleVO queryMonthSchedule(Long employeeId, String month) throws Exception;

    /** 某天排班详情 + 该日我的申请（workDate yyyy-MM-dd） */
    MiniAppDayShiftVO queryDayDetail(Long employeeId, Date workDate) throws Exception;

    /** 是否上级角色（存在未删除的直属下级，parent_id 指向自己） */
    boolean isSupervisor(Long employeeId);

    /** 审批权限校验：当前员工必须是该申请单申请人的直属上级，否则抛异常 */
    void checkApprovalPermission(Long approverEmployeeId, Long applicationId) throws Exception;

    /** 切换公司：根据当前员工 openid 返回可选公司列表 */
    java.util.List<CompanyOptionVO> switchCompany(Long employeeId) throws Exception;

    /** 确认切换公司：按 openid 定位目标公司员工并返回新 token */
    MiniAppLoginVO confirmSwitch(Long employeeId, String companyId) throws Exception;
}
