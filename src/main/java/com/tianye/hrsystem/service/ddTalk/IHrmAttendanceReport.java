package com.tianye.hrsystem.service.ddTalk;

import com.taobao.api.ApiException;
import com.tianye.hrsystem.model.HrmAttendanceReportField;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: IHrmAttendanceReport
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年04月19日 23:32
 **/
public interface IHrmAttendanceReport {
    /**
     * create by: mmzs
     * description: TODO
     * create time:
     * <p>
     * 更新报表字段
     *
     * @return
     */
    List<HrmAttendanceReportField> UpdateReportFields() throws ApiException;
    /**
     * create by: mmzs
     * description: TODO
     * create time:  
     * 
     更新考勤的报表
     * @return 
     */
    void UpdateAttendanceReport(String userId, Long empId, Date begin, Date end) throws ApiException;

    void UpdateAttendanceReportQuick(String userId, Long empId, Date begin, Date end) throws ApiException;
    /**
     * create by: mmzs
     * description: TODO
     * create time:  
     * 

     * @return 
     */
    void UpdateHolidayReport(String userId, Long empId, Date begin, Date end) throws ApiException;

    void UpdateHolidayReportQuick(String userId, Long empId, Date begin, Date end) throws ApiException;
    void ProcessOne(String CompanyID);
}
