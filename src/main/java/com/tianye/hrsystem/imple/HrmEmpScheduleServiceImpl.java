package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.po.HrmEmpSchedule;
import com.tianye.hrsystem.mapper.HrmEmpScheduleMapper;
import com.tianye.hrsystem.service.IHrmEmpScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * 员工排班信息 服务类
 */
@Service
public class HrmEmpScheduleServiceImpl extends BaseServiceImpl<HrmEmpScheduleMapper, HrmEmpSchedule> implements IHrmEmpScheduleService
{
    @Autowired
    private HrmEmpScheduleMapper scheduleMapper;
    @Override
    public HrmEmpSchedule getEmpScheduleByParams(HashMap<String, Object> params)
    {
        return scheduleMapper.getEmpScheduleByParams(params);
    }
}
