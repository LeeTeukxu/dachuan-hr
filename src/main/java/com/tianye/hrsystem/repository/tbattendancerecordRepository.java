package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface tbattendancerecordRepository  extends JpaRepository<tbattendancerecord,Long>  {
    Optional<tbattendancerecord> findFirstByUserIdAndTimeResultAndUserCheckTime(String UserID, String TimeResult,
            Date CheckTime);
}
