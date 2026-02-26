package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface tbattendancedetailRepository extends JpaRepository<tbattendancedetail, Long> {
    List<tbattendancedetail> findAllByWorkDateBetween(Date Begin, Date End);
    List<tbattendancedetail> findAllByEmpIdAndWorkDateBetween(Long empId,Date begin,Date end);
}
