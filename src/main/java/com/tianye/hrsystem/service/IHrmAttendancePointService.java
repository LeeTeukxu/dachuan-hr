package com.tianye.hrsystem.service;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddAttendancePointBO;
import com.tianye.hrsystem.entity.po.HrmAttendancePoint;
import com.tianye.hrsystem.entity.vo.AttendancePointPageListVO;

/**
 * <p>
 * 打卡地址表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-12
 */
public interface IHrmAttendancePointService extends BaseService<HrmAttendancePoint> {
    /**
     * 查询列表数据
     *
     * @param pageEntity
     * @return
     */
    BasePage<AttendancePointPageListVO> queryAttendancePointPageList(PageEntity pageEntity);


    /**
     * 添加地点
     *
     * @param attendancePoint
     */
    HrmAttendancePoint addAttendancePoint(AddAttendancePointBO attendancePoint);
}
