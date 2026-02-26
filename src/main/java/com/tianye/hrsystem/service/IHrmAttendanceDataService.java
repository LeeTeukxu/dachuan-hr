package com.tianye.hrsystem.service;

import java.util.Date;

/**
 * @ClassName: IHrmAttendanceService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 15:49
 **/
public interface IHrmAttendanceDataService {
    boolean SyncData(String EmpIDS, Date Begin, Date End) throws Exception;
    
    /**
     * 支持断点续传的同步方法
     * @param resumeFromBreakpoint true=从断点恢复, false=从头开始
     */
    boolean SyncDataWithResume(String EmpIDS, Date Begin, Date End, boolean resumeFromBreakpoint) throws Exception;
    
    /**
     * 获取当前同步进度
     */
    java.util.Map<String, Object> getSyncProgress();
    
    /**
     * 清除同步进度缓存
     */
    void clearSyncProgress();
}
