package com.tianye.hrsystem.modules.salary.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.modules.salary.dto.QuerySalarySlipListDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalarySlip;
import com.tianye.hrsystem.modules.salary.vo.QuerySalarySlipListVO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 工资条 Mapper 接口
 * </p>
 *
 * @author hmb
 * @since 2020-11-03
 */
public interface HrmSalarySlipMapper extends BaseMapper<HrmSalarySlip> {

    Page<QuerySalarySlipListVO> querySalarySlipList(Page<QuerySalarySlipListVO> parse, @Param("data") QuerySalarySlipListDto querySalarySlipListBO, @Param("employeeId") Long employeeId);
}
