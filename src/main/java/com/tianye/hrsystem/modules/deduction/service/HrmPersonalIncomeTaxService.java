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
import java.util.List;
import java.util.Map;

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
            Integer year = 0;
            Integer month = 0;
            for (int i = TWO; i < read.size(); i++) {
                HrmPersonalIncomeTax hrmPersonalIncomeTax = new HrmPersonalIncomeTax();
                List<Object> row = read.get(i);
                String EmployeeName = row.get(0).toString();
                listHrmEmployees.stream().forEach(f -> {
                    if (f.getEmployeeName().equals(EmployeeName)) {
                        hrmPersonalIncomeTax.setEmployeeId(f.getEmployeeId());
                    }
                });

                if (!row.get(4).toString().equals("")) {
                    hrmPersonalIncomeTax.setAccumulatedIncome(new BigDecimal(row.get(4).toString()));
                }
                if (!row.get(5).toString().equals("")) {
                    hrmPersonalIncomeTax.setAccumulatedDeductionOfExpenses(new BigDecimal(row.get(5).toString()));
                }
                if (!row.get(6).toString().equals("")) {
                    hrmPersonalIncomeTax.setAccumulatedProvidentFund(new BigDecimal(row.get(6).toString()));
                }
                if (!row.get(7).toString().equals("")) {
                    hrmPersonalIncomeTax.setAccumulatedTaxPayment(new BigDecimal(row.get(7).toString()));
                }

                String[] date = dates.split("-");
                year = Integer.parseInt(date[0]);
                month = Integer.parseInt(date[1]);
                hrmPersonalIncomeTax.setYear(year);
                hrmPersonalIncomeTax.setEndMonth(month);
                list.add(hrmPersonalIncomeTax);
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
