package com.tianye.hrsystem.service.employee;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.bo.QueryEmployFieldManageBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeFieldManage;
import com.tianye.hrsystem.entity.vo.EmployeeFieldManageVO;

import java.util.List;

public interface IHrmEmployeeFieldManageService extends BaseService<HrmEmployeeFieldManage> {
    /**
     * 查询管理可设置员工字段列表
     *
     * @param queryEmployFieldManageBO
     * @return
     */
    List<EmployeeFieldManageVO> queryEmployeeManageField(QueryEmployFieldManageBO queryEmployFieldManageBO);

    /**
     * 修改管理可以设置员工字段
     *
     * @param manageFields
     */
    void setEmployeeManageField(List<EmployeeFieldManageVO> manageFields);

    /**
     * 查询管理员不可见字段
     *
     * @param entryStatus
     * @return
     */
    List<HrmEmployeeFieldManage> queryManageField(Integer entryStatus);
}