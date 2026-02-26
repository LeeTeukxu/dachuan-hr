package com.tianye.hrsystem.service;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryHrmAttendanceGroupBO;
import com.tianye.hrsystem.entity.bo.SetAttendanceGroupBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceGroup;
import com.tianye.hrsystem.entity.vo.HrmAttendanceGroupVO;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.QueryMyAttendanceGroupVO;
import com.tianye.hrsystem.model.tbattendanceuser;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * 考勤组表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-13
 */
public interface IHrmAttendanceGroupService extends BaseService<HrmAttendanceGroup> {
    /**
     * 查询考勤组列表
     *
     * @param queryHrmAttendanceGroupBO
     * @return
     */
    BasePage<HrmAttendanceGroupVO> queryAttendanceGroupPageList(QueryHrmAttendanceGroupBO queryHrmAttendanceGroupBO);

    /**
     * 新增或修改考勤组
     *
     * @param attendanceGroup
     */
    void setAttendanceGroup(SetAttendanceGroupBO attendanceGroup);

    /**
     * 校验考勤组名称
     *
     * @param queryHrmAttendanceGroupBO
     * @return
     */
    void verifyAttendanceGroupName(QueryHrmAttendanceGroupBO queryHrmAttendanceGroupBO);

    /**
     * 查询员工属于考勤组
     *
     * @param employeeId
     * @return
     */
    HrmAttendanceGroup queryAttendanceGroup(Long employeeId);

    /**
     * 从钉钉数据里面查询员工所属考勤组
     * @param employeeId
     * @return
     */
    HrmAttendanceGroup queryAttendanceGroupDingDing(Long employeeId);

    /**
     * 批量查询在钉钉考勤组中的员工ID集合
     *
     * @param employeeIds 员工ID列表
     * @return 命中钉钉考勤组的员工ID集合
     */
    Set<Long> queryEmployeeIdsInAttendanceGroupDingDing(Collection<Long> employeeIds);

    /**
     * 查询我的考勤分组
     *
     * @param employeeId
     * @return
     */
    QueryMyAttendanceGroupVO queryMyAttendanceGroup(Long employeeId);

    /**
     * 通过考勤组id删除考勤组
     *
     * @param attendanceGroupId
     */
    OperationLog deleteAttendanceGroup(Long attendanceGroupId);

    /**
     * 变更考勤组相关考勤组
     *
     * @param hrmAttendanceGroup
     */
    void changeAttendanceGroup(HrmAttendanceGroup hrmAttendanceGroup);

    /**
     * 为未初始化今日之前考勤班次的员工设置出勤班次
     */
    void setAttendanceDateShiftByGroup();

    /**
     * 查询是否存在默认考勤组
     */
    void checkInitAttendData();
}
