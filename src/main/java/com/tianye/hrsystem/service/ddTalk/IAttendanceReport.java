package com.tianye.hrsystem.service.ddTalk;

import com.tianye.hrsystem.model.ddTalk.RptAttendanceDetail;
import com.tianye.hrsystem.model.ddTalk.singleDate;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: IGenerator
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月19日 14:42
 **/
public interface IAttendanceReport {
    List<RptAttendanceDetail> Create(Date Begin,Date End) throws Exception;
}
