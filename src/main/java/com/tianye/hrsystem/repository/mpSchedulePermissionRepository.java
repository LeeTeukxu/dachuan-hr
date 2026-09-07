package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.MpSchedulePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface mpSchedulePermissionRepository extends JpaRepository<MpSchedulePermission, Long> {

    MpSchedulePermission findByEmployeeId(Long employeeId);

    List<MpSchedulePermission> findByEmployeeIdIn(List<Long> employeeIds);
}
