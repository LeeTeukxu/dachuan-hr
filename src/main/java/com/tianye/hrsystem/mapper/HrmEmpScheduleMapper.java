package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.po.HrmEmpSchedule;

import java.util.HashMap;


/**
 * 员工排班信息(HrmEmpSchedule)表数据库访问层
 *
 * @author makejava
 * @since 2024-04-21 10:44:31
 */
public interface HrmEmpScheduleMapper extends BaseMapper<HrmEmpSchedule>
{
    /**
     * 根据条件查询员工排班信息
     * @param params
     * @return
     */
    HrmEmpSchedule getEmpScheduleByParams(HashMap<String,Object> params);
}

