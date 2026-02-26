package com.tianye.hrsystem.mapper;


import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.bo.QueryEmployFieldManageBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeFieldManage;
import com.tianye.hrsystem.entity.vo.EmployeeFieldManageVO;

import java.util.List;

/**
 * <p>
 * 自定义字段管理表 Mapper 接口
 * </p>
 *
 * @author guomenghao
 * @since 2021-04-14
 */
public interface HrmEmployeeFieldManageMapper extends BaseMapper<HrmEmployeeFieldManage> {
    /**
     * 查询管理可设置员工字段列表
     *
     * @param queryEmployFieldManageBO
     * @return
     */
    List<EmployeeFieldManageVO> queryEmployeeManageField(QueryEmployFieldManageBO queryEmployFieldManageBO);
}
