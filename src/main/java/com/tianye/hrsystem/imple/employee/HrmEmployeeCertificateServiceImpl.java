package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.entity.po.HrmEmployeeCertificate;
import com.tianye.hrsystem.repository.hrmEmployeeCertificateRepository;
import com.tianye.hrsystem.service.employee.IHrmEmployeeCertificateService;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @ClassName: HrmEmployeeCertificateServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月27日 17:04
 **/
@Service
public class HrmEmployeeCertificateServiceImpl implements IHrmEmployeeCertificateService {

    @Autowired
    hrmEmployeeCertificateRepository cerRep;
    @Override
    public HrmEmployeeCertificate getById(Long Id) {
        Optional<HrmEmployeeCertificate> findFs=cerRep.findFirstByCertificateId(Id);
        if(findFs.isPresent()){
            return findFs.get();
        } else return null;
    }
}
