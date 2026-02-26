package com.tianye.hrsystem.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.po.HrmEmployeeFile;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * 员工附件表 Mapper 接口
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface HrmEmployeeFileMapper extends BaseMapper<HrmEmployeeFile> {

    @InterceptorIgnore(tenantLine = "true")
    Map<String, Object> queryFileNum(@Param("employeeId") Long employeeId);
}
