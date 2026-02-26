package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryAttendanceEmpMonthRecordBO;
import com.tianye.hrsystem.entity.bo.QueryAttendanceOutCardBO;
import com.tianye.hrsystem.entity.bo.QueryAttendancePageBO;
import com.tianye.hrsystem.entity.bo.QueryNotesStatusBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceClock;
import com.tianye.hrsystem.entity.vo.HrmEmpNightShiftSubsidyVo;
import com.tianye.hrsystem.entity.vo.QueryAttendanceEmpMonthRecordVO;
import com.tianye.hrsystem.entity.vo.QueryAttendancePageVO;
import com.tianye.hrsystem.entity.vo.QueryEmployeeAttendanceVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Handler;

/**
 * <p>
 * 打卡记录表 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-12-07
 */
public interface HrmAttendanceClockMapper extends BaseMapper<HrmAttendanceClock> {

    BasePage<QueryAttendancePageVO> queryPageList(BasePage<QueryAttendancePageVO> parse,
                                                  @Param("data") QueryAttendancePageBO attendancePageBo, @Param("employeeIds") Collection<Long> employeeIds);

    List<HrmAttendanceClock> queryClockListByTime(@Param("time") LocalDate time, @Param("employeeIds") Collection<Long> employeeIds);

    Set<String> queryClockStatusList(@Param("data") QueryNotesStatusBO queryNotesStatusBo,
            @Param("employeeIds") Collection<Long> employeeIds);

    BasePage<QueryAttendancePageVO> queryMyPageList(BasePage<Object> parse, @Param("employeeId") Long employeeId);

    BasePage<QueryEmployeeAttendanceVO> queryAttendanceEmpPageList(BasePage<QueryAttendanceEmpMonthRecordVO> page,
                                                                   @Param("data") QueryAttendanceEmpMonthRecordBO queryAttendanceEmpMonthRecordBo, @Param("employeeIds") Collection<Long> employeeIds);

    Integer queryEmpAttendanceOverTimeCountDays(@Param("times") List<LocalDate> times, @Param("employeeId") Long employeeId);

    BasePage<Map<String, Object>> queryOutCardPageList(BasePage<Map<String, Object>> parse, @Param("data") QueryAttendanceOutCardBO queryAttendanceOutCardBo);

    /**
     * 根据条件查询接口
     *
     * @param clockType     打卡类型
     * @param startDateTime 开始时间
     * @param endDateTime   结束时间
     * @param employeeIds   员工列表
     * @param clockStage    打卡阶段
     * @return
     */
    List<HrmAttendanceClock> queryAttendanceClockList(@Param("clockType") Integer clockType, @Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime, @Param("employeeIds") List<Long> employeeIds, @Param("clockStage") Integer clockStage);


    /**
     * 获取考勤时间内有夜班补贴的数据
     */

    List<HrmEmpNightShiftSubsidyVo> getEmpNightShiftSubsidyList(HashMap<String,Object> params);

}
