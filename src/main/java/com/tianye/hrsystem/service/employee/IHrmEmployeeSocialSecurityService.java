package com.tianye.hrsystem.service.employee;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QuerySalaryListBO;
import com.tianye.hrsystem.modules.salary.entity.HrmEmployeeSalaryCard;
import com.tianye.hrsystem.entity.po.HrmEmployeeSocialSecurityInfo;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.QuerySalaryListVO;
import com.tianye.hrsystem.entity.vo.SalaryOptionHeadVO;
import com.tianye.hrsystem.entity.vo.SalarySocialSecurityVO;

import java.util.List;

/**
 * <p>
 * 员工公积金信息 服务类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface IHrmEmployeeSocialSecurityService extends BaseService<HrmEmployeeSocialSecurityInfo> {

    /**
     * 工资社保基本信息
     *
     * @param employeeId
     * @return
     */
    SalarySocialSecurityVO salarySocialSecurityInformation(Long employeeId);

    /**
     * 添加修改工资卡
     *
     * @param salaryCard
     */
    void addOrUpdateSalaryCard(HrmEmployeeSalaryCard salaryCard);

    /**
     * 删除工资卡
     *
     * @param salaryCardId
     */
    void deleteSalaryCard(Long salaryCardId);

    /**
     * 添加修改社保信息
     *
     * @param socialSecurityInfo
     */
    OperationLog addOrUpdateSocialSecurity(HrmEmployeeSocialSecurityInfo socialSecurityInfo);

    /**
     * 删除社保
     *
     * @param socialSecurityInfoId
     */
    void deleteSocialSecurity(Long socialSecurityInfoId);

    /**
     * 查询薪资列表
     *
     * @param querySalaryListBO
     * @return
     */
    BasePage<QuerySalaryListVO> querySalaryList(QuerySalaryListBO querySalaryListBO);

    /**
     * 查询薪资详情
     *
     * @param sEmpRecordId
     * @return
     */
    List<SalaryOptionHeadVO> querySalaryDetail(String sEmpRecordId);
}
