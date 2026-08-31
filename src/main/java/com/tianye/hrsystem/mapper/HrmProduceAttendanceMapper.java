package com.tianye.hrsystem.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.bo.QueryMonthAttendanceBO;
import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.entity.vo.AdministrativeAttendanceReportMetricVO;
import com.tianye.hrsystem.entity.vo.QueryMonthAttendanceVO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public interface HrmProduceAttendanceMapper extends BaseMapper<HrmProduceAttendance>
{
    /**
     * 获取加班补贴和其他扣款数据
     */
    List<HrmProduceAttendance> getOvertimeAllowanceStatistics(HashMap<String,Object> params);

    Page<QueryMonthAttendanceVO> queryProduceAttendanceList(Page<QueryMonthAttendanceVO> parse,
                                                            @Param("data")QueryMonthAttendanceBO queryMonthAttendanceBO);

    List<AdministrativeAttendanceReportMetricVO> queryAdministrativeAttendanceReportMetrics(@Param("beginDate") Date beginDate,
                                                                                            @Param("endDate") Date endDate,
                                                                                            @Param("employeeIds") List<Long> employeeIds);
}
