package com.tianye.hrsystem.modules.salary.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryConfigMapper;
import com.tianye.hrsystem.modules.salary.service.IHrmSalaryConfigService;
import com.tianye.hrsystem.modules.salary.vo.QueryInItConfigVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <p>
 * 薪资初始配置 服务实现类
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
@Service
public class HrmSalaryConfigServiceImpl extends BaseServiceImpl<HrmSalaryConfigMapper, HrmSalaryConfig> implements IHrmSalaryConfigService {



    @Autowired
    private HrmSalaryConfigMapper hrmSalaryConfigMapper;
    @Override
    public QueryInItConfigVO queryInItConfig() {
        QueryInItConfigVO data = new QueryInItConfigVO();

        HrmSalaryConfig config = new HrmSalaryConfig();
        config.setConfigId(111L);
        hrmSalaryConfigMapper.insert(config);
        List<HrmSalaryConfig> configList = hrmSalaryConfigMapper.queryList(null);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInitConfig(HrmSalaryConfig salaryConfig) {
        HrmSalaryConfig one = getOne(Wrappers.emptyWrapper());

    }

    @Override
    public void updateInitStatus(Integer type) {

    }


}
