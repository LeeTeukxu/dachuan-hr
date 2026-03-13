package com.tianye.hrsystem.modules.holiday.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.modules.holiday.bo.QueryHolidayDeductionBO;
import com.tianye.hrsystem.modules.holiday.bo.UpdateHolidayDeductionBO;
import com.tianye.hrsystem.modules.holiday.entity.HrmHolidayDeduction;
import com.tianye.hrsystem.modules.holiday.entity.HrmRemainingVacation;
import com.tianye.hrsystem.modules.holiday.mapper.HrmHolidayDeductionMapper;
import com.tianye.hrsystem.modules.holiday.mapper.HrmRemainingVacationMapper;
import com.tianye.hrsystem.modules.holiday.vo.QueryHolidayDeductionVO;
import com.tianye.hrsystem.modules.holiday.vo.QueryRemainingVacationVO;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class HrmHolidayDeductionService extends BaseServiceImpl<HrmHolidayDeductionMapper, HrmHolidayDeduction> {

    @Autowired
    HrmHolidayDeductionMapper hrmHolidayDeductionMapper;

    @Autowired
    HrmRemainingVacationService hrmRemainingVacationService;

    public Page<QueryHolidayDeductionVO> queryHolidayDeductionList(@RequestBody QueryHolidayDeductionBO queryHolidayDeductionBO) {
        return hrmHolidayDeductionMapper.queryHolidayDeductionList(queryHolidayDeductionBO.parse(), queryHolidayDeductionBO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveHolidayDeduction(UpdateHolidayDeductionBO updateHolidayDeductionBO) {
        HrmHolidayDeduction hrmHolidayDeduction = BeanUtil.copyProperties(updateHolidayDeductionBO, HrmHolidayDeduction.class);
        save(hrmHolidayDeduction);

        LambdaUpdateWrapper<HrmRemainingVacation> wrapper = new LambdaUpdateWrapper<HrmRemainingVacation>()
                .eq(HrmRemainingVacation:: getHolidayId, hrmHolidayDeduction.getHolidayId());
        HrmRemainingVacation hrmRemainingVacation = hrmRemainingVacationService.getOne(wrapper);
        if (updateHolidayDeductionBO.getType() < 6 && updateHolidayDeductionBO.getUpdate_status() == 2) {
            hrmRemainingVacation.setRemainingVacation(hrmRemainingVacation.getRemainingVacation().subtract(hrmHolidayDeduction.getDeductionTime()));
            hrmRemainingVacationService.update(hrmRemainingVacation, wrapper);
        }
    }

    public List<QueryHolidayDeductionVO> queryHolidayDeduction(QueryHolidayDeductionBO queryHolidayDeductionBO) {
        return hrmHolidayDeductionMapper.queryHolidayDeduction(queryHolidayDeductionBO);
    }

    public List<QueryHolidayDeductionVO> queryHolidayDeductionBatch(int year, int month) {
        return hrmHolidayDeductionMapper.queryHolidayDeductionBatch(year, month);
    }
}
