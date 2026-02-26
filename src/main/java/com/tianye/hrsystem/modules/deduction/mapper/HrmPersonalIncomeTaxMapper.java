package com.tianye.hrsystem.modules.deduction.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.deduction.bo.QueryPersonalIncomeTaxBO;
import com.tianye.hrsystem.modules.deduction.entity.HrmPersonalIncomeTax;
import com.tianye.hrsystem.modules.deduction.vo.QueryPersonalIncomeTaxVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public interface HrmPersonalIncomeTaxMapper extends BaseMapper<HrmPersonalIncomeTax> {

    Page<QueryPersonalIncomeTaxVO> queryRemainingVacationList(Page<QueryPersonalIncomeTaxVO> parse,
                                                              @Param("data") QueryPersonalIncomeTaxBO queryPersonalIncomeTaxBO);

    /**
     * 获取员工截止某月的累计个税数据
     * @param employeeId
     * @param year
     * @param month
     * @return
     */
    QueryPersonalIncomeTaxVO getQueryPersonalIncomeTax(HashMap<String,Object> params);


    /**
     * 更新员工的累计个税数据
     */
    Integer updateEmployeeIncomeTax(HrmPersonalIncomeTax tax);


    /**
     * 根据条件删除个税累计数据
     * @param params
     * @return
     */
    Integer deleteByParams(HashMap<String,Object> params);

    /**
     * 获取员工某年度累计收入（取该年截止月份最大的一条记录）
     * @param employeeId 员工ID
     * @param year 年度
     * @return 累计收入，无记录时返回 null
     */
    BigDecimal getAccumulatedIncomeByEmployeeAndYear(@Param("employeeId") Long employeeId, @Param("year") Integer year);

    /**
     * 批量查询员工某月个税累计数据
     */
    List<QueryPersonalIncomeTaxVO> queryPersonalIncomeTaxByEmployeeIds(@Param("employeeIds") Collection<Long> employeeIds,
                                                                        @Param("year") Integer year,
                                                                        @Param("month") Integer month);

    /**
     * 批量查询员工某年度累计收入（取该年截止月份最大的一条记录）
     */
    List<QueryPersonalIncomeTaxVO> queryAccumulatedIncomeByEmployeeIdsAndYear(@Param("employeeIds") Collection<Long> employeeIds,
                                                                               @Param("year") Integer year);
}
