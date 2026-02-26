package com.tianye.hrsystem.service.employee;

import com.tianye.hrsystem.entity.po.HrmEmployeeCertificate;

/**
 * @ClassName: IHrmEmployeeCertificateService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月27日 17:01
 **/
public interface IHrmEmployeeCertificateService {
    HrmEmployeeCertificate getById(Long Id);
}
