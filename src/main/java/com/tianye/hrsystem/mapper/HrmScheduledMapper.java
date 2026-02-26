package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.entity.vo.HrmScheduledVo;

import java.util.HashMap;
import java.util.List;

public interface HrmScheduledMapper
{
    List<HrmScheduledVo> findStartingScheduled();

    HrmScheduledVo selectScheDuleByCode(String code);

    int updateScheduledStatus(HashMap<String,Object> params);
}
