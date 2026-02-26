package com.tianye.hrsystem.modules.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.modules.salary.dto.QuerySendRecordListDto;
import com.tianye.hrsystem.modules.salary.dto.QuerySlipEmployeePageListDto;
import com.tianye.hrsystem.modules.salary.dto.SendSalarySlipDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalarySlipRecord;
import com.tianye.hrsystem.modules.salary.entity.SlipEmployeeVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySendDetailListVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySendRecordListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 发工资条记录 Mapper 接口
 * </p>
 *
 * @author hmb
 * @since 2020-11-03
 */
public interface HrmSalarySlipRecordMapper extends BaseMapper<HrmSalarySlipRecord> {

    Page<SlipEmployeeVO> querySlipEmployeePageList(Page<SlipEmployeeVO> page, @Param("sRecordId") Long sRecordId, @Param("data") QuerySlipEmployeePageListDto slipEmployeePageListDto);

    Page<QuerySendRecordListVO> querySendRecordList(Page<QuerySendRecordListVO> page, @Param("data") QuerySendRecordListDto querySendRecordListBO);

    Page<QuerySendDetailListVO> querySendDetailList(Page<QuerySendDetailListVO> page, @Param("data") QuerySendRecordListDto querySendRecordListBO);

    List<Long> querySlipEmployeeIds(@Param("sRecordId") Long sRecordId, @Param("data") SendSalarySlipDto sendSalarySlipBO);
}
