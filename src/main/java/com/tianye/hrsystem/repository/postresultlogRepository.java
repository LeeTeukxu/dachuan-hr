package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface postresultlogRepository  extends JpaRepository<Postresultlog,Integer>  {

    List<Postresultlog> findAllByCreateTimeGreaterThanEqualAndCreateTimeLessThan(Date begin, Date end);

    /** 保留期清理：按 createTime 取最旧的一批 id（配合 Pageable 限批） */
    @Query("select p.id from Postresultlog p where p.createTime < :cutoff")
    List<Integer> findIdsByCreateTimeLessThan(@Param("cutoff") Date cutoff, Pageable pageable);

    /** 保留期清理：按 id 批量删除（单批独立事务） */
    @Modifying
    @Query("delete from Postresultlog p where p.id in :ids")
    int deleteByIdIn(@Param("ids") List<Integer> ids);
}
