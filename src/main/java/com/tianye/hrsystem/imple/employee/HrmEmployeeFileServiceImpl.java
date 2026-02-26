package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.bean.BeanUtil;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.bo.AddFileBO;
import com.tianye.hrsystem.entity.bo.QueryFileBySubTypeBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeFile;
import com.tianye.hrsystem.enums.HrmActionBehaviorEnum;
import com.tianye.hrsystem.mapper.HrmEmployeeFileMapper;
import com.tianye.hrsystem.service.AdminFileService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 员工附件表 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Service
public class HrmEmployeeFileServiceImpl extends BaseServiceImpl<HrmEmployeeFileMapper, HrmEmployeeFile> implements IHrmEmployeeFileService {

    @Autowired
    private HrmEmployeeFileMapper employeeFileMapper;

    @Autowired
    private AdminFileService adminFileService;

    @Resource
    private EmployeeActionRecordServiceImpl employeeActionRecordService;

    @Override
    public Map<String, Object> queryFileNum(Long employeeId) {
        return employeeFileMapper.queryFileNum(employeeId);
    }

    @Override
    public List<HrmEmployeeFile> queryFileBySubType(QueryFileBySubTypeBO queryFileBySubTypeBO) {
        List<HrmEmployeeFile> list = lambdaQuery().eq(HrmEmployeeFile::getEmployeeId, queryFileBySubTypeBO.getEmployeeId())
                .eq(HrmEmployeeFile::getSubType, queryFileBySubTypeBO.getSubType())
                .list();
        list.forEach(employeeFile -> {
            employeeFile.setFile(adminFileService.queryById(Long.valueOf(employeeFile.getFileId())));
        });
        return list;
    }

    @Override
    public void addFile(AddFileBO addFileBO) {
        HrmEmployeeFile employeeFile = BeanUtil.copyProperties(addFileBO, HrmEmployeeFile.class);
        save(employeeFile);
        employeeActionRecordService.addFileRecord(employeeFile, HrmActionBehaviorEnum.ADD);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long employeeFileId) {
        HrmEmployeeFile employeeFile = getById(employeeFileId);
        Long fileId = Long.valueOf(employeeFile.getFileId());
        adminFileService.deleteById(fileId);
        removeById(employeeFileId);
        employeeActionRecordService.addFileRecord(employeeFile, HrmActionBehaviorEnum.DELETE);
    }
}
