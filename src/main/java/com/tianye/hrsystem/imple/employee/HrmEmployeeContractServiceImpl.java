package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.po.FileEntity;
import com.tianye.hrsystem.entity.po.HrmEmployeeContract;
import com.tianye.hrsystem.entity.vo.ContractInformationVO;
import com.tianye.hrsystem.enums.HrmActionBehaviorEnum;
import com.tianye.hrsystem.enums.LabelGroupEnum;
import com.tianye.hrsystem.mapper.HrmEmployeeContractMapper;
import com.tianye.hrsystem.service.AdminFileService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeContractService;
import com.tianye.hrsystem.util.TransferUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 员工合同 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Service
public class HrmEmployeeContractServiceImpl extends BaseServiceImpl<HrmEmployeeContractMapper, HrmEmployeeContract> implements IHrmEmployeeContractService {

    @Autowired
    private AdminFileService adminFileService;

    @Resource
    private EmployeeActionRecordServiceImpl employeeActionRecordService;

    @Override
    public List<ContractInformationVO> contractInformation(Long employeeId) {
        List<HrmEmployeeContract> contractList = lambdaQuery().eq(HrmEmployeeContract::getEmployeeId, employeeId).orderByAsc(HrmEmployeeContract::getSort).list();
        List<ContractInformationVO> contractInformationVOList = TransferUtil.transferList(contractList, ContractInformationVO.class);
        contractInformationVOList.forEach(contractInformationVO -> {
            if (StrUtil.isNotEmpty(contractInformationVO.getBatchId())) {
                List<FileEntity> listResult = adminFileService.queryFileList(contractInformationVO.getBatchId());
                contractInformationVO.setFileList(listResult);
            }
        });
        return contractInformationVOList;
    }

    @Override
    public void addOrUpdateContract(HrmEmployeeContract employeeContract) {
        if (employeeContract.getContractId() == null) {
            employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.CONTRACT, employeeContract.getEmployeeId());
        } else {
            HrmEmployeeContract old = getById(employeeContract.getContractId());
            employeeActionRecordService.entityUpdateRecord(LabelGroupEnum.CONTRACT, BeanUtil.beanToMap(old), BeanUtil.beanToMap(employeeContract), employeeContract.getEmployeeId());
        }
        saveOrUpdate(employeeContract);
    }

    @Override
    public void deleteContract(Long contractId) {
        HrmEmployeeContract contract = getById(contractId);
        employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.DELETE, LabelGroupEnum.CONTRACT, contract.getEmployeeId());
        removeById(contractId);
    }

    @Override
    public List<Long> queryToExpireContractCount() {
        return getBaseMapper().queryToExpireContractCount();
    }
}
