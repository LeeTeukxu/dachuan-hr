package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface tbattendanceapproveRepository  extends JpaRepository<tbattendanceapprove,String>  {

    long countByBeginTimeBetween(Date begin, Date end);

    long countByBeginTimeBetweenAndUserIdIn(Date begin, Date end, List<String> userIds);

    @Query("select distinct a.subType from tbattendanceapprove a where a.subType is not null and a.subType <> '' order by a.subType")
    List<String> findDistinctSubTypes();

    @Query("select a from tbattendanceapprove a " +
            "where a.userId in :userIds and (" +
            "  (a.workDate is not null and a.workDate between :beginDate and :endDate) or " +
            "  (a.workDate is null and a.beginTime is not null and a.beginTime between :beginDate and :endDate) or " +
            "  (a.workDate is null and a.beginTime is null and a.endTime is not null and a.endTime between :beginDate and :endDate)" +
            ")")
    List<tbattendanceapprove> findAllByUserIdInAndWorkDateBetween(@Param("userIds") List<String> userIds,
                                                                  @Param("beginDate") Date beginDate,
                                                                  @Param("endDate") Date endDate);

    @Transactional
    long deleteByBeginTimeBetweenAndUserIdInAndTagNameIn(Date begin, Date end, List<String> userIds, List<String> tagNames);

    @Transactional
    @Modifying
    @Query("delete from tbattendanceapprove a " +
            "where a.beginTime between :begin and :end " +
            "and a.userId in :userIds " +
            "and a.tagName in :tagNames " +
            "and a.id not in :retainedIds")
    int deleteByBeginTimeBetweenAndUserIdInAndTagNameInAndIdNotIn(@Param("begin") Date begin,
                                                                  @Param("end") Date end,
                                                                  @Param("userIds") List<String> userIds,
                                                                  @Param("tagNames") List<String> tagNames,
                                                                  @Param("retainedIds") List<String> retainedIds);
}
