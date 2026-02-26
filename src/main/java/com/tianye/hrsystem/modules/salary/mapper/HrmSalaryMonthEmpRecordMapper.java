package com.tianye.hrsystem.modules.salary.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.vo.QuerySalaryListVO;
import com.tianye.hrsystem.modules.salary.dto.QueryHistorySalaryDetailDto;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryPageListDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthEmpRecord;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryPageListVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 员工每月薪资记录 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface HrmSalaryMonthEmpRecordMapper extends BaseMapper<HrmSalaryMonthEmpRecord> {

    /**
     * 查询计薪人员id
     *
     * @return
     */
    List<Map<String, Object>> queryPaySalaryEmployeeList(@Param("endTime") LocalDate endTime, @Param("employeeIds") Collection<Long> dataAuthEmployeeIds);

    List<Long> queryPaySalaryEmployeeIdList(@Param("endTime") LocalDate endTime, @Param("employeeIds") Collection<Long> dataAuthEmployeeIds);

//    List<QuerySalaryPageListVO> querySalaryPageList(@Param("data") QuerySalaryPageListDto querySalaryPageListDto,
//                                                    @Param("employeeIds") Collection<Long> dataAuthEmployeeIds);

    BasePage<QuerySalaryPageListVO> querySalaryPageList(BasePage<QuerySalaryPageListVO> parse, @Param("data") QuerySalaryPageListDto querySalaryPageListBO,
                                                        @Param("employeeIds") Collection<Long> dataAuthEmployeeIds);

    List<QuerySalaryPageListVO> querySalaryMonthList( @Param("data") QuerySalaryPageListDto querySalaryPageListBO,
                                                      @Param("employeeIds") Collection<Long> dataAuthEmployeeIds);

    BasePage<QuerySalaryPageListVO> querySalaryPageListByRecordId(BasePage<QuerySalaryPageListVO> parse, @Param("data") QueryHistorySalaryDetailDto queryHistorySalaryDetailDto);

    List<Long> querysEmpRecordIds(@Param("data") QuerySalaryPageListDto querySalaryPageListBO,
                                  @Param("employeeIds") Collection<Long> dataAuthEmployeeIds);

    BasePage<QuerySalaryListVO> querySalaryRecord(BasePage<Object> parse, @Param("employeeId") Long employeeId);

    List<Long> queryEmployeeIds(@Param("sRecordId") Long sRecordId, @Param("employeeIds") Collection<Long> dataAuthEmployeeIds);
}
