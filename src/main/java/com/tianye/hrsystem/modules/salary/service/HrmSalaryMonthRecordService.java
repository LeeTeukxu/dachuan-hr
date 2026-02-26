package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.LanguageFieldUtil;
import com.tianye.hrsystem.enums.HrmLanguageEnum;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import com.tianye.hrsystem.modules.salary.dto.QueryHistorySalaryDetailDto;
import com.tianye.hrsystem.modules.salary.dto.QueryHistorySalaryListDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthEmpRecordMapper;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryMonthRecordMapper;
import com.tianye.hrsystem.modules.salary.vo.QueryHistorySalaryDetailVO;
import com.tianye.hrsystem.modules.salary.vo.QueryHistorySalaryListVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryPageListVO;
import com.tianye.hrsystem.modules.salary.vo.SalaryOptionHeadVO;
import com.tianye.hrsystem.util.TransferUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HrmSalaryMonthRecordService extends BaseServiceImpl<HrmSalaryMonthRecordMapper, HrmSalaryMonthRecord> {
    @Autowired
    private HrmSalaryMonthRecordMapper salaryMonthRecordMapper;

    @Autowired
    private HrmSalaryMonthEmpRecordMapper salaryMonthEmpRecordMapper;

    @Autowired
    private HrmSalaryMonthOptionValueService salaryMonthOptionValueService;

    public Page<QueryHistorySalaryListVO> queryHistorySalaryList(QueryHistorySalaryListDto queryHistorySalaryListDto) {
//        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.SALARY_MENU_ID);
//        if (CollUtil.isEmpty(employeeIds)) {
//            return new BasePage<>();
//        }
        Page<QueryHistorySalaryListVO> salaryList = salaryMonthRecordMapper.queryHistorySalaryList(queryHistorySalaryListDto.parse(), queryHistorySalaryListDto);
//        List<QueryHistorySalaryListVO> list = salaryList.getList();
//        if (CollectionUtil.isNotEmpty(list)) {
//            //添加语言包key
//            Map<String, String> keyMap = new HashMap<>();
//            for (QueryHistorySalaryListVO listVO : list) {
//                keyMap.put("title_resourceKey", "{" + HrmLanguageEnum.parseKey(listVO.getMonth()) + "}" + "{" + HrmLanguageEnum.SALARY_REPORT.getKey() + "}");
//                listVO.setLanguageKeyMap(keyMap);
//            }
//        }
        return salaryList;
    }

    public QueryHistorySalaryDetailVO queryHistorySalaryDetail(QueryHistorySalaryDetailDto queryHistorySalaryDetailDto) {
        HrmSalaryMonthRecord monthRecord = getById(queryHistorySalaryDetailDto.getSRecordId());
//        Collection<Long> employeeIds = employeeUtil.queryDataAuthEmpIdByMenuId(MenuIdConstant.SALARY_MENU_ID);
        QueryHistorySalaryDetailVO historySalaryDetailVO = salaryMonthRecordMapper.queryHistorySalaryDetail(queryHistorySalaryDetailDto.getSRecordId());
        List<SalaryOptionHeadVO> salaryOptionHeadVOList = JSON.parseArray(monthRecord.getOptionHead(), SalaryOptionHeadVO.class);
        BasePage<QuerySalaryPageListVO> page = salaryMonthEmpRecordMapper.querySalaryPageListByRecordId(queryHistorySalaryDetailDto.parse(), queryHistorySalaryDetailDto);
        page.getList().forEach(querySalaryPageListVO -> {
            List<ComputeSalaryDto> list = salaryMonthOptionValueService.querySalaryOptionValue(querySalaryPageListVO.getSEmpRecordId());
            List<QuerySalaryPageListVO.SalaryValue> salaryValues = TransferUtil.transferList(list, QuerySalaryPageListVO.SalaryValue.class);
            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 1, querySalaryPageListVO.getNeedWorkDay().toString(), 1, "应出勤天数"));
            salaryValues.add(new QuerySalaryPageListVO.SalaryValue(0L, 2, querySalaryPageListVO.getActualWorkDay().toString(), 1, "实际出勤天数"));
            querySalaryPageListVO.setSalary(salaryValues);
        });
        for (SalaryOptionHeadVO headVO : salaryOptionHeadVOList) {
            //添加语言包key
            headVO.setLanguageKeyMap(LanguageFieldUtil.getFieldNameKeyMap("name_resourceKey", "hrm.", StrUtil.toString(headVO.getCode())));
        }
        historySalaryDetailVO.setSalaryOptionHeadList(salaryOptionHeadVOList);
        historySalaryDetailVO.setPageData(page);
        Map<String, String> keyMap = new HashMap<>();
        keyMap.put("title_resourceKey", "{" + HrmLanguageEnum.parseKey(historySalaryDetailVO.getMonth()) + "}" + "{" + HrmLanguageEnum.SALARY_REPORT.getKey() + "}");
        historySalaryDetailVO.setLanguageKeyMap(keyMap);
        return historySalaryDetailVO;
    }
}
