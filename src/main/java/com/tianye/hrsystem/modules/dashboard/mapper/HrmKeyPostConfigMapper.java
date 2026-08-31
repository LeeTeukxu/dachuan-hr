package com.tianye.hrsystem.modules.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianye.hrsystem.modules.dashboard.entity.HrmKeyPostConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface HrmKeyPostConfigMapper extends BaseMapper<HrmKeyPostConfig> {

    List<Map<String, Object>> listPostsWithFlag();
}
