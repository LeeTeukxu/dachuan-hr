package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface hrmEmployeeChangeRecordRepository  extends JpaRepository<HrmEmployeeChangeRecord,String>  {

}
