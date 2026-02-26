package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.vo.QueryInItConfigVO;

/**
 * <p>
 * 薪资初始配置 服务类
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
public interface IHrmSalaryConfigService extends BaseService<HrmSalaryConfig> {

    /**
     * 查询薪资/社保初始化配置是否存在
     *
     * @return
     */
    QueryInItConfigVO queryInItConfig();

    /**
     * 保存初始化配置
     *
     * @param salaryConfig
     */
    void saveInitConfig(HrmSalaryConfig salaryConfig);

    /**
     * 修改初始化配置状态为已配置
     *
     * @param type
     */
    void updateInitStatus(Integer type);


}
