package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryOverTimeRecordPageListBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeOverTimeRecord;
import com.tianye.hrsystem.mapper.HrmEmployeeOverTimeRecordMapper;
import com.tianye.hrsystem.service.employee.IHrmEmployeeOverTimeRecordService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * <p>
 * 员工加班表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2021-09-07
 */
@Service
public class HrmEmployeeOverTimeRecordServiceImpl extends BaseServiceImpl<HrmEmployeeOverTimeRecordMapper, HrmEmployeeOverTimeRecord> implements IHrmEmployeeOverTimeRecordService {

    @Override
    public BasePage<Map<String, Object>> queryOverTimeRecordPageList(QueryOverTimeRecordPageListBO queryOverTimeRecordPageListBO) {
        BasePage<Map<String, Object>> page = getBaseMapper().queryOverTimeRecordPageList(queryOverTimeRecordPageListBO.parse(), queryOverTimeRecordPageListBO);
        return page;
    }
}
