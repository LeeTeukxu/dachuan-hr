package com.tianye.hrsystem.modules.additional.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.additional.bo.QueryAdditionalBO;
import com.tianye.hrsystem.modules.additional.entity.HrmAdditional;
import com.tianye.hrsystem.modules.additional.entity.HrmEmployeeAdditional;
import com.tianye.hrsystem.modules.additional.mapper.HrmAdditionalMapper;
import com.tianye.hrsystem.modules.additional.mapper.HrmEmployeeAdditionalMapper;
import com.tianye.hrsystem.modules.additional.vo.QueryEmployeeAdditionalVO;
import com.tianye.hrsystem.modules.bonus.bo.QueryBonusBO;
import com.tianye.hrsystem.modules.bonus.vo.QueryBounsVO;
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
public class HrmEmployeeAdditionalService extends BaseServiceImpl<HrmEmployeeAdditionalMapper, HrmEmployeeAdditional> {

    @Autowired
    HrmEmployeeAdditionalMapper hrmEmployeeAdditionalMapper;

    @Autowired
    hrmEmployeeRepository employeeRepository;

    private static final int TWO = 2;

    @Transactional(rollbackFor = Exception.class)
    public void resolveEmployeeAdditionalData(MultipartFile multipartFile) throws Exception {
        if (multipartFile != null) {
            Integer year = 0;
            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<HrmEmployeeAdditional> list = new ArrayList<>();
            List<List<Object>> read = reader.read();
            List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
            for (int i = TWO; i < read.size(); i++) {
                HrmEmployeeAdditional hrmEmployeeAdditional = new HrmEmployeeAdditional();
                List<Object> row = read.get(i);
                String EmployeeName = row.get(0).toString();
                listHrmEmployees.stream().forEach(f -> {
                    if (f.getEmployeeName().equals(EmployeeName)) {
                        hrmEmployeeAdditional.setEmployeeId(f.getEmployeeId());
                    }
                });

                if (!row.get(1).toString().equals("")) {
                    hrmEmployeeAdditional.setChildrenEducation(new BigDecimal(row.get(1).toString()));
                }
                if (!row.get(2).toString().equals("")) {
                    hrmEmployeeAdditional.setHousingLoanInterest(new BigDecimal(row.get(2).toString()));
                }
                if (!row.get(3).toString().equals("")) {
                    hrmEmployeeAdditional.setHousingRent(new BigDecimal(row.get(3).toString()));
                }
                if (!row.get(4).toString().equals("")) {
                    hrmEmployeeAdditional.setSupportingTheElderly(new BigDecimal(row.get(4).toString()));
                }
                if (!row.get(5).toString().equals("")) {
                    hrmEmployeeAdditional.setContinuingEducation(new BigDecimal(row.get(5).toString()));
                }
                if (!row.get(6).toString().equals("")) {
                    hrmEmployeeAdditional.setRaisingGirls(new BigDecimal(row.get(6).toString()));
                }
                year = Integer.parseInt(row.get(7).toString());
                hrmEmployeeAdditional.setYear(Integer.parseInt(row.get(7).toString()));
                list.add(hrmEmployeeAdditional);
            }
            LambdaQueryWrapper<HrmEmployeeAdditional> wrappers = new LambdaQueryWrapper<>();
            wrappers.eq(HrmEmployeeAdditional::getYear, year);
            hrmEmployeeAdditionalMapper.delete(wrappers);
            saveBatch(list);
        }
    }

    public Page<QueryEmployeeAdditionalVO> queryEmployeeAdditionalList(@RequestBody QueryAdditionalBO queryAdditionalBO) {
        return hrmEmployeeAdditionalMapper.queryEmployeeAdditionalList(queryAdditionalBO.parse(), queryAdditionalBO);
    }
}
