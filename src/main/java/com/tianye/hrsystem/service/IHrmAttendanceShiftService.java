package com.tianye.hrsystem.service;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryAttendanceDailyDetailBO;
import com.tianye.hrsystem.entity.bo.QueryHrmAttendanceShiftBO;
import com.tianye.hrsystem.entity.bo.SetAttendanceShiftBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceShift;
import com.tianye.hrsystem.entity.vo.HrmAttendanceShiftVO;
import com.tianye.hrsystem.entity.vo.OperationLog;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 班次表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-12
 */
public interface IHrmAttendanceShiftService extends BaseService<HrmAttendanceShift> {
    /**
     * 查询考勤班次列表
     *
     * @param queryHrmAttendanceShiftBO
     * @return
     */
    BasePage<HrmAttendanceShiftVO> queryAttendanceShiftPageList(QueryHrmAttendanceShiftBO queryHrmAttendanceShiftBO);

    /**
     * 添加或修改班次
     *
     * @param attendanceShift
     */
    void setAttendanceShift(SetAttendanceShiftBO attendanceShift);

    /**
     * 校验考勤班次名称
     *
     * @param queryHrmAttendanceShiftBO
     * @return
     */
    void verifyAttendanceShiftName(QueryHrmAttendanceShiftBO queryHrmAttendanceShiftBO);

    /**
     * 查询考勤时间
     *
     * @param shiftSetting 考勤班次
     * @return
     */
    Map<Integer, Map<String, Object>> queryAttendanceDate(String shiftSetting);

    /**
     * 通过班次id删除班次
     *
     * @param attendanceShiftId
     */
    OperationLog deleteAttendanceShift(Long attendanceShiftId);

    /**
     * 查询员工指定日期的排班数据
     */
    HrmAttendanceShiftVO getEmpHrmAttendanceShift(QueryAttendanceDailyDetailBO dailyDetailBO);

}
