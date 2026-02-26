package com.tianye.hrsystem.modules.attendanceinfo.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.attendanceinfo.bo.QueryAttendanceInfoBO;
import com.tianye.hrsystem.modules.attendanceinfo.entity.HrmAttendanceInfo;
import com.tianye.hrsystem.modules.attendanceinfo.vo.QueryAttendanceInfoVO;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;

public interface HrmAttendanceInfoMapper extends BaseMapper<HrmAttendanceInfo> {

    List<HrmAttendanceInfo> queryInfo(HashMap<String, Object> params);

    List<QueryAttendanceInfoVO> queryAttendanceInfoByParam(@Param("data") QueryAttendanceInfoBO queryAttendanceInfoBO);
}
