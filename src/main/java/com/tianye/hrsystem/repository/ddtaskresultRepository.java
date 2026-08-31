package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.model.ddTalk.Ddtaskresult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ddtaskresultRepository  extends JpaRepository<Ddtaskresult,Integer>  {
    Optional<Ddtaskresult> findFirstByProcessedAndCompanyIdOrderByCreatetime(Integer process, String companyId);

    /** 保留期清理：已处理(proces=200)且超期的最旧一批 id（配合 Pageable 限批） */
    @Query("select d.id from Ddtaskresult d where d.processed = :processed and d.createtime < :cutoff")
    List<Integer> findIdsByProcessedAndCreatetimeLessThan(@Param("processed") Integer processed,
                                                          @Param("cutoff") Date cutoff,
                                                          Pageable pageable);

    /** 保留期清理：按 id 批量删除（单批独立事务） */
    @Modifying
    @Query("delete from Ddtaskresult d where d.id in :ids")
    int deleteByIdIn(@Param("ids") List<Integer> ids);
    
    // 原有方法：基于empId去重（会导致重名员工被跳过）
    long countAllByEmpIdAndClassNameAndBeginAndEnd(Long empId,String className,String Begin,String End);
    
    // 【修复重名员工问题】：新增基于userId去重的方法，保证每个钉钉用户都能生成考勤数据
    long countAllByUserIdAndClassNameAndBeginAndEnd(String userId,String className,String Begin,String End);
}
