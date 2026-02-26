package com.tianye.hrsystem.service.employee;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryOverTimeRecordPageListBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeOverTimeRecord;

import java.util.Map;

/**
 * <p>
 * 员工加班表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2021-09-07
 */
public interface IHrmEmployeeOverTimeRecordService extends BaseService<HrmEmployeeOverTimeRecord> {
    /**
     * 查询加班列表
     *
     * @param queryOverTimeRecordPageListBO
     * @return
     */
    BasePage<Map<String, Object>> queryOverTimeRecordPageList(QueryOverTimeRecordPageListBO queryOverTimeRecordPageListBO);
}
