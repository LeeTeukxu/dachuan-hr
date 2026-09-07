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
    Optional<HrmEmployee> findFirstByOpenid(String openid);
    List<SimpleHrmEmployeeVO> findAllByEmployeeNameLike(String EmpName);
    List<SimpleHrmEmployeeVO> findAllByStatusAndIsDel(Integer Status,Integer IsDel);

    Optional<SimpleHrmEmployeeVO> findFirstByEmployeeId(Long EmployeeId);

    List<HrmEmployee> findAllByEmployeeIdIn(List<Long> employeeIds);

    List<HrmEmployee> findAllByIsDelAndEntryStatusIn(Integer isDel, List<Integer> entryStatuses);

    Optional<HrmEmployee> findFirstByEmployeeName(String userName);

    List<HrmEmployee>  findAllByDeptId(Long DeptId);

    List<HrmEmployee> findAllByEmployeeName(String EmployeeName);

    long countByParentIdAndIsDel(Long parentId, Integer isDel);

    /** 直属下属列表（parent_id = 本人），用于小程序审批可见范围默认档 */
    List<HrmEmployee> findAllByParentIdAndIsDel(Long parentId, Integer isDel);
}
