package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.model.ddTalk.Ddtaskresult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ddtaskresultRepository  extends JpaRepository<Ddtaskresult,Integer>  {
    Optional<Ddtaskresult> findFirstByProcessedAndCompanyIdOrderByCreatetime(Integer process, String companyId);
    
    // 原有方法：基于empId去重（会导致重名员工被跳过）
    long countAllByEmpIdAndClassNameAndBeginAndEnd(Long empId,String className,String Begin,String End);
    
    // 【修复重名员工问题】：新增基于userId去重的方法，保证每个钉钉用户都能生成考勤数据
    long countAllByUserIdAndClassNameAndBeginAndEnd(String userId,String className,String Begin,String End);
}
