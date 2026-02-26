package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmEmployeeDataRepository  extends JpaRepository<HrmEmployeeData,Long>  {
    List<HrmEmployeeData> findAllByEmployeeId(Long employeeId);

    List<HrmEmployeeData> findAllByEmployeeIdAndLabelGroup(Integer EmpID,Integer LabelGroup);
    int deleteAllByEmployeeIdAndLabelGroup(Integer EmployeeId,Integer LabelGroup);
}
