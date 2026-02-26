package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.po.HrmEmployeeContactsData;
import com.tianye.hrsystem.mapper.HrmEmployeeContactsDataMapper;
import com.tianye.hrsystem.service.employee.IHrmEmployeeContactsDataService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 客户扩展字段数据表 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Service
public class HrmEmployeeContactsDataServiceImpl extends BaseServiceImpl<HrmEmployeeContactsDataMapper, HrmEmployeeContactsData> implements IHrmEmployeeContactsDataService {

    @Override
    public Integer verifyUnique(Long fieldId, String value, Long id) {
        return getBaseMapper().verifyUnique(fieldId, value, id);
    }
}
