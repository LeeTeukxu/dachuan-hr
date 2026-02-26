package com.tianye.hrsystem.modules.insurance.mapper;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.modules.insurance.dto.QueryEmpInsuranceMonthBO;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.vo.QueryEmpInsuranceMonthVO;
import com.tianye.hrsystem.modules.insurance.vo.QueryInsurancePageListVO;
import com.tianye.hrsystem.modules.insurance.dto.QueryInsuranceRecordListBO;
import com.tianye.hrsystem.modules.insurance.vo.SimpleHrmEmployeeVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 员工每月社保记录 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface HrmInsuranceMonthEmpRecordMapper extends BaseMapper<HrmInsuranceMonthEmpRecord> {

    Page<QueryEmpInsuranceMonthVO> queryEmpInsuranceMonth(Page<QueryEmpInsuranceMonthVO> parse, @Param("data") QueryEmpInsuranceMonthBO queryEmpInsuranceMonthBO);

    List<SimpleHrmEmployeeVO> queryNoInsuranceEmp(@Param("iRecordId") Long iRecordId, @Param("dateTime") DateTime dateTime);

    Page<QueryInsurancePageListVO> myInsurancePageList(Page<QueryInsurancePageListVO> parse, @Param("data") QueryInsuranceRecordListBO recordListBO);

}
