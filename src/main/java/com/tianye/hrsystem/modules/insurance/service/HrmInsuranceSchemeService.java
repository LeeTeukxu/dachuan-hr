package com.tianye.hrsystem.modules.insurance.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Range;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.AddInsuranceSchemeBO;
import com.tianye.hrsystem.entity.po.HrmEmployeeSocialSecurityInfo;
import com.tianye.hrsystem.entity.vo.InsuranceSchemeVO;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.enums.BehaviorEnum;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.insurance.dto.InsuranceSchemeDto;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceProject;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceScheme;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceProjectMapper;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceSechemeMapper;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceSchemeListVO;
import com.tianye.hrsystem.util.TransferUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 社保方案表(HrmInsuranceScheme)表服务接口
 *
 * @author makejava
 * @since 2024-03-23 11:18:45
 */
@Service
public class HrmInsuranceSchemeService extends BaseServiceImpl<HrmInsuranceSechemeMapper, HrmInsuranceScheme> {

    @Autowired
    private HrmInsuranceProjectService insuranceProjectService;

    @Autowired
    private HrmInsuranceSechemeMapper insuranceSechemeMapper;

    @Autowired
    private HrmInsuranceProjectMapper insuranceProjectMapper;

    @Autowired
    private HrmEmployeeSocialSecurityService employeeSocialSecurityService;

    @Autowired
    private HrmInsuranceMonthEmpRecordService insuranceMonthEmpRecordService;

    public BasePage<InsuranceSchemeListVO> index(@RequestBody PageEntity pageEntity) {
       BasePage<InsuranceSchemeListVO> page = insuranceSechemeMapper.index(pageEntity.parse());
       return page;
   }

    public InsuranceSchemeVO queryInsuranceSchemeById(Long schemeId) {
        HrmInsuranceScheme insuranceScheme = getById(schemeId);
        List<HrmInsuranceProject> projectList = insuranceProjectService.lambdaQuery().eq(HrmInsuranceProject::getSchemeId, schemeId).list();
        InsuranceSchemeVO insuranceSchemeVO = BeanUtil.copyProperties(insuranceScheme, InsuranceSchemeVO.class);
        List<AddInsuranceSchemeBO.HrmInsuranceProjectBO> hrmInsuranceProjectBOS = TransferUtil.transferList(projectList, InsuranceSchemeVO.HrmInsuranceProjectBO.class);
        Range<Integer> socialSecurityClosed = Range.closed(1, 9);
        Range<Integer> providentFundClosed = Range.closed(10, 11);
        List<AddInsuranceSchemeBO.HrmInsuranceProjectBO> socialSecurityList = new ArrayList<>();
        List<AddInsuranceSchemeBO.HrmInsuranceProjectBO> providentFundList = new ArrayList<>();
        hrmInsuranceProjectBOS.forEach(project -> {
            if (socialSecurityClosed.contains(project.getType())) {
                socialSecurityList.add(project);
            } else if (providentFundClosed.contains(project.getType())) {
                providentFundList.add(project);
            }
        });
        insuranceSchemeVO.setSocialSecurityProjectList(socialSecurityList);
        insuranceSchemeVO.setProvidentFundProjectList(providentFundList);
        Map<String, String> keyMap = new HashMap<>();
        return insuranceSchemeVO;
    }

   public void deleteInsuranceScheme(Long schemeId) {
        insuranceSechemeMapper.deleteBySchemeId(schemeId);
        insuranceProjectMapper.deleteBySchemeId(schemeId);
   }


    @Transactional(rollbackFor = Exception.class)
    public void saveInsuranceProject(InsuranceSchemeDto schemeDto) {
        HrmInsuranceScheme insuranceScheme = BeanUtil.copyProperties(schemeDto, HrmInsuranceScheme.class);
        LoginUserInfo info = CompanyContext.get();

        if (insuranceScheme.getSchemeId() != null) {
            LambdaUpdateWrapper<HrmInsuranceScheme> wrapper = new LambdaUpdateWrapper<HrmInsuranceScheme>()
                    .eq(HrmInsuranceScheme:: getSchemeId,insuranceScheme.getSchemeId());
            insuranceScheme.setUpdateUserId(Long.parseLong(info.getUserId()));
            insuranceScheme.setUpdateTime(LocalDateTime.now());
            update(insuranceScheme, wrapper);
        }else {
            insuranceScheme.setCreateUserId(Long.parseLong(info.getUserId()));
            insuranceScheme.setCreateTime(LocalDateTime.now());
            save(insuranceScheme);
        }

        //社保参保项目列表
        List<InsuranceSchemeDto.HrmInsuranceProjectBO> projectBOList = schemeDto.getSocialSecurityProjectList();
        //公积金参保项目
        if (CollUtil.isNotEmpty(schemeDto.getProvidentFundProjectList())) {
            projectBOList.addAll(schemeDto.getProvidentFundProjectList());
        }
        List<HrmInsuranceProject> projectList = TransferUtil.transferList(projectBOList, HrmInsuranceProject.class);
        projectList.forEach(project ->
                project.setSchemeId(insuranceScheme.getSchemeId()));
        if (insuranceScheme.getSchemeId() != null) {
            projectList.forEach(project ->
                    project.setUpdateUserId(info.getUserId()));
            projectList.forEach(project ->
                    project.setUpdateTime(LocalDateTime.now()));
        }else {
            projectList.forEach(project ->
                    project.setCreateUserId(info.getUserId()));
            projectList.forEach(project ->
                    project.setCreateTime(LocalDateTime.now()));
        }
        insuranceProjectService.saveOrUpdateBatch(projectList);

//        HrmInsuranceScheme insuranceScheme = BeanUtil.copyProperties(schemeDto, HrmInsuranceScheme.class);
//
//        OperationLog operationLog = new OperationLog();
//        if (insuranceScheme.getSchemeId() != null) {
//            Long schemeId = insuranceScheme.getSchemeId();
//            lambdaUpdate().set(HrmInsuranceScheme::getIsDel, IsEnum.YES.getValue()).eq(HrmInsuranceScheme::getSchemeId, schemeId).update();
//            insuranceProjectService.lambdaUpdate().set(HrmInsuranceProject::getIsDel, IsEnum.YES.getValue()).eq(HrmInsuranceProject::getSchemeId, schemeId).update();
//            insuranceScheme.setSchemeId(null);
//            save(insuranceScheme);
//            //把使用该社保方案的员工更新新的社保方案
//            employeeSocialSecurityService.lambdaUpdate().set(com.tianye.hrsystem.entity.po.HrmEmployeeSocialSecurityInfo::getSchemeId, insuranceScheme.getSchemeId()).eq(HrmEmployeeSocialSecurityInfo::getSchemeId, schemeId).update();
//            insuranceMonthEmpRecordService.lambdaUpdate().set(HrmInsuranceMonthEmpRecord::getSchemeId, insuranceScheme.getSchemeId()).eq(HrmInsuranceMonthEmpRecord::getSchemeId, schemeId).update();
//
//            operationLog.setOperationObject(insuranceScheme.getSchemeName());
//            operationLog.setOperationInfo("编辑社保方案：" + insuranceScheme.getSchemeName());
//            operationLog.setBehavior(BehaviorEnum.UPDATE);
//        } else {
//            save(insuranceScheme);
//            operationLog.setOperationObject(insuranceScheme.getSchemeName());
//            operationLog.setOperationInfo("新建社保方案：" + insuranceScheme.getSchemeName());
//            operationLog.setBehavior(BehaviorEnum.SAVE);
//        }
//        List<InsuranceSchemeDto.HrmInsuranceProjectBO> projectBOList = schemeDto.getSocialSecurityProjectList();
//        if (CollUtil.isNotEmpty(schemeDto.getProvidentFundProjectList())) {
//            projectBOList.addAll(schemeDto.getProvidentFundProjectList());
//        }
//        List<HrmInsuranceProject> projectList = TransferUtil.transferList(projectBOList, HrmInsuranceProject.class);
//        projectList.forEach(project ->
//                project.setSchemeId(insuranceScheme.getSchemeId()).setProjectId(null));
//        insuranceProjectService.saveBatch(projectList);
//        return operationLog;
    }
}
