package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.collection.CollectionUtil;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.LanguageFieldUtil;
import com.tianye.hrsystem.entity.bo.QueryEmployFieldManageBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeFieldManage;
import com.tianye.hrsystem.entity.vo.EmployeeFieldManageVO;
import com.tianye.hrsystem.mapper.HrmEmployeeFieldManageMapper;
import com.tianye.hrsystem.service.employee.IHrmEmployeeFieldManageService;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * <p>
 * 自定义字段管理表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2021-04-14
 */
@Service
public class HrmEmployeeFieldManageServiceImpl extends BaseServiceImpl<HrmEmployeeFieldManageMapper, HrmEmployeeFieldManage> implements IHrmEmployeeFieldManageService {

    @Override
    public List<EmployeeFieldManageVO> queryEmployeeManageField(QueryEmployFieldManageBO queryEmployFieldManageBO) {
        List<EmployeeFieldManageVO> manageVOS = getBaseMapper().queryEmployeeManageField(queryEmployFieldManageBO);
        if (CollectionUtil.isNotEmpty(manageVOS)) {
            for (EmployeeFieldManageVO manageVO : manageVOS) {
                //添加语言包key
                manageVO.setLanguageKeyMap(LanguageFieldUtil.getFieldNameKeyMap("name_resourceKey", "customField.hrmField.", manageVO.getFieldName()));
            }

        }
        return manageVOS;
    }

    @Override
    public void setEmployeeManageField(List<EmployeeFieldManageVO> manageFields) {
        List<HrmEmployeeFieldManage> employeeManageFields = manageFields.stream().map(field -> {
            HrmEmployeeFieldManage hrmEmployeeFieldManage = new HrmEmployeeFieldManage();
            hrmEmployeeFieldManage.setId(field.getId());
            hrmEmployeeFieldManage.setIsManageVisible(field.getIsManageVisible());
            return hrmEmployeeFieldManage;
        }).collect(toList());
        updateBatchById(employeeManageFields);
    }

    @Override
    public List<HrmEmployeeFieldManage> queryManageField(Integer entryStatus) {
        List<HrmEmployeeFieldManage> employeeFieldManages = lambdaQuery().eq(HrmEmployeeFieldManage::getEntryStatus, entryStatus).list();
        return employeeFieldManages;
    }
}
