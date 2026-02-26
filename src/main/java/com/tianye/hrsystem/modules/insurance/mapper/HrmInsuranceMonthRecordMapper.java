package com.tianye.hrsystem.modules.insurance.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthRecord;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsurancePageListBO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsurancePageListVO;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsuranceRecordListBO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsuranceRecordListVO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 每月社保记录 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface HrmInsuranceMonthRecordMapper extends BaseMapper<HrmInsuranceMonthRecord> {

    /**
     * 查询参保员工id
     *
     * @return
     */
    List<Map<String, Long>> queryInsuranceEmployee();

    @InterceptorIgnore(tenantLine = "true")
    Page<QueryInsuranceRecordListVO> queryInsuranceRecordList(Page<QueryInsuranceRecordListVO> parse,
                                                              @Param("data") QueryInsuranceRecordListBO recordListBO);

    Page<QueryInsurancePageListVO> queryInsurancePageList(Page<QueryInsurancePageListVO> parse, @Param("data") QueryInsurancePageListBO queryInsurancePageListBO);

    @InterceptorIgnore(tenantLine = "true")
    QueryInsuranceRecordListVO queryInsuranceRecord(@Param("iRecordId") String iRecordId,
                                                    @Param("employeeIds") Collection<Long> employeeIds);

    List<Long> queryDeleteEmpRecordIds(Long iRecordId);

    QueryInsuranceRecordListVO queryNoEmpInsuranceRecord(String iRecordId);
}
