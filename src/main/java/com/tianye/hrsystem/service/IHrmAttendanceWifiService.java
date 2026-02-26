package com.tianye.hrsystem.service;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddAttendanceWifiBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceWifi;
import com.tianye.hrsystem.entity.vo.AttendanceWifiPageListVO;

/**
 * <p>
 * 打卡wifi表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-12
 */
public interface IHrmAttendanceWifiService extends BaseService<HrmAttendanceWifi> {
    /**
     * 查询打卡wifi列表
     *
     * @param pageEntity
     * @return
     */
    BasePage<AttendanceWifiPageListVO> queryAttendanceWifiPageList(PageEntity pageEntity);

    /**
     * 添加打卡wifi
     *
     * @param attendanceWifi
     */
    HrmAttendanceWifi addAttendanceWifi(AddAttendanceWifiBO attendanceWifi);
}
