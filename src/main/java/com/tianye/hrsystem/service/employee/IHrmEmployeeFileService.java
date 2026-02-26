package com.tianye.hrsystem.service.employee;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.bo.AddFileBO;
import com.tianye.hrsystem.entity.bo.QueryFileBySubTypeBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeFile;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 员工附件表 服务类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface IHrmEmployeeFileService extends BaseService<HrmEmployeeFile> {

    /**
     * 查询员工总体附件
     *
     * @param employeeId
     * @return
     */
    Map<String, Object> queryFileNum(Long employeeId);

    /**
     * 根据附件类型查询附件详情
     *
     * @param queryFileBySubTypeBO
     * @return
     */
    List<HrmEmployeeFile> queryFileBySubType(QueryFileBySubTypeBO queryFileBySubTypeBO);

    /**
     * 添加附件
     *
     * @param addFileBO
     */
    void addFile(AddFileBO addFileBO);

    /**
     * 删除附件
     *
     * @param employeeFileId
     */
    void deleteFile(Long employeeFileId);
}
