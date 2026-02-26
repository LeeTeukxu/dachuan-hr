package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface hrmEmployeeCertificateRepository  extends JpaRepository<HrmEmployeeCertificate,Long>  {
    List<com.tianye.hrsystem.entity.po.HrmEmployeeCertificate> findAllByEmployeeId(Long EmpID);
    Optional<com.tianye.hrsystem.entity.po.HrmEmployeeCertificate> findFirstByCertificateId(Long Id);
}
