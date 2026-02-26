//package com.tianye.hrsystem.mapper;
//
//import cn.hutool.core.date.DateTime;
//import com.tianye.hrsystem.base.BaseMapper;
//import com.tianye.hrsystem.common.BasePage;
//import com.tianye.hrsystem.entity.po.HrmInsuranceMonthEmpRecord;
//import com.tianye.hrsystem.entity.vo.SimpleHrmEmployeeVO;
//import com.tianye.hrsystem.modules.insurance.dto.QueryEmpInsuranceMonthBO;
//import com.tianye.hrsystem.modules.insurance.dto.QueryInsuranceRecordListBO;
//import com.tianye.hrsystem.modules.insurance.vo.QueryEmpInsuranceMonthVO;
//import com.tianye.hrsystem.modules.insurance.vo.QueryInsurancePageListVO;
//import org.apache.ibatis.annotations.Param;
//
//import java.util.List;
//
///**
// * <p>
// * 员工每月社保记录 Mapper 接口
// * </p>
// *
// * @author zhangzhiwei
// * @since 2020-05-26
// */
//public interface HrmInsuranceMonthEmpRecordMapper extends BaseMapper<HrmInsuranceMonthEmpRecord> {
//
//    BasePage<QueryEmpInsuranceMonthVO> queryEmpInsuranceMonth(BasePage<QueryEmpInsuranceMonthVO> parse, @Param("data") QueryEmpInsuranceMonthBO queryEmpInsuranceMonthBO);
//
//    List<SimpleHrmEmployeeVO> queryNoInsuranceEmp(@Param("iRecordId") Long iRecordId, @Param("dateTime") DateTime dateTime);
//
//    BasePage<QueryInsurancePageListVO> myInsurancePageList(BasePage<QueryInsurancePageListVO> parse, @Param("data") QueryInsuranceRecordListBO recordListBO);
//
//}
