package com.tianye.hrsystem.modules.holiday.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.holiday.bo.QueryRemainingVacationBO;
import com.tianye.hrsystem.modules.holiday.entity.HrmHolidayDeduction;
import com.tianye.hrsystem.modules.holiday.entity.HrmRemainingVacation;
import com.tianye.hrsystem.modules.holiday.mapper.HrmHolidayDeductionMapper;
import com.tianye.hrsystem.modules.holiday.mapper.HrmRemainingVacationMapper;
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
public class HrmRemainingVacationService extends BaseServiceImpl<HrmRemainingVacationMapper, HrmRemainingVacation> {

    @Autowired
    hrmEmployeeRepository employeeRepository;

    @Autowired
    HrmRemainingVacationMapper hrmRemainingVacationMapper;

    @Autowired
    HrmHolidayDeductionMapper hrmHolidayDeductionMapper;

    private static final int THREE = 3;

    @Transactional(rollbackFor = Exception.class)
    public void resolveRemainingVacationData(MultipartFile multipartFile) throws Exception {
        if (multipartFile != null) {
            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<HrmRemainingVacation> list = new ArrayList<>();
            List<List<Object>> read = reader.read();
            List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
            for (int i = THREE; i < read.size(); i++) {
                HrmRemainingVacation hrmRemainingVacation = new HrmRemainingVacation();
                List<Object> row = read.get(i);
                String EmployeeName = row.get(1).toString();
                hrmRemainingVacation.setEmployeeName(EmployeeName);
                listHrmEmployees.stream().forEach(f -> {
                    if (f.getEmployeeName().equals(EmployeeName)) {
                        hrmRemainingVacation.setEmployeeId(f.getEmployeeId());
                    }
                });
                System.out.println(EmployeeName);
                String jiaban = row.get(6).toString();
                if (jiaban.equals("")) {
                    jiaban = "0";
                }

                String leijishengjia = row.get(5).toString();
                if (leijishengjia.equals("")) {
                    leijishengjia = "0";
                }

                BigDecimal parseLeiJi = new BigDecimal(leijishengjia).multiply(new BigDecimal(8));
                BigDecimal parseHour = parseLeiJi.add(new BigDecimal(jiaban));
                BigDecimal parseMinute = parseHour.multiply(new BigDecimal(60));
                hrmRemainingVacation.setRemainingVacation(parseMinute);
                list.add(hrmRemainingVacation);
            }
            LambdaQueryWrapper<HrmRemainingVacation> wrappers = new LambdaQueryWrapper<>();
            hrmRemainingVacationMapper.delete(wrappers);

//            LambdaQueryWrapper<HrmHolidayDeduction> holidayWrapper = new LambdaQueryWrapper<>();
//            hrmHolidayDeductionMapper.delete(holidayWrapper);

            saveBatch(list);
        }
    }

    public Page<QueryRemainingVacationVO> queryRemainingVacationList(@RequestBody QueryRemainingVacationBO queryRemainingVacationBO) {
        return hrmRemainingVacationMapper.queryRemainingVacationList(queryRemainingVacationBO.parse(), queryRemainingVacationBO);
    }
}
