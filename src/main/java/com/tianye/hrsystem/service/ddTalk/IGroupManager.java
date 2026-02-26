package com.tianye.hrsystem.service.ddTalk;

import com.taobao.api.ApiException;

/**
 * @ClassName: IGroupManager
 * @Author: 肖新民
 * @*TODO:考勤组
 * @CreateTime: 2024年03月14日 11:41
 **/
public interface IGroupManager {
    void GetAndSave() throws ApiException;
}
