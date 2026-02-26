package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryBasicMapper;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryBasicDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * //基本工资表 服务实现类
 */
@Service
public class HrmSalaryBasicService extends BaseServiceImpl<HrmSalaryBasicMapper, HrmSalaryBasic>
{

    @Autowired
    private HrmSalaryBasicMapper hrmSalaryBasicMapper;

    /**
     * 基本工资列表
     * @param querySalaryBasicDto
     * @return
     */
    public Page<QuerySalaryBasicVO> querySalaryBasic(QuerySalaryBasicDto querySalaryBasicDto)
    {
        Page<QuerySalaryBasicVO> page = hrmSalaryBasicMapper.querySalaryBasic(querySalaryBasicDto.parse(), querySalaryBasicDto);
        return page;
    }

    /**
     * 根据主键ID删除基本工资
     * @param id
     * @return
     */
    public void deleteSalaryBasic(Long id) {
        hrmSalaryBasicMapper.deleteSalaryBasic(id);
    }

    /**
     * 保存基本工资
     * @param querySalaryBasicDto
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveSalaryBasic(QuerySalaryBasicDto querySalaryBasicDto) {
        HrmSalaryBasic salaryBasic = BeanUtil.copyProperties(querySalaryBasicDto, HrmSalaryBasic.class);
        LoginUserInfo info = CompanyContext.get();

        if (salaryBasic.getId() != null) {
            LambdaUpdateWrapper<HrmSalaryBasic> wrapper = new LambdaUpdateWrapper<HrmSalaryBasic>()
                    .eq(HrmSalaryBasic::getId, salaryBasic.getId());
            salaryBasic.setUpdateUserId(Long.parseLong(info.getUserId()));
            salaryBasic.setUpdateTime(LocalDateTime.now());
            update(salaryBasic, wrapper);
        }else {
            salaryBasic.setCreateUserId(Long.parseLong(info.getUserId()));
            salaryBasic.setCreateTime(LocalDateTime.now());
            save(salaryBasic);
        }
    }

    /**
     * 根据id查询基本工资
     * @param id
     * @return
     */
    public QuerySalaryBasicVO queryById(String id) {
        QuerySalaryBasicVO querySalaryBasicVO = new QuerySalaryBasicVO();
        HrmSalaryBasic hrmSalaryBasic = getById(id);
        if (hrmSalaryBasic == null) {
            return querySalaryBasicVO;
        }
        querySalaryBasicVO.setId(hrmSalaryBasic.getId());
        querySalaryBasicVO.setDeptId(hrmSalaryBasic.getDeptId());
        querySalaryBasicVO.setSalaryBasic(hrmSalaryBasic.getSalaryBasic());
        querySalaryBasicVO.setOvertimePay(hrmSalaryBasic.getOvertimePay());
        querySalaryBasicVO.setSubsidy(hrmSalaryBasic.getSubsidy());
        return querySalaryBasicVO;
    }

    /**
     * 查询基本工资
     * @param id
     * @return
     */
    public QuerySalaryBasicVO findAll() {
        QuerySalaryBasicVO querySalaryBasicVO = new QuerySalaryBasicVO();
        List<HrmSalaryBasic> findOne = list();
        if (findOne.size() <= 0) {
            return querySalaryBasicVO;
        }
        for (HrmSalaryBasic hrmSalaryBasic : findOne) {
            querySalaryBasicVO.setId(hrmSalaryBasic.getId());
            querySalaryBasicVO.setDeptId(hrmSalaryBasic.getDeptId());
            querySalaryBasicVO.setSalaryBasic(hrmSalaryBasic.getSalaryBasic());
            querySalaryBasicVO.setOvertimePay(hrmSalaryBasic.getOvertimePay());
            querySalaryBasicVO.setSubsidy(hrmSalaryBasic.getSubsidy());
        }
        return querySalaryBasicVO;
    }
}
