//package com.tianye.hrsystem.mapper;
//
//import com.tianye.hrsystem.base.BaseMapper;
//import com.tianye.hrsystem.common.BasePage;
//import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
//import org.apache.ibatis.annotations.Param;
//
//import java.math.BigDecimal;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//
///**
// * <p>
// * 每月薪资记录 Mapper 接口
// * </p>
// *
// * @author zhangzhiwei
// * @since 2020-05-26
// */
//public interface HrmSalaryMonthRecordMapper extends BaseMapper<HrmSalaryMonthRecord> {
//
//    BigDecimal queryBeforeTaxTotalSalary(@Param("employeeId") Integer employeeId, @Param("year") Integer year, @Param("month") Integer month);
//
//    /**
//     * 查询每月薪资合计(生成历史薪资使用)
//     *
//     * @param sRecordId
//     * @return
//     */
//    Map<String, Object> queryMonthSalaryCount(Long sRecordId);
//
//    BasePage<QueryHistorySalaryListVO> queryHistorySalaryList(BasePage<Object> parse, @Param("data") QueryHistorySalaryListBO queryHistorySalaryListBO,
//                                                              @Param("employeeIds") Collection<Long> employeeIds);
//
//    List<Map<String, Object>> querySalaryOptionCount(String sRecordId);
//
//    List<Map<String, Object>> querySalaryByIds(@Param("sEmpRecordIds") List<Long> sEmpRecordIds, @Param("sRecordId") Long sRecordId);
//
//    List<Long> queryDeleteEmpRecordIds(Long sRecordId);
//
//    QueryHistorySalaryDetailVO queryHistorySalaryDetail(@Param("sRecordId") Long sRecordId, @Param("employeeIds") Collection<Long> employeeIds);
//
//}
