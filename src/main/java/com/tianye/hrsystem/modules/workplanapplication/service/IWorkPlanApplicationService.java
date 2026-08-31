package com.tianye.hrsystem.modules.workplanapplication.service;

import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.model.HrmWorkplanApplication;

import java.util.Date;
import java.util.List;

/**
 * 排班修改申请·审批服务（后端单一实现，供小程序 /mp/application/* 与 hr_web /workPlanApplication 共用）
 */
public interface IWorkPlanApplicationService {

    HrmWorkplanApplication submit(Long employeeId, Date workDate, String shiftType,
                                  String customStart, String customEnd,
                                  String customShiftPeriod, Boolean customContinuousShift,
                                  String restShiftType, String remark) throws Exception;

    List<HrmWorkplanApplication> listMy(Long employeeId, String status);

    void cancel(Long employeeId, Long applicationId) throws Exception;

    /** 供审批时校验申请单存在 */
    HrmWorkplanApplication findById(Long applicationId) throws Exception;

    List<HrmWorkplanApplication> listToApprove(Long approverEmployeeId, String scope);

    /** 同意则写 tbplanlist 并标记当前生效；驳回填原因 */
    WorkPlanEmployeeDayShiftVO approve(Long applicationId, String action, String reason) throws Exception;

    List<HrmWorkplanApplication> listAll(String status, Date begin, Date end);

    WorkPlanEmployeeDayShiftVO queryEmployeeDayShift(Long employeeId, Date workDate) throws Exception;
}