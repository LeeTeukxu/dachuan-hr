package com.tianye.hrsystem.mapper;


import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.bo.QueryAttendanceDailyDetailBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceShift;
import com.tianye.hrsystem.entity.vo.HrmAttendanceShiftVO;

import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * 班次表 Mapper 接口
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-12
 */
public interface HrmAttendanceShiftMapper extends BaseMapper<HrmAttendanceShift>
{
    /**
     * 查询员工某日的班次
     * @param dailyDetailBO
     * @return
     */
    HrmAttendanceShiftVO getEmpHrmAttendanceShift(HashMap<String,Object> params);

    /**
     * 批量查询所有员工在指定日期范围内的排班时长
     */
    List<HrmAttendanceShiftVO> getEmpHrmAttendanceShiftBatch(HashMap<String,Object> params);
}
