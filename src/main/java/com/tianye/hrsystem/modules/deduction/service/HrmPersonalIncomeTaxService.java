package com.tianye.hrsystem.modules.deduction.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.entity.bo.SetAttendanceRuleBO;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.OperationResult;
import com.tianye.hrsystem.modules.deduction.bo.QueryPersonalIncomeTaxBO;
import com.tianye.hrsystem.modules.deduction.bo.UpdatePersonalIncomeTaxBO;
import com.tianye.hrsystem.modules.deduction.entity.HrmPersonalIncomeTax;
import com.tianye.hrsystem.modules.deduction.mapper.HrmPersonalIncomeTaxMapper;
import com.tianye.hrsystem.modules.deduction.vo.QueryPersonalIncomeTaxVO;
import com.tianye.hrsystem.modules.insurance.dto.UpdateInsuranceProjectBO;
import com.tianye.hrsystem.modules.salary.support.TaxImportEmployeeMatcher;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HrmPersonalIncomeTaxService extends BaseServiceImpl<HrmPersonalIncomeTaxMapper, HrmPersonalIncomeTax> {

    @Autowired
    hrmEmployeeRepository employeeRepository;
    
    @Autowired
    HrmPersonalIncomeTaxMapper hrmPersonalIncomeTaxMapper;

    private static final int TWO = 2;

    @Transactional(rollbackFor = Exception.class)
    public void resolvePersonalIncomeTaxData(MultipartFile multipartFile, String dates) throws Exception {
        if (multipartFile != null) {
            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<HrmPersonalIncomeTax> list = new ArrayList<>();
            List<List<Object>> read = reader.read();
            List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
            String[] date = dates.split("-");
            Integer year = Integer.parseInt(date[0]);
            Integer month = Integer.parseInt(date[1]);
            Set<String> importedKeys = new HashSet<>();
            for (int i = TWO; i < read.size(); i++) {
                List<Object> row = read.get(i);
                if (TaxImportEmployeeMatcher.isBlankRow(row)) {
                    continue;
                }

                HrmPersonalIncomeTax hrmPersonalIncomeTax = new HrmPersonalIncomeTax();
                int displayRow = i + 1;
                String employeeName = TaxImportEmployeeMatcher.readCellText(row, 0);
                String mobile = TaxImportEmployeeMatcher.readCellText(row, 8);
                Long employeeId = TaxImportEmployeeMatcher.resolveEmployeeId(listHrmEmployees, employeeName, mobile, displayRow);
                TaxImportEmployeeMatcher.ensureUniqueEmployeePeriod(importedKeys, employeeId, employeeName, dates, displayRow);
                hrmPersonalIncomeTax.setEmployeeId(employeeId);

                BigDecimal accumulatedIncome = TaxImportEmployeeMatcher.readBigDecimal(row, 4);
                if (accumulatedIncome != null) {
                    hrmPersonalIncomeTax.setAccumulatedIncome(accumulatedIncome);
                }
                BigDecimal accumulatedDeductionOfExpenses = TaxImportEmployeeMatcher.readBigDecimal(row, 5);
                if (accumulatedDeductionOfExpenses != null) {
                    hrmPersonalIncomeTax.setAccumulatedDeductionOfExpenses(accumulatedDeductionOfExpenses);
                }
                BigDecimal accumulatedProvidentFund = TaxImportEmployeeMatcher.readBigDecimal(row, 6);
                if (accumulatedProvidentFund != null) {
                    hrmPersonalIncomeTax.setAccumulatedProvidentFund(accumulatedProvidentFund);
                }
                BigDecimal accumulatedTaxPayment = TaxImportEmployeeMatcher.readBigDecimal(row, 7);
                if (accumulatedTaxPayment != null) {
                    hrmPersonalIncomeTax.setAccumulatedTaxPayment(accumulatedTaxPayment);
                }
                hrmPersonalIncomeTax.setYear(year);
                hrmPersonalIncomeTax.setEndMonth(month);
                list.add(hrmPersonalIncomeTax);
            }
            if (list.isEmpty()) {
                return;
            }
            LambdaQueryWrapper<HrmPersonalIncomeTax> wrappers = new LambdaQueryWrapper<>();
            wrappers.eq(HrmPersonalIncomeTax::getYear, year).eq(HrmPersonalIncomeTax::getEndMonth, month);
            hrmPersonalIncomeTaxMapper.delete(wrappers);

            saveBatch(list);
        }
    }

    public Page<QueryPersonalIncomeTaxVO> queryPersonalIncomeTaxList(@RequestBody QueryPersonalIncomeTaxBO queryRemainingVacationBO) {
        return hrmPersonalIncomeTaxMapper.queryRemainingVacationList(queryRemainingVacationBO.parse(), queryRemainingVacationBO);
    }

    @Transactional(rollbackFor = Exception.class)
    public OperationResult updatePersonalIncomeTax(UpdatePersonalIncomeTaxBO updatePersonalIncomeTaxBO) {
        List<HrmPersonalIncomeTax> result = new ArrayList<>();
        for (UpdatePersonalIncomeTaxBO.Project project : updatePersonalIncomeTaxBO.getPersonalIncomeTaxValues()) {
            HrmPersonalIncomeTax hrmPersonalIncomeTax = BeanUtil.copyProperties(project, HrmPersonalIncomeTax.class);
            result.add(hrmPersonalIncomeTax);
        }
        saveOrUpdateBatch(result);
        return null;
    }

    public OperationResult deletePersonalIncomeTax(Long personalIncomeTaxId) {
        hrmPersonalIncomeTaxMapper.deleteById(personalIncomeTaxId);
        return null;
    }
}
