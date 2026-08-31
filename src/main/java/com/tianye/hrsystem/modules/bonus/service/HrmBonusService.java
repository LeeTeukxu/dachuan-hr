package com.tianye.hrsystem.modules.bonus.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.bonus.bo.QueryBonusBO;
import com.tianye.hrsystem.modules.bonus.entity.HrmBonus;
import com.tianye.hrsystem.modules.bonus.entity.HrmBonusTaxOnly;
import com.tianye.hrsystem.modules.bonus.mapper.HrmBonusMapper;
import com.tianye.hrsystem.modules.bonus.mapper.HrmBonusTaxOnlyMapper;
import com.tianye.hrsystem.modules.bonus.vo.QueryBounsVO;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.baomidou.mybatisplus.extension.toolkit.Db.saveBatch;

@Service
public class HrmBonusService {

    @Autowired
    hrmEmployeeRepository employeeRepository;

    @Autowired
    hrmDeptRepository hrmDeptRepository;

    @Autowired
    HrmBonusMapper hrmBonusMapper;

    @Autowired
    HrmBonusTaxOnlyMapper hrmBonusTaxOnlyMapper;

    private static final int TWO = 2;

    @Transactional(rollbackFor = Exception.class)
    public void resolveBonusData(MultipartFile multipartFile, String year, String month) throws Exception {
        if (multipartFile == null) {
            return;
        }
        List<BonusImportRow> rows = readBonusImportRows(multipartFile);
        List<HrmBonus> list = new ArrayList<>();
        Integer parsedYear = Integer.parseInt(year);
        Integer parsedMonth = Integer.parseInt(month);
        for (BonusImportRow row : rows) {
            HrmBonus hrmBonus = new HrmBonus();
            hrmBonus.setEmployeeId(row.employeeId);
            hrmBonus.setDeptId(row.deptId);
            hrmBonus.setEmployeeName(row.employeeName);
            hrmBonus.setBonus(row.bonus);
            hrmBonus.setYear(parsedYear);
            hrmBonus.setMonth(parsedMonth);
            list.add(hrmBonus);
        }
        LambdaQueryWrapper<HrmBonus> wrappers = new LambdaQueryWrapper<>();
        wrappers.eq(HrmBonus::getYear, year).eq(HrmBonus::getMonth, month);
        hrmBonusMapper.delete(wrappers);
        if (!list.isEmpty()) {
            saveBatch(list);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolveTaxOnlyBonusData(MultipartFile multipartFile, String year, String month) throws Exception {
        if (multipartFile == null) {
            return;
        }
        List<BonusImportRow> rows = readBonusImportRows(multipartFile);
        List<HrmBonusTaxOnly> list = new ArrayList<>();
        Integer parsedYear = Integer.parseInt(year);
        Integer parsedMonth = Integer.parseInt(month);
        for (BonusImportRow row : rows) {
            HrmBonusTaxOnly hrmBonus = new HrmBonusTaxOnly();
            hrmBonus.setEmployeeId(row.employeeId);
            hrmBonus.setDeptId(row.deptId);
            hrmBonus.setEmployeeName(row.employeeName);
            hrmBonus.setBonus(row.bonus);
            hrmBonus.setYear(parsedYear);
            hrmBonus.setMonth(parsedMonth);
            list.add(hrmBonus);
        }
        LambdaQueryWrapper<HrmBonusTaxOnly> wrappers = new LambdaQueryWrapper<>();
        wrappers.eq(HrmBonusTaxOnly::getYear, year).eq(HrmBonusTaxOnly::getMonth, month);
        hrmBonusTaxOnlyMapper.delete(wrappers);
        if (!list.isEmpty()) {
            saveBatch(list);
        }
    }

    private List<BonusImportRow> readBonusImportRows(MultipartFile multipartFile) throws Exception {
        ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
        List<BonusImportRow> list = new ArrayList<>();
        List<List<Object>> read = reader.read();
        List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
        for (int i = TWO; i < read.size(); i++) {
            List<Object> row = read.get(i);
            if (row.size() < TWO || row.get(0) == null || row.get(0).equals("")) {
                continue;
            }
            BonusImportRow bonusRow = new BonusImportRow();
            String employeeName = row.get(0).toString();
            listHrmEmployees.forEach(employee -> {
                if (employee.getEmployeeName().equals(employeeName)) {
                    bonusRow.employeeId = employee.getEmployeeId();
                    bonusRow.deptId = employee.getDeptId();
                }
            });
            bonusRow.employeeName = employeeName;
            if (row.size() > 1 && row.get(1) != null && !row.get(1).equals("")) {
                bonusRow.bonus = new BigDecimal(row.get(1).toString());
            } else {
                bonusRow.bonus = BigDecimal.ZERO;
            }
            list.add(bonusRow);
        }
        return list;
    }

    private static class BonusImportRow {
        private Long employeeId;
        private String employeeName;
        private Long deptId;
        private BigDecimal bonus;
    }

    public Page<QueryBounsVO> queryHrmBonusList(@RequestBody QueryBonusBO queryBonusBO) {
        return hrmBonusMapper.queryHrmBonusList(queryBonusBO.parse(), queryBonusBO);
    }
}
