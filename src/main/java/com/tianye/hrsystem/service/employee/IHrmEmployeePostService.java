package com.tianye.hrsystem.service.employee;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.bo.DeleteLeaveInformationBO;
import com.tianye.hrsystem.entity.bo.UpdateInformationBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeCertificate;
import com.tianye.hrsystem.entity.po.HrmEmployeeQuitInfo;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.PostInformationVO;

/**
 * <p>
 * 员工证书 服务类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface IHrmEmployeePostService extends BaseService<HrmEmployeeCertificate> {

    /**
     * 岗位信息
     *
     * @param employeeId
     * @return
     */
    PostInformationVO postInformation(Long employeeId);

    /**
     * 修改岗位信息
     *
     * @param updateInformationBO
     */
    OperationLog updatePostInformation(UpdateInformationBO updateInformationBO);

    /**
     * 办理离职
     *
     * @param quitInfo
     * @return
     */
    OperationLog addOrUpdateLeaveInformation(HrmEmployeeQuitInfo quitInfo);

    /**
     * 取消离职
     *
     * @param deleteLeaveInformationBO
     * @return
     */
    OperationLog deleteLeaveInformation(DeleteLeaveInformationBO deleteLeaveInformationBO);

    /**
     * 岗位档案信息
     *
     * @return
     */
    PostInformationVO postArchives();

}
