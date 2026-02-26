package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface tbattendanceuserRepository  extends JpaRepository<tbattendanceuser,Integer>  {
    Optional<tbattendanceuser> findFirstByUserId(String  UserID);
    List<tbattendanceuser> findAllByEmpIdIn(List<Long> IDS);
    List<tbattendanceuser> findAllByUserName(String UserName);
}
