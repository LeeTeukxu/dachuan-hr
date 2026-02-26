package com.tianye.hrsystem.modules.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.modules.salary.dto.QueryHistorySalaryListDto;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryMonthRecordDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import com.tianye.hrsystem.modules.salary.vo.QueryHistorySalaryDetailVO;
import com.tianye.hrsystem.modules.salary.vo.QueryHistorySalaryListVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryMonthRecordVO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 每月薪资记录 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface HrmSalaryMonthRecordMapper extends BaseMapper<HrmSalaryMonthRecord> {

//    BigDecimal queryBeforeTaxTotalSalary(@Param("employeeId") Integer employeeId, @Param("year") Integer year, @Param("month") Integer month);
//
//    /**
//     * 查询每月薪资合计(生成历史薪资使用)
//     *
//     * @param sRecordId
//     * @return
//     */

    Map<String, Object> queryMonthSalaryCount(Long sRecordId);

    Page<QueryHistorySalaryListVO> queryHistorySalaryList(Page<Object> parse, @Param("data") QueryHistorySalaryListDto queryHistorySalaryListDto);
//
//    List<Map<String, Object>> querySalaryOptionCount(String sRecordId);

    List<Map<String, Object>> querySalaryByIds(@Param("sEmpRecordIds") List<Long> sEmpRecordIds, @Param("sRecordId") Long sRecordId);
//
    List<Long> queryDeleteEmpRecordIds(Long sRecordId);
//
    QueryHistorySalaryDetailVO queryHistorySalaryDetail(@Param("sRecordId") Long sRecordId);

    HrmSalaryMonthRecord getLastSalaryRecord();

    HrmSalaryMonthRecord querySalaryRecordById(Long sRecordId);

    Page<QuerySalaryMonthRecordVO> querySlipEmployeePageList(@Param("page") Page<HrmSalaryMonthRecord> page, @Param("data") QuerySalaryMonthRecordDto querySalaryMonthRecordDto);
}
