package com.tianye.hrsystem.mapper;


import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryOverTimeRecordPageListBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeOverTimeRecord;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.Map;

/**
 * <p>
 * 员工加班表 Mapper 接口
 * </p>
 *
 * @author guomenghao
 * @since 2021-09-07
 */
public interface HrmEmployeeOverTimeRecordMapper extends BaseMapper<HrmEmployeeOverTimeRecord> {
    /**
     * 查询加班记录列表
     *
     * @param page
     * @param queryOverTimeRecordPageListBO
     * @return
     */
    BasePage<Map<String, Object>> queryOverTimeRecordPageList(BasePage<Map<String, Object>> page, @Param("data") QueryOverTimeRecordPageListBO queryOverTimeRecordPageListBO);


}
