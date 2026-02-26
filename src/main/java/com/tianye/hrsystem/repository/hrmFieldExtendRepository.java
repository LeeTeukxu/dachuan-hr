package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmFieldExtendRepository  extends JpaRepository<HrmFieldExtend,Integer>  {
    List<HrmFieldExtend> findAllByParentFieldId(Integer ParentID);
}
