package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface tbattendancegroupRepository  extends JpaRepository<tbattendancegroup,Integer>  {
    Optional<tbattendancegroup> findFirstByGroupId(Long GroupID);
}
