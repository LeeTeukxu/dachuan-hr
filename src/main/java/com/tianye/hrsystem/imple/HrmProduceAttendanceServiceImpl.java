package com.tianye.hrsystem.imple;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.bo.QueryMonthAttendanceBO;
import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.entity.vo.QueryMonthAttendanceVO;
import com.tianye.hrsystem.mapper.HrmProduceAttendanceMapper;
import com.tianye.hrsystem.model.HrmDept;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.service.IHrmProduceAttendanceService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.apache.log4j.helpers.SyslogWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class HrmProduceAttendanceServiceImpl extends BaseServiceImpl<HrmProduceAttendanceMapper, HrmProduceAttendance> implements IHrmProduceAttendanceService {

    @Autowired
    IHrmEmployeeService hrmEmployeeService;

    @Autowired
    hrmEmployeeRepository employeeRepository;

    @Autowired
    hrmDeptRepository deptRepository;

    @Autowired
    HrmProduceAttendanceMapper produceAttendanceMapper;

    private static final int TWO = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveProduceAttendanceData(MultipartFile multipartFile, String dates) throws Exception {
        if (multipartFile != null) {
            ExcelReader reader = ExcelUtil.getReader(multipartFile.getInputStream());
            List<HrmProduceAttendance> list = new ArrayList<>();
            List<List<Object>> read = reader.read();
            List<com.tianye.hrsystem.model.HrmEmployee> listHrmEmployees = employeeRepository.findAll();
            List<HrmDept> listDepts = deptRepository.findAll();
            Integer year = 0;
            Integer month = 0;
            for (int i = TWO; i < read.size(); i++) {
                HrmProduceAttendance hrmProduceAttendance = new HrmProduceAttendance();
                List<Object> row = read.get(i);
                String EmployeeName = row.get(1).toString().trim();

//                LambdaQueryWrapper<HrmEmployee> wrapper = new QueryWrapper<HrmEmployee>().lambda().select(HrmEmployee::getEmployeeId).eq(HrmEmployee::getEmployeeName, EmployeeName);
//                HrmEmployee employee = hrmEmployeeService.getOne(wrapper);
//                hrmProduceAttendance.setEmployeeId(employee.getEmployeeId());
                listHrmEmployees.stream().forEach(f -> {
                    if (f.getEmployeeName().equals(EmployeeName)) {
                        hrmProduceAttendance.setEmployeeId(f.getEmployeeId());

                        listDepts.stream().forEach(x -> {
                            if (f.getDeptId().equals(x.getDeptId())) {
                                if (x.getName().indexOf("质检部") > -1 || x.getName().indexOf("质量部") > -1 || x.getName().indexOf("仓储部") > -1 || x.getName().indexOf("研发部") > -1
                                        || x.getName().indexOf("工程部") > -1
                                        || x.getName().indexOf("生产部") > -1 || x.getName().indexOf("仓库") > -1 || x.getName().indexOf("设备技术部") > -1) {
                                    hrmProduceAttendance.setDepartment(2);
                                }else hrmProduceAttendance.setDepartment(1);
                            }
                        });
                    }
                });

                String[] date = dates.split("-");
                year = Integer.parseInt(date[0]);
                month = Integer.parseInt(date[1]);
                hrmProduceAttendance.setYear(year);
                hrmProduceAttendance.setMonth(month);
                hrmProduceAttendance.setEmployeeName(EmployeeName);
                if (!row.get(2).toString().trim().equals("")) {
                    hrmProduceAttendance.setPositiveAttendance(new BigDecimal(row.get(2).toString().trim()));
                }else hrmProduceAttendance.setPositiveAttendance(new BigDecimal(0));
                if (!row.get(3).toString().trim().equals("")) {
                    hrmProduceAttendance.setProbationAttendance(new BigDecimal(row.get(3).toString().trim()));
                }else hrmProduceAttendance.setPositiveAttendance(new BigDecimal(0));
                if (!row.get(4).toString().trim().equals("")) {
                    hrmProduceAttendance.setWorkOverTime(new BigDecimal(row.get(4).toString().trim().trim()));
                }
////                if (!row.get(4).toString().equals("")) {
////                    hrmProduceAttendance.setEmptyClass(Integer.parseInt(row.get(4).toString()));
////                }else hrmProduceAttendance.setEmptyClass(0);
////                if (!row.get(5).toString().equals("")) {
////                    hrmProduceAttendance.setMiddleClass(Integer.parseInt(row.get(5).toString()));
//                }else hrmProduceAttendance.setMiddleClass(0);
                if (!row.get(5).toString().trim().equals("")) {
                    hrmProduceAttendance.setNightShift(Integer.parseInt(row.get(5).toString().trim()));
                }else hrmProduceAttendance.setNightShift(0);
                if (!row.get(6).toString().trim().equals("")) {
                    hrmProduceAttendance.setNightSubsidy(new BigDecimal(row.get(6).toString().trim()));
                }else hrmProduceAttendance.setNightSubsidy(new BigDecimal(0));
//                if (!row.get(8).toString().equals("")) {
//                    hrmProduceAttendance.setCurrentMonthVacation(new BigDecimal(row.get(8).toString()));
//                }
                if (!row.get(7).toString().trim().equals("")) {
                    hrmProduceAttendance.setHighTemperature(new BigDecimal(row.get(7).toString().trim()));
                }else hrmProduceAttendance.setHighTemperature(new BigDecimal(0));
                if (!row.get(8).toString().trim().equals("")) {
                    hrmProduceAttendance.setLowTemperature(new BigDecimal(row.get(8).toString().trim()));
                }else hrmProduceAttendance.setLowTemperature(new BigDecimal(0));
                if (!row.get(9).toString().trim().equals("")) {
                    hrmProduceAttendance.setOtherSubsidies(new BigDecimal(row.get(9).toString().trim()));
                }else hrmProduceAttendance.setOtherSubsidies(new BigDecimal(0));
                if (!row.get(10).toString().trim().equals("")) {
                    hrmProduceAttendance.setOtherDeductions(new BigDecimal(row.get(10).toString().trim()));
                }else hrmProduceAttendance.setOtherDeductions(new BigDecimal(0));
                if (!row.get(11).toString().trim().equals("")) {
                    hrmProduceAttendance.setLoan(new BigDecimal(row.get(11).toString().trim()));
                }else hrmProduceAttendance.setLoan(new BigDecimal(0));
                //有BUG
                if (!row.get(12).toString().trim().equals("")) {
                    hrmProduceAttendance.setRemark(row.get(12).toString().trim());
                }
                list.add(hrmProduceAttendance);
            }
            LambdaQueryWrapper<HrmProduceAttendance> wrappers = new LambdaQueryWrapper<>();
            wrappers.eq(HrmProduceAttendance::getYear, year).eq(HrmProduceAttendance::getMonth, month);
            produceAttendanceMapper.delete(wrappers);
            saveBatch(list);
        }
    }

    @Override
    public Page<QueryMonthAttendanceVO> queryProduceAttendanceList(QueryMonthAttendanceBO queryMonthAttendanceBO) {
        return produceAttendanceMapper.queryProduceAttendanceList(queryMonthAttendanceBO.parse(), queryMonthAttendanceBO);
    }
}
