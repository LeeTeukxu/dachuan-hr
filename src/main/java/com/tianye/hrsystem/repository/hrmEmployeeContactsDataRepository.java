package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.HrmEmployeeContactsData;
import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmEmployeeContactsDataRepository  extends JpaRepository<HrmEmployeeContactsData,Long>  {
    List<HrmEmployeeContactsData> findAllByContactsId(Long CID);
    int deleteAllByContactsIdAndLabelGroup(Long Id,Integer Group);
    int deleteAllByContactsId(Long Id);
}
