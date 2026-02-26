package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.entity.vo.SimpleHrmEmployeeVO;
import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface hrmEmployeeRepository  extends JpaRepository<HrmEmployee,Long>  {
    Optional<HrmEmployee> findFirstByJobNumber(String JobNumber);
    Optional<HrmEmployee> findFirstByJobNumberAndEmployeeIdNot(String JobNumber,Long EmpID);
    Optional<HrmEmployee> findFirstByMobile(String JobNumber);
    Optional<HrmEmployee> findFirstByMobileAndEmployeeIdNot(String JobNumber,Long EmpID);
    List<SimpleHrmEmployeeVO> findAllByEmployeeNameLike(String EmpName);
    List<SimpleHrmEmployeeVO> findAllByStatusAndIsDel(Integer Status,Integer IsDel);

    Optional<SimpleHrmEmployeeVO> findFirstByEmployeeId(Long EmployeeId);

    List<HrmEmployee> findAllByEmployeeIdIn(List<Long> employeeIds);

    Optional<HrmEmployee> findFirstByEmployeeName(String userName);

    List<HrmEmployee>  findAllByDeptId(Long DeptId);

    List<HrmEmployee> findAllByEmployeeName(String EmployeeName);
}
