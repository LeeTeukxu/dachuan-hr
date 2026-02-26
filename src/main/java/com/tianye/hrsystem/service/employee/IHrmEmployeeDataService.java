package com.tianye.hrsystem.service.employee;

import com.alibaba.fastjson.JSONObject;
import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.po.HrmEmployeeData;

import java.util.List;

public interface IHrmEmployeeDataService extends BaseService<HrmEmployeeData> {
    /**
     * 查询员工自定义字段值
     *
     * @param employeeId
     * @return
     */
    List<HrmEmployeeData> queryListByEmployeeId(Long  employeeId);


    /**
     * 查询员工自定义字段值
     *
     * @param employeeId
     * @return
     */
    List<JSONObject> queryFiledListByEmployeeId(Long employeeId);

    /**
     * 验证字段唯一
     *
     * @param fieldId
     * @param value
     * @param id
     * @return
     */
    Integer verifyUnique(Long fieldId, String value, Long id);
}
