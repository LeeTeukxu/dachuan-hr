package com.tianye.hrsystem.modules.salary.service;


import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.salary.dto.AddSlipTemplateBO;
import com.tianye.hrsystem.modules.salary.entity.HrmSalarySlipTemplate;
import com.tianye.hrsystem.modules.salary.entity.HrmSalarySlipTemplateOption;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalarySlipTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 工资表模板 服务实现类
 * </p>
 *
 * @author hmb
 * @since 2020-11-03
 */
@Service
public class HrmSalarySlipTemplateService extends BaseServiceImpl<HrmSalarySlipTemplateMapper, HrmSalarySlipTemplate>
{

    @Autowired
    private HrmSalarySlipTemplateOptionService slipTemplateOptionService;

    @Transactional(rollbackFor = Exception.class)
    public void addSlipTemplate(AddSlipTemplateBO addSlipTemplateBO)
    {
        HrmSalarySlipTemplate slipTemplate = new HrmSalarySlipTemplate();
        slipTemplate.setTemplateName(addSlipTemplateBO.getTemplateName());
        slipTemplate.setHideEmpty(addSlipTemplateBO.getHideEmpty());
        save(slipTemplate);
        List<HrmSalarySlipTemplateOption> slipTemplateOption = addSlipTemplateBO.getSlipTemplateOption();
        for (int i = 0; i < slipTemplateOption.size(); i++) {
            HrmSalarySlipTemplateOption salarySlipTemplateOption = slipTemplateOption.get(i);
            salarySlipTemplateOption.setTemplateId(slipTemplate.getId());
            salarySlipTemplateOption.setType(1);
            salarySlipTemplateOption.setPid(0L);
            salarySlipTemplateOption.setSort(i + 1);
            salarySlipTemplateOption.setId(null);
            slipTemplateOptionService.save(salarySlipTemplateOption);
            List<HrmSalarySlipTemplateOption> optionList = salarySlipTemplateOption.getOptionList();
            for (int j = 0; j < optionList.size(); j++) {
                HrmSalarySlipTemplateOption option = optionList.get(j);
                option.setId(null);
                option.setTemplateId(slipTemplate.getId());
                option.setType(2);
                option.setSort(j + 1);
                option.setPid(salarySlipTemplateOption.getId());
            }
            slipTemplateOptionService.saveBatch(optionList, optionList.size());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSlipTemplate(Long templateId)
    {
        slipTemplateOptionService.lambdaUpdate().eq(HrmSalarySlipTemplateOption::getTemplateId, templateId).remove();
        removeById(templateId);
    }
}
