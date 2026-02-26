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
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
            for (int i = TWO; i < read.size(); i++) {
                HrmAdditional hrmAdditional = new HrmAdditional();
                List<Object> row = read.get(i);
                String EmployeeName = row.get(0).toString();
                listHrmEmployees.stream().forEach(f -> {
                    if (f.getEmployeeName().equals(EmployeeName)) {
                        hrmAdditional.setEmployeeId(f.getEmployeeId());
                    }
                });

                if (!row.get(4).toString().equals("")) {
                    hrmAdditional.setChildrenEducation(new BigDecimal(row.get(4).toString()));
                }
                if (!row.get(5).toString().equals("")) {
                    hrmAdditional.setHousingRent(new BigDecimal(row.get(5).toString()));
                }
                if (!row.get(6).toString().equals("")) {
                    hrmAdditional.setHousingLoanInterest(new BigDecimal(row.get(6).toString()));
                }
                if (!row.get(7).toString().equals("")) {
                    hrmAdditional.setSupportingTheElderly(new BigDecimal(row.get(7).toString()));
                }
                if (!row.get(8).toString().equals("")) {
                    hrmAdditional.setContinuingEducation(new BigDecimal(row.get(8).toString()));
                }
                if (!row.get(9).toString().equals("")) {
                    hrmAdditional.setRaisingGirls(new BigDecimal(row.get(9).toString()));
                }
                hrmAdditional.setYear(Integer.parseInt(year));
                hrmAdditional.setMonth(Integer.parseInt(month));
                list.add(hrmAdditional);
            }
            LambdaQueryWrapper<HrmAdditional> wrappers = new LambdaQueryWrapper<>();
            wrappers.eq(HrmAdditional::getYear, year).eq(HrmAdditional::getMonth, month);
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
