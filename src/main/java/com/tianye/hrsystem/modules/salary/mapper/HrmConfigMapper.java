package com.tianye.hrsystem.modules.salary.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.salary.entity.HrmConfig;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 人力资源配置表 Mapper 接口
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-13
 */
public interface HrmConfigMapper extends BaseMapper<HrmConfig> {
    /**
     * @param tableName
     */
    void removeAllData(@Param("tableName") String tableName);
}
