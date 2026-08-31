package com.tianye.hrsystem.modules.additional.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.enums.HrmCodeEnum;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.additional.entity.HrmEmployeeAdditional;
import com.tianye.hrsystem.modules.additional.mapper.HrmAdditionalMapper;
import com.tianye.hrsystem.modules.additional.mapper.HrmEmployeeAdditionalMapper;
import com.tianye.hrsystem.modules.additional.vo.QueryEmployeeAdditionalVO;
import com.tianye.hrsystem.modules.bonus.bo.QueryBonusBO;
import com.tianye.hrsystem.modules.bonus.vo.QueryBounsVO;
import com.tianye.hrsystem.modules.salary.support.TaxImportEmployeeMatcher;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HrmEmployeeAdditionalService extends BaseServiceImpl<HrmEmployeeAdditionalMapper, HrmEmployeeAdditional> {

    @Autowired
    HrmEmployeeAdditionalMapper hrmEmployeeAdditionalMapper;

    @Autowired
    hrmEmployeeRepository employeeRepository;

    private static final int TWO = 2;

    @Transactional(rollbackFor = Exception.class)
    public void resolveEmployeeAdditionalData(MultipartFile multipartFile) throws Exception {
        if (multipartFile != null) {
            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<HrmEmployeeAdditional> list = new ArrayList<>();
            List<List<Object>> read = reader.read();
            List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
            Set<Integer> importYears = new HashSet<>();
            Set<String> importedKeys = new HashSet<>();
            for (int i = TWO; i < read.size(); i++) {
                List<Object> row = read.get(i);
                if (TaxImportEmployeeMatcher.isBlankRow(row)) {
                    continue;
                }

                HrmEmployeeAdditional hrmEmployeeAdditional = new HrmEmployeeAdditional();
                int displayRow = i + 1;
                String employeeName = TaxImportEmployeeMatcher.readCellText(row, 0);
                String mobile = TaxImportEmployeeMatcher.readCellText(row, 8);
                Long employeeId = TaxImportEmployeeMatcher.resolveEmployeeId(listHrmEmployees, employeeName, mobile, displayRow);
                Integer year = TaxImportEmployeeMatcher.readInteger(row, 7);
                if (year == null) {
                    throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + displayRow + "行年份不能为空");
                }
                TaxImportEmployeeMatcher.ensureUniqueEmployeePeriod(importedKeys, employeeId, employeeName, String.valueOf(year), displayRow);
                importYears.add(year);
                hrmEmployeeAdditional.setEmployeeId(employeeId);

                BigDecimal childrenEducation = TaxImportEmployeeMatcher.readBigDecimal(row, 1);
                if (childrenEducation != null) {
                    hrmEmployeeAdditional.setChildrenEducation(childrenEducation);
                }
                BigDecimal housingLoanInterest = TaxImportEmployeeMatcher.readBigDecimal(row, 2);
                if (housingLoanInterest != null) {
                    hrmEmployeeAdditional.setHousingLoanInterest(housingLoanInterest);
                }
                BigDecimal housingRent = TaxImportEmployeeMatcher.readBigDecimal(row, 3);
                if (housingRent != null) {
                    hrmEmployeeAdditional.setHousingRent(housingRent);
                }
                BigDecimal supportingTheElderly = TaxImportEmployeeMatcher.readBigDecimal(row, 4);
                if (supportingTheElderly != null) {
                    hrmEmployeeAdditional.setSupportingTheElderly(supportingTheElderly);
                }
                BigDecimal continuingEducation = TaxImportEmployeeMatcher.readBigDecimal(row, 5);
                if (continuingEducation != null) {
                    hrmEmployeeAdditional.setContinuingEducation(continuingEducation);
                }
                BigDecimal raisingGirls = TaxImportEmployeeMatcher.readBigDecimal(row, 6);
                if (raisingGirls != null) {
                    hrmEmployeeAdditional.setRaisingGirls(raisingGirls);
                }
                hrmEmployeeAdditional.setYear(year);
                list.add(hrmEmployeeAdditional);
            }
            if (list.isEmpty()) {
                return;
            }
            for (Integer importYear : importYears) {
                LambdaQueryWrapper<HrmEmployeeAdditional> wrappers = new LambdaQueryWrapper<>();
                wrappers.eq(HrmEmployeeAdditional::getYear, importYear);
                hrmEmployeeAdditionalMapper.delete(wrappers);
            }
            saveBatch(list);
        }
    }

    public Page<QueryEmployeeAdditionalVO> queryEmployeeAdditionalList(@RequestBody QueryAdditionalBO queryAdditionalBO) {
        return hrmEmployeeAdditionalMapper.queryEmployeeAdditionalList(queryAdditionalBO.parse(), queryAdditionalBO);
    }
}
