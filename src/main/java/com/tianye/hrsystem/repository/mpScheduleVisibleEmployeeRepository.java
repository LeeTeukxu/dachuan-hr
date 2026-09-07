package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.MpScheduleVisibleEmployee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface mpScheduleVisibleEmployeeRepository extends JpaRepository<MpScheduleVisibleEmployee, Long> {

    List<MpScheduleVisibleEmployee> findByPermissionEmployeeId(Long permissionEmployeeId);

    void deleteByPermissionEmployeeId(Long permissionEmployeeId);
}
