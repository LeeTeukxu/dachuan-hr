package com.tianye.hrsystem.modules.salary.service;

import com.alibaba.fastjson.JSON;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.enums.HrmCodeEnum;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.salary.dto.SetChangeTemplateDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryChangeTemplate;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryChangeTemplateMapper;
import com.tianye.hrsystem.modules.salary.vo.ChangeSalaryOptionVO;
import com.tianye.hrsystem.modules.salary.vo.QueryChangeTemplateListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HrmSalaryChangeTemplateService extends BaseServiceImpl<HrmSalaryChangeTemplateMapper, HrmSalaryChangeTemplate>
{
    @Autowired
    private HrmSalaryChangeTemplateMapper salaryChangeTemplateMapper;

    public List<QueryChangeTemplateListVO> queryChangeTemplateList()
    {
        List<HrmSalaryChangeTemplate> list = lambdaQuery().list();
        return list.stream().map(template -> {
            QueryChangeTemplateListVO changeTemplateListVO = new QueryChangeTemplateListVO();
            changeTemplateListVO.setId(template.getId());
            changeTemplateListVO.setTemplateName(template.getTemplateName());
            changeTemplateListVO.setIsDefault(template.getIsDefault());
            List<ChangeSalaryOptionVO> salaryOptionVOS = JSON.parseArray(template.getValue(), ChangeSalaryOptionVO.class);
            changeTemplateListVO.setValue(salaryOptionVOS);
            return changeTemplateListVO;
        }).collect(Collectors.toList());
    }


    public List<ChangeSalaryOptionVO> queryChangeSalaryOption()
    {
        List<ChangeSalaryOptionVO> optionVOS = salaryChangeTemplateMapper.queryChangeSalaryOption();
        return optionVOS;
    }

    public void setChangeTemplate(SetChangeTemplateDto setChangeTemplateDto)
    {
        LoginUserInfo userInfo = CompanyContext.get();
        HrmSalaryChangeTemplate salaryChangeTemplate = new HrmSalaryChangeTemplate();
        salaryChangeTemplate.setId(setChangeTemplateDto.getId());
        salaryChangeTemplate.setTemplateName(setChangeTemplateDto.getTemplateName());
        salaryChangeTemplate.setValue(JSON.toJSONString(setChangeTemplateDto.getValue()));
        salaryChangeTemplate.setCreateTime(LocalDateTime.now());
        salaryChangeTemplate.setCreateUserId(userInfo.getUserId());
        saveOrUpdate(salaryChangeTemplate);
    }

    public void deleteChangeTemplate(Long id)
    {
        HrmSalaryChangeTemplate template = getById(id);
        if (template.getIsDefault() == 1) {
            throw new HrmException(HrmCodeEnum.DEFAULT_TEMPLATE_CANNOT_BE_DELETED);
        }
        removeById(id);
    }
}
