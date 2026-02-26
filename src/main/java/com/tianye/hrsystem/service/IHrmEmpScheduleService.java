package com.tianye.hrsystem.service;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.po.AdminMessage;
import com.tianye.hrsystem.entity.po.HrmEmpSchedule;

import java.util.HashMap;

/**
 * 员工排班信息 服务接口
 */
public interface IHrmEmpScheduleService extends BaseService<HrmEmpSchedule>
{

    /**
     * 根据条件查询员工排班信息
     * @param params
     * @return
     */
    HrmEmpSchedule getEmpScheduleByParams(HashMap<String,Object> params);
}
