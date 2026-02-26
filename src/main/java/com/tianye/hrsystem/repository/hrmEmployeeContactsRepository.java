package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface hrmEmployeeContactsRepository  extends JpaRepository<HrmEmployeeContacts,Long>  {
    List<com.tianye.hrsystem.entity.po.HrmEmployeeContacts> findAllByEmployeeId(Long EmpID);
   Optional<com.tianye.hrsystem.entity.po.HrmEmployeeContacts> findFirstByContactsId(Long Id);

}
