package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryConfigDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryConfigMapper;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryConfigVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * //计薪设置 服务实现类
 */
@Service
public class HrmSalaryConfigService extends BaseServiceImpl<HrmSalaryConfigMapper, HrmSalaryConfig>
{

    @Autowired
    private HrmSalaryConfigMapper hrmSalaryConfigMapper;

    /**
     * 计薪设置列表
     * @param querySalaryConfigDto
     * @return
     */
    public Page<QuerySalaryConfigVO> querySalaryConfig  (QuerySalaryConfigDto querySalaryConfigDto)
    {
        Page<QuerySalaryConfigVO> page = hrmSalaryConfigMapper.querySalaryConfig(querySalaryConfigDto.parse(), querySalaryConfigDto);
        return page;
    }

    /**
     * 根据主键ID删除计薪设置
     * @param id
     * @return
     */
    public void deleteSalaryConfig(Long id) {
        hrmSalaryConfigMapper.deleteSalaryConfig(id);
    }

    /**
     * 保存计薪设置
     * @param querySalaryConfigDto
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveSalaryConfig(QuerySalaryConfigDto querySalaryConfigDto) {
        HrmSalaryConfig salaryConfig = BeanUtil.copyProperties(querySalaryConfigDto, HrmSalaryConfig.class);
        LoginUserInfo info = CompanyContext.get();

        if (salaryConfig.getConfigId() != null) {
            LambdaUpdateWrapper<HrmSalaryConfig> wrapper = new LambdaUpdateWrapper<HrmSalaryConfig>()
                    .eq(HrmSalaryConfig::getConfigId, salaryConfig.getConfigId());
            salaryConfig.setUpdateUserId(Long.parseLong(info.getUserId()));
            salaryConfig.setUpdateTime(LocalDateTime.now());
            update(salaryConfig, wrapper);
        }else {
            salaryConfig.setCreateUserId(Long.parseLong(info.getUserId()));
            salaryConfig.setCreateTime(LocalDateTime.now());
            save(salaryConfig);
        }
    }

    /**
     * 根据id查询计薪设置
     * @param id
     * @return
     */
    public QuerySalaryConfigVO queryById(String id) {
        QuerySalaryConfigVO querySalaryConfigVO = new QuerySalaryConfigVO();
        HrmSalaryConfig hrmSalaryConfig = getById(id);
        if (hrmSalaryConfig == null) {
            return querySalaryConfigVO;
        }
        querySalaryConfigVO.setConfigId(hrmSalaryConfig.getConfigId());
        querySalaryConfigVO.setSalaryCycleStartDay(hrmSalaryConfig.getSalaryCycleStartDay());
        querySalaryConfigVO.setSalaryCycleEndDay(hrmSalaryConfig.getSalaryCycleEndDay());
        querySalaryConfigVO.setPayType(hrmSalaryConfig.getPayType());
        querySalaryConfigVO.setPayDay(hrmSalaryConfig.getPayDay());
        querySalaryConfigVO.setSocialSecurityMonthType(hrmSalaryConfig.getSocialSecurityMonthType());
        querySalaryConfigVO.setSalaryStartMonth(hrmSalaryConfig.getSalaryStartMonth());
        querySalaryConfigVO.setSocialSecurityStartMonth(hrmSalaryConfig.getSocialSecurityStartMonth());
        return querySalaryConfigVO;
    }

    /**
     * 查询计薪设置
     * @return
     */
    public QuerySalaryConfigVO findAll() {
        QuerySalaryConfigVO querySalaryConfigVO = new QuerySalaryConfigVO();
        List<HrmSalaryConfig> findOne = list();
        if (findOne.size() <= 0) {
            return querySalaryConfigVO;
        }
        for (HrmSalaryConfig hrmSalaryConfig : findOne) {
            querySalaryConfigVO.setConfigId(hrmSalaryConfig.getConfigId());
            querySalaryConfigVO.setSalaryCycleStartDay(hrmSalaryConfig.getSalaryCycleStartDay());
            querySalaryConfigVO.setSalaryCycleEndDay(hrmSalaryConfig.getSalaryCycleEndDay());
            querySalaryConfigVO.setPayType(hrmSalaryConfig.getPayType());
            querySalaryConfigVO.setPayDay(hrmSalaryConfig.getPayDay());
            querySalaryConfigVO.setSocialSecurityMonthType(hrmSalaryConfig.getSocialSecurityMonthType());
            querySalaryConfigVO.setSalaryStartMonth(hrmSalaryConfig.getSalaryStartMonth());
            querySalaryConfigVO.setSocialSecurityStartMonth(hrmSalaryConfig.getSocialSecurityStartMonth());
        }
        return querySalaryConfigVO;
    }
}
