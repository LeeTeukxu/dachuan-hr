package com.tianye.hrsystem.service.employee;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.po.HrmEmployeeContacts;

/**
 * @ClassName: IHrmEmployeeContactsService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月27日 22:55
 **/
public interface IHrmEmployeeContactsService extends BaseService<HrmEmployeeContacts> {

    Integer verifyUnique(String fieldName, String value, Long contactsId);
}
