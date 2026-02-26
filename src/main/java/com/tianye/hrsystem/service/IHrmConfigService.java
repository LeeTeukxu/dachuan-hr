package com.tianye.hrsystem.service;


import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.modules.salary.entity.HrmConfig;

/**
 * <p>
 * 人力资源配置表 服务类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-13
 */
public interface IHrmConfigService extends BaseService<HrmConfig> {


    void addOrUpdate(HrmConfig hrmConfig);

    void initHrmData();

    void initAttendData();
}
