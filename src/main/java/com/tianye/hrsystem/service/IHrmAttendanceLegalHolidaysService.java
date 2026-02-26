package com.tianye.hrsystem.service;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.bo.SetLegalHolidaysBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceLegalHolidays;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 考勤法定节假日 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-23
 */
public interface IHrmAttendanceLegalHolidaysService extends BaseService<HrmAttendanceLegalHolidays> {
    /**
     * 添加法定节假日
     *
     * @param setLegalHolidaysB0
     */
    void addLegalHolidays(SetLegalHolidaysBO setLegalHolidaysB0);

    /**
     * 通过日期查询法定节假日
     *
     * @param day
     * @return
     */
    HrmAttendanceLegalHolidays checkIsLegalHolidays(LocalDateTime day);

    /**
     * 查询法定节假日列表
     *
     * @return
     */
    List<HrmAttendanceLegalHolidays> queryLegalHolidayList();
}
