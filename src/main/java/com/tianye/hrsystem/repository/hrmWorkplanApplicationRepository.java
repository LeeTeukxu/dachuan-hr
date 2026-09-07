package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.HrmWorkplanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface hrmWorkplanApplicationRepository extends JpaRepository<HrmWorkplanApplication, Long> {

    List<HrmWorkplanApplication> findByEmployeeIdOrderByCreateTimeDesc(Long employeeId);

    List<HrmWorkplanApplication> findByEmployeeIdAndStatusOrderByCreateTimeDesc(Long employeeId, String status);

    List<HrmWorkplanApplication> findByEmployeeIdAndWorkDate(Long employeeId, Date workDate);

    /** 待我审批：申请人的直属上级 = 当前审批人（通过 employee 的 parent_id 关联，在 service 中按部门过滤） */
    List<HrmWorkplanApplication> findByStatusOrderByCreateTimeDesc(String status);

    List<HrmWorkplanApplication> findByStatusAndWorkDateBetweenOrderByCreateTimeDesc(String status, Date begin, Date end);

    List<HrmWorkplanApplication> findByWorkDateBetweenOrderByCreateTimeDesc(Date begin, Date end);

    List<HrmWorkplanApplication> findByEmployeeIdAndWorkDateBetweenOrderByCreateTimeDesc(Long employeeId, Date begin, Date end);

    /** 某员工某时间段内处于“待审批”状态的申请（用于月历标记 pending） */
    List<HrmWorkplanApplication> findByEmployeeIdAndStatusAndWorkDateBetween(
            Long employeeId, String status, Date begin, Date end);

    /** 待我审批列表：返回申请单（条件：申请人 employee.parent_id = 审批人员工ID，且状态精确匹配） */
    @Query("SELECT a FROM HrmWorkplanApplication a WHERE a.status = :status " +
            "AND a.employeeId IN (SELECT e.employeeId FROM HrmEmployee e WHERE e.parentId = :approverEmployeeId) " +
            "ORDER BY a.createTime DESC")
    List<HrmWorkplanApplication> findToApproveByStatus(@Param("status") String status,
                                                       @Param("approverEmployeeId") Long approverEmployeeId);

    /** 我审批过的：审批人为当前员工且状态为通过/驳回 */
    @Query("SELECT a FROM HrmWorkplanApplication a WHERE a.approverEmployeeId = :approverEmployeeId " +
            "AND a.status <> 'pending' ORDER BY a.approveTime DESC")
    List<HrmWorkplanApplication> findApprovedByApprover(@Param("approverEmployeeId") Long approverEmployeeId);

    /** 可见范围=指定员工集合时：待审批列表（申请人 ∈ ids） */
    @Query("SELECT a FROM HrmWorkplanApplication a WHERE a.status = :status " +
            "AND a.employeeId IN :employeeIds ORDER BY a.createTime DESC")
    List<HrmWorkplanApplication> findToApproveByStatusAndEmployeeIdIn(@Param("status") String status,
                                                                      @Param("employeeIds") List<Long> employeeIds);

    /** 可见范围=指定员工集合时：已处理列表（申请人 ∈ ids 且状态为通过/驳回） */
    @Query("SELECT a FROM HrmWorkplanApplication a WHERE a.status <> 'pending' " +
            "AND a.employeeId IN :employeeIds ORDER BY a.approveTime DESC")
    List<HrmWorkplanApplication> findProcessedByEmployeeIdIn(@Param("employeeIds") List<Long> employeeIds);

    /** 可见范围=全部员工：全部已处理申请 */
    List<HrmWorkplanApplication> findByStatusNotOrderByApproveTimeDesc(String status);
}