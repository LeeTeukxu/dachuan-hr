package com.tianye.hrsystem.modules.salary.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.entity.vo.QueryEmployeeListByDeptIdVO;
import com.tianye.hrsystem.mapper.HrmDeptMapper;
import com.tianye.hrsystem.model.tbdictdata;
import com.tianye.hrsystem.modules.salary.dto.DictDataQueryDto;
import com.tianye.hrsystem.modules.salary.mapper.HrmDictDataMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DemoService
{

    @Autowired
    private IHrmSalaryConfigService hrmSalaryConfigService;

    @Autowired

    private HrmDictDataMapper dictDataMapper;



    public Page<tbdictdata> pageQueryTest1(DictDataQueryDto dto)
    {

        Page<tbdictdata> page = dictDataMapper.selectPageVo(dto.parse(),dto);

        return page;
    }



}
