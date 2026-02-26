package com.tianye.hrsystem.service.ddTalk;

import java.util.Date;

/**
 * @ClassName: IRecordManager
 * @Author: 肖新民
 * @*TODO: 考勤记录获取
 * @CreateTime: 2024年03月14日 11:42
 **/
public interface IRecordManager {
    void GetAndSave(Date Begin,Date End) throws Exception;
}
