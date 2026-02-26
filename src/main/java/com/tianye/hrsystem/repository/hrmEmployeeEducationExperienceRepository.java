package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface hrmEmployeeEducationExperienceRepository  extends JpaRepository<HrmEmployeeEducationExperience,Long>  {
    Optional<HrmEmployeeEducationExperience> findFirstByEducationAndEmployeeId(Integer Edu,Long EmpID);
}
