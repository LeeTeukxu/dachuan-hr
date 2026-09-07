package com.tianye.hrsystem.service.employee;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.po.HrmEmployeeContract;
import com.tianye.hrsystem.entity.vo.ContractInformationVO;
import com.tianye.hrsystem.entity.vo.DuplicateContractVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
     * @return 导入结果：importedCount-导入数量，skippedCount-跳过重复数量，skippedDetails-跳过详情
     */
    Map<String, Object> importContracts(MultipartFile file) throws Exception;

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

    /**
     * 查询重复合同列表
     * 按 employeeId + contractType + startTime + endTime 分组
     *
     * @return 重复合同分组列表
     */
    List<DuplicateContractVO> queryDuplicateContracts();

    /**
     * 批量删除重复合同（保留每组最新的合同）
     *
     * @param contractIds 要删除的合同ID列表
     * @return 删除数量
     */
    Integer deleteDuplicateContracts(List<Long> contractIds);

}
