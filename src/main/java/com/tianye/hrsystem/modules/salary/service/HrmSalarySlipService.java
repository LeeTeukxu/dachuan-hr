package com.tianye.hrsystem.modules.salary.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.salary.dto.QuerySalarySlipListDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalarySlip;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalarySlipMapper;
import com.tianye.hrsystem.modules.salary.vo.QuerySalarySlipListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 工资条 服务实现类
 * </p>
 *
 * @author hmb
 * @since 2020-11-03
 */
@Service
public class HrmSalarySlipService extends BaseServiceImpl<HrmSalarySlipMapper, HrmSalarySlip> {


    @Autowired
    private HrmSalarySlipMapper salarySlipMapper;

    @Autowired
    private HrmSalarySlipOptionService salarySlipOptionService;

    @Autowired
    private HrmSalarySlipRecordService slipRecordService;


    public Page<QuerySalarySlipListVO> querySalarySlipList(QuerySalarySlipListDto querySalarySlipListBO,Long employeeId)
    {
        Page<QuerySalarySlipListVO> page = salarySlipMapper.querySalarySlipList(querySalarySlipListBO.parse(), querySalarySlipListBO, employeeId);
        page.getRecords().forEach(slip -> {
            if (slip.getReadStatus() == 0) {
                lambdaUpdate().set(HrmSalarySlip::getReadStatus, 1).eq(HrmSalarySlip::getId, slip.getId()).update();
            }
            slip.setSalarySlipOptionList(slipRecordService.querySlipDetail(slip.getId()));
        });
        return page;
    }
}
