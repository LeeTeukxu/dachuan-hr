package com.tianye.hrsystem.repository;


import com.tianye.hrsystem.model.updateRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface updateRecordRepository extends JpaRepository<updateRecord,Long>  {
    int countByMainKeyAndSubKey(String mainKey,String subKey);
    updateRecord findByMainKeyAndSubKey(String mainKey, String subKey);
}
