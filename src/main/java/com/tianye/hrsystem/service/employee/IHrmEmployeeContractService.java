package com.tianye.hrsystem.service.employee;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.po.HrmEmployeeContract;
import com.tianye.hrsystem.entity.vo.ContractInformationVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 员工合同 服务类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface IHrmEmployeeContractService extends BaseService<HrmEmployeeContract> {

    /**
     * 合同基本信息
     *
     * @param employeeId
     * @return
     */
    List<ContractInformationVO> contractInformation(Long employeeId);

    /**
     * 添加或者合同
     *
     * @param employeeContract
     */
    void addOrUpdateContract(HrmEmployeeContract employeeContract);

    /**
     * 导入员工合同
     *
     * @param file 合同导入 Excel
     * @return 导入合同数量
     */
    Integer importContracts(MultipartFile file) throws Exception;

    /**
     * 删除合同
     *
     * @param contractId
     */
    void deleteContract(Long contractId);

    /**
     * 查询合同到期的员工id
     *
     * @return
     */
    List<Long> queryToExpireContractCount();

}
