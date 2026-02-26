package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmEmployeeContractRepository  extends JpaRepository<HrmEmployeeContract,Long>  {
    List<com.tianye.hrsystem.entity.po.HrmEmployeeContract> findAllByEmployeeId(Long EmpID);

}
