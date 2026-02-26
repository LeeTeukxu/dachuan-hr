package com.tianye.hrsystem.service;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.SetAttendanceRuleBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceRule;
import com.tianye.hrsystem.entity.vo.AttendanceRulePageListVO;
import com.tianye.hrsystem.entity.vo.OperationLog;

/**
 * <p>
 * 打卡规则表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-10
 */
public interface IHrmAttendanceRuleService extends BaseService<HrmAttendanceRule> {
    /**
     * 查询考勤列表
     *
     * @param pageEntity
     * @return
     */
    BasePage<AttendanceRulePageListVO> queryAttendanceRulePageList(PageEntity pageEntity);

    /**
     * 添加或修改考勤规则
     *
     * @param attendanceRule
     */
    void setAttendanceRule(SetAttendanceRuleBO attendanceRule);

    /**
     * 校验考勤规则名称
     *
     * @param attendanceRule
     */
    void verifyAttendanceRuleName(SetAttendanceRuleBO attendanceRule) ;

    /**
     * 通过考勤规则id删除考勤规则
     *
     * @param attendanceRuleId
     * @return
     */
    OperationLog deleteAttendanceRule(Long attendanceRuleId) ;
}
