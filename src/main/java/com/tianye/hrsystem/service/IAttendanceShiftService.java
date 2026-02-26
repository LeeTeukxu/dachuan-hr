package com.tianye.hrsystem.service;

import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.entity.bo.SetAttendanceShiftBO;
import com.tianye.hrsystem.entity.param.QueryAttendanceShiftParameter;
import com.tianye.hrsystem.model.HrmAttendanceShift;

/**
 * @ClassName: IAttendanceShiftService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月20日 23:12
 **/
public interface IAttendanceShiftService {
    void Save(SetAttendanceShiftBO vo) throws Exception;
    PageObject<HrmAttendanceShift> GetPageList(QueryAttendanceShiftParameter param);

    void DeleteOne(Long ID) throws Exception;
}
