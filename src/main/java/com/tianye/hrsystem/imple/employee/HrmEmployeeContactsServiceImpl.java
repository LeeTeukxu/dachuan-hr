package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.po.HrmEmployeeContacts;
import com.tianye.hrsystem.mapper.HrmEmployeeContactsMapper;
import com.tianye.hrsystem.service.employee.IHrmEmployeeContactsService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 员工联系人 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Service
public class HrmEmployeeContactsServiceImpl extends BaseServiceImpl<HrmEmployeeContactsMapper, HrmEmployeeContacts> implements IHrmEmployeeContactsService {

    @Override
    public Integer verifyUnique(String fieldName, String value, Long contactsId) {
        return getBaseMapper().verifyUnique(fieldName, value, contactsId);
    }
}
