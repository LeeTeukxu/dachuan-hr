package com.tianye.hrsystem.service;

import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentsBO;
import com.tianye.hrsystem.entity.vo.WorkPlanCustomShiftOptionVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayAssignmentsVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.entity.vo.WorkPlanImportPreviewVO;
import com.tianye.hrsystem.entity.vo.WorkPlanSubmitProgressVO;
import com.tianye.hrsystem.entity.vo.WorkPlanSubmitResultVO;
import com.tianye.hrsystem.model.UserObject;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import com.tianye.hrsystem.model.tbplanlist;

import java.io.File;
import java.util.Date;
import java.util.List;

public interface IWorkPlanService {
    WorkPlanSubmitResultVO AddAll(List<tbplanlist> planList)throws Exception;
    WorkPlanImportPreviewVO previewImportExcel(MultipartFile file) throws Exception;
    File createImportTemplateExcel() throws Exception;
    void RemoveAll(List<Integer> IDArray) throws Exception;
    PageObject<tbplanlist> getMaxDate(Integer pageSize, Integer pageNum, String sortField, String sortOrder);
    PageObject<tbplanlist> loadBySelectedDate(Date selectedDate, boolean loadLast, Integer pageSize, Integer pageNum,
                                              String sortField, String sortOrder);
    WorkPlanSubmitProgressVO querySubmitProgress(String taskId);
    WorkPlanEmployeeDayShiftVO queryEmployeeDayShift(Long employeeId, Date workDate) throws Exception;
    WorkPlanEmployeeDayAssignmentsVO queryEmployeeDayAssignments(Long employeeId, Date workDate) throws Exception;
    WorkPlanEmployeeDayAssignmentsVO saveEmployeeDayAssignments(SaveWorkPlanEmployeeDayAssignmentsBO request) throws Exception;
    void removeEmployeeDayShift(Long employeeId, Date workDate) throws Exception;
    WorkPlanEmployeeDayShiftVO saveEmployeeDayCustomShift(Long employeeId, Date workDate,
                                                          String customStart, String customEnd,
                                                          String customShiftPeriod) throws Exception;
    WorkPlanEmployeeDayShiftVO saveEmployeeDayShift(Long employeeId, Date workDate,
                                                    String shiftType, String customStart, String customEnd,
                                                    String customShiftPeriod) throws Exception;
    WorkPlanEmployeeDayShiftVO saveEmployeeDayShift(Long employeeId, Date workDate,
                                                    String shiftType, String customStart, String customEnd,
                                                    String customShiftPeriod, Boolean customContinuousShift) throws Exception;
    WorkPlanEmployeeDayShiftVO saveEmployeeDayShift(Long employeeId, Date workDate,
                                                    String shiftType, String customStart, String customEnd,
                                                    String customShiftPeriod, Boolean customContinuousShift,
                                                    String restShiftType) throws Exception;
    WorkPlanEmployeeDayShiftVO saveEmployeeDayShift(Long employeeId, Date workDate,
                                                    String shiftType, String customStart, String customEnd,
                                                    String customShiftPeriod, Boolean customContinuousShift,
                                                    Boolean customContinuousShiftExplicit,
                                                    String restShiftType) throws Exception;
    void fillCustomShiftMeta(List<tbplanlist> planList) throws Exception;
    List<WorkPlanCustomShiftOptionVO> queryCustomShiftOptions() throws Exception;

    List<UserObject>getUsers(String CompanyID) throws Exception;
    List<UserObject> getUsersForDisplay(String companyId) throws Exception;
    List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getAllGroups() throws Exception;
    List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getAllGroupsForDisplay() throws Exception;
}
