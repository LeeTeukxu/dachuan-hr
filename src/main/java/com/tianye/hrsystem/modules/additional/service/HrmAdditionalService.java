package com.tianye.hrsystem.modules.additional.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.ZipUtils;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.vo.OperationResult;
import com.tianye.hrsystem.enums.MultipartFileUtil;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.bo.UpdateAdditionalBO;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.additional.mapper.HrmAdditionalMapper;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.salary.support.TaxImportEmployeeMatcher;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HrmAdditionalService extends BaseServiceImpl<HrmAdditionalMapper, HrmAdditional> {

    @Autowired
    hrmEmployeeRepository employeeRepository;
    
    @Autowired
    HrmAdditionalMapper hrmAdditionalMapper;

    private static final int TWO = 2;

    @Transactional(rollbackFor = Exception.class)
    public void resolveAdditionalData(MultipartFile multipartFile, String year, String month) throws Exception {
        if (multipartFile != null) {
            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<HrmAdditional> list = new ArrayList<>();
            List<List<Object>> read = reader.read();
            List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
            Integer importYear = Integer.parseInt(year);
            Integer importMonth = Integer.parseInt(month);
            String period = String.format("%04d-%02d", importYear, importMonth);
            Set<String> importedKeys = new HashSet<>();
            for (int i = TWO; i < read.size(); i++) {
                List<Object> row = read.get(i);
                if (TaxImportEmployeeMatcher.isBlankRow(row)) {
                    continue;
                }

                HrmAdditional hrmAdditional = new HrmAdditional();
                int displayRow = i + 1;
                String employeeName = TaxImportEmployeeMatcher.readCellText(row, 0);
                String mobile = TaxImportEmployeeMatcher.readCellText(row, 10);
                Long employeeId = TaxImportEmployeeMatcher.resolveEmployeeId(listHrmEmployees, employeeName, mobile, displayRow);
                TaxImportEmployeeMatcher.ensureUniqueEmployeePeriod(importedKeys, employeeId, employeeName, period, displayRow);
                hrmAdditional.setEmployeeId(employeeId);

                BigDecimal childrenEducation = TaxImportEmployeeMatcher.readBigDecimal(row, 4);
                if (childrenEducation != null) {
                    hrmAdditional.setChildrenEducation(childrenEducation);
                }
                BigDecimal housingRent = TaxImportEmployeeMatcher.readBigDecimal(row, 5);
                if (housingRent != null) {
                    hrmAdditional.setHousingRent(housingRent);
                }
                BigDecimal housingLoanInterest = TaxImportEmployeeMatcher.readBigDecimal(row, 6);
                if (housingLoanInterest != null) {
                    hrmAdditional.setHousingLoanInterest(housingLoanInterest);
                }
                BigDecimal supportingTheElderly = TaxImportEmployeeMatcher.readBigDecimal(row, 7);
                if (supportingTheElderly != null) {
                    hrmAdditional.setSupportingTheElderly(supportingTheElderly);
                }
                BigDecimal continuingEducation = TaxImportEmployeeMatcher.readBigDecimal(row, 8);
                if (continuingEducation != null) {
                    hrmAdditional.setContinuingEducation(continuingEducation);
                }
                BigDecimal raisingGirls = TaxImportEmployeeMatcher.readBigDecimal(row, 9);
                if (raisingGirls != null) {
                    hrmAdditional.setRaisingGirls(raisingGirls);
                }
                hrmAdditional.setYear(importYear);
                hrmAdditional.setMonth(importMonth);
                list.add(hrmAdditional);
            }
            if (list.isEmpty()) {
                return;
            }
            LambdaQueryWrapper<HrmAdditional> wrappers = new LambdaQueryWrapper<>();
            wrappers.eq(HrmAdditional::getYear, importYear).eq(HrmAdditional::getMonth, importMonth);
            hrmAdditionalMapper.delete(wrappers);

            saveBatch(list);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolveAdditionalInfoData(MultipartFile multipartFile, String year) throws Exception {
        LoginUserInfo info = CompanyContext.get();
        String targetDir = "D:\\additional\\" + info.getCompanyId() + "\\";
        ZipUtils.unzip(multipartFile, targetDir);

        readExcel(targetDir);
    }

    private void readExcel(String targetDir) throws Exception {
        File file = new File(targetDir);
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".xls") || f.getName().endsWith(".xlsx")) {
                MultipartFile multipartFile = MultipartFileUtil.getMultipartFile(f);
                ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
                List<List<Object>> read = reader.read();
            }
        }
    }

    public Page<QueryAdditionalVO> queryAdditionalList(@RequestBody QueryAdditionalBO queryAdditionalBO) {
        return hrmAdditionalMapper.queryAdditionalList(queryAdditionalBO.parse(), queryAdditionalBO);
    }

    @Transactional(rollbackFor = Exception.class)
    public OperationResult updateAdditional(UpdateAdditionalBO updateAdditionalBO) {
        List<HrmAdditional> result = new ArrayList<>();
        for (UpdateAdditionalBO.Project project : updateAdditionalBO.getAdditionalValues()) {
            HrmAdditional hrmAdditional = BeanUtil.copyProperties(project, HrmAdditional.class);
            result.add(hrmAdditional);
        }
        saveOrUpdateBatch(result);
        return null;
    }

    public OperationResult deleteAdditional(Long additionalId) {
        hrmAdditionalMapper.deleteById(additionalId);
        return null;
    }
}
