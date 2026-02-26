package com.tianye.hrsystem.modules.bonus.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.vo.QueryAdditionalVO;
import com.tianye.hrsystem.modules.bonus.bo.QueryBonusBO;
import com.tianye.hrsystem.modules.bonus.entity.HrmBonus;
import com.tianye.hrsystem.modules.bonus.mapper.HrmBonusMapper;
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

    private static final int TWO = 2;

    @Transactional(rollbackFor = Exception.class)
    public void resolveBonusData(MultipartFile multipartFile, String year, String month) throws Exception {
        if (multipartFile != null) {
            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<HrmBonus> list = new ArrayList<>();
            List<List<Object>> read = reader.read();
            List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
            for (int i = TWO; i < read.size(); i++) {
                HrmBonus hrmBonus = new HrmBonus();
                List<Object> row = read.get(i);
                if (!row.get(0).equals("")) {
                    String EmployeeName = row.get(0).toString();
                    listHrmEmployees.stream().forEach(f -> {
                        if (f.getEmployeeName().equals(EmployeeName)) {
                            hrmBonus.setEmployeeId(f.getEmployeeId());
                            hrmBonus.setDeptId(f.getDeptId());
                        }
                    });
                    hrmBonus.setEmployeeName(EmployeeName);
                }

                if (!row.get(1).equals("")) {
                    hrmBonus.setBonus(new BigDecimal(row.get(1).toString()));
                }
                hrmBonus.setYear(Integer.parseInt(year));
                hrmBonus.setMonth(Integer.parseInt(month));
                list.add(hrmBonus);
            }
            LambdaQueryWrapper<HrmBonus> wrappers = new LambdaQueryWrapper<>();
            wrappers.eq(HrmBonus::getYear, year).eq(HrmBonus::getMonth, month);
            hrmBonusMapper.delete(wrappers);

            saveBatch(list);
        }
    }

    public Page<QueryBounsVO> queryHrmBonusList(@RequestBody QueryBonusBO queryBonusBO) {
        return hrmBonusMapper.queryHrmBonusList(queryBonusBO.parse(), queryBonusBO);
    }
}
