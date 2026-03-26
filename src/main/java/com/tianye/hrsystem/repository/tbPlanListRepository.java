package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface tbPlanListRepository  extends JpaRepository<tbplanlist,Integer>  {
    Page<tbplanlist> findAllByGroupId(String  groupId,Pageable pageable);
    Page<tbplanlist> findAllByGroupIdAndWorkDateBetween(String groupId, Date begin,Date end,Pageable pageable);
    Page<tbplanlist> findAllByWorkDateBetween(Date begin,Date end,Pageable pageable);
    List<tbplanlist> findAllByGroupIdAndWorkDateBetweenOrderByProductNameAsc(String groupId, Date begin, Date end);
    tbplanlist findTopByWorkDateLessThanOrderByWorkDateDesc(Date workDate);
}
