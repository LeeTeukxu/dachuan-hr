package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmEmployeeFieldRepository  extends JpaRepository<HrmEmployeeField,Integer>  {
    List<HrmEmployeeField> findAllByIsHiddenAndLabelGroupOrderBySorting(Integer IsHidden,Integer group);
}
