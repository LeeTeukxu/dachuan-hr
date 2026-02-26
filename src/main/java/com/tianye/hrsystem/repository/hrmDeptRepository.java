package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.entity.vo.SimpleHrmDeptVO;
import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmDeptRepository  extends JpaRepository<HrmDept,Long>  {
    Integer countByCode(String Code);
    Integer countByCodeAndDeptIdNot(String Code,Long DepId);
    Integer countByParentId(Long ID);
    List<SimpleHrmDeptVO>findAllByDeptIdIn(List<Long> IDS);

    List<HrmDept> findAllByName(String Name);
    List<HrmDept> findAllByParentIdAndName(Long parentId,String Name);

    List<HrmDept> findAllByNameOrderByDeptId(String Name);

    HrmDept getAllByDeptId(Long DeptId);
}
