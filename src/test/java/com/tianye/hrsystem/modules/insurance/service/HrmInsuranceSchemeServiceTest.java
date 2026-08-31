package com.tianye.hrsystem.modules.insurance.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.AddInsuranceSchemeBO;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.po.HrmEmployeeSocialSecurityInfo;
import com.tianye.hrsystem.entity.vo.InsuranceSchemeVO;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.insurance.dto.InsuranceSchemeDto;
import com.tianye.hrsystem.modules.insurance.dto.UpdateInsuranceProjectBO;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpProjectRecord;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceProject;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceScheme;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceSechemeMapper;
import com.tianye.hrsystem.modules.insurance.vo.EmpInsuranceByIdVO;
import com.tianye.hrsystem.modules.insurance.vo.InsuranceSchemeListVO;
import org.mockito.ArgumentCaptor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HrmInsuranceSchemeServiceTest {

    @Test
    public void indexRaisesGroupConcatLimitBeforeQueryingUsageEmployeeNames() {
        HrmInsuranceSechemeMapper mapper = mock(HrmInsuranceSechemeMapper.class);
        when(mapper.index(any(BasePage.class))).thenReturn(new BasePage<InsuranceSchemeListVO>());
        HrmInsuranceSchemeService service = new HrmInsuranceSchemeService();
        ReflectionTestUtils.setField(service, "insuranceSechemeMapper", mapper);

        service.index(new PageEntity());

        InOrder inOrder = inOrder(mapper);
        inOrder.verify(mapper).setGroupConcatMaxLen();
        inOrder.verify(mapper).index(any(BasePage.class));
    }

    @Test
    public void queryInsuranceSchemeById_shouldBackfillMedicalLongTermCareAfterMaternityInsurance() {
        HrmInsuranceSchemeService service = spy(new HrmInsuranceSchemeService());
        HrmInsuranceScheme scheme = new HrmInsuranceScheme();
        scheme.setSchemeId(1001L);
        scheme.setSchemeName("默认社保方案");
        scheme.setCity("460100");
        doReturn(scheme).when(service).getById(1001L);

        HrmInsuranceProjectService projectService = mock(HrmInsuranceProjectService.class);
        LambdaQueryChainWrapper<HrmInsuranceProject> query = mock(LambdaQueryChainWrapper.class);
        when(projectService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), eq(1001L))).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(
                project(1, 11L),
                project(2, 12L),
                project(3, 13L),
                project(4, 14L),
                project(5, 15L),
                project(10, 20L)
        ));
        ReflectionTestUtils.setField(service, "insuranceProjectService", projectService);

        InsuranceSchemeVO result = service.queryInsuranceSchemeById(1001L);

        List<Integer> socialTypes = result.getSocialSecurityProjectList().stream()
                .map(AddInsuranceSchemeBO.HrmInsuranceProjectBO::getType)
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 12), socialTypes);

        AddInsuranceSchemeBO.HrmInsuranceProjectBO longTermCare = result.getSocialSecurityProjectList().get(5);
        Assert.assertNull("旧方案自动补出的医疗长期护理保险不应伪造项目 ID", longTermCare.getProjectId());
        Assert.assertEquals("医疗长期护理保险", longTermCare.getProjectName());
        Assert.assertEquals(BigDecimal.ZERO, longTermCare.getDefaultAmount());
        Assert.assertEquals(BigDecimal.ZERO, longTermCare.getCorporateProportion());
        Assert.assertEquals(BigDecimal.ZERO, longTermCare.getPersonalProportion());
        Assert.assertEquals(BigDecimal.ZERO, longTermCare.getCorporateAmount());
        Assert.assertEquals(BigDecimal.ZERO, longTermCare.getPersonalAmount());
        Assert.assertEquals(Integer.valueOf(1), longTermCare.getIsEnabled());

        List<Integer> providentTypes = result.getProvidentFundProjectList().stream()
                .map(AddInsuranceSchemeBO.HrmInsuranceProjectBO::getType)
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(10), providentTypes);
    }

    @Test
    public void queryInsuranceSchemeById_shouldReturnSavedMedicalLongTermCareEnabledState() {
        HrmInsuranceSchemeService service = spy(new HrmInsuranceSchemeService());
        HrmInsuranceScheme scheme = new HrmInsuranceScheme();
        scheme.setSchemeId(1002L);
        scheme.setSchemeName("可停用长期护理方案");
        doReturn(scheme).when(service).getById(1002L);

        HrmInsuranceProjectService projectService = mock(HrmInsuranceProjectService.class);
        LambdaQueryChainWrapper<HrmInsuranceProject> query = mock(LambdaQueryChainWrapper.class);
        when(projectService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), eq(1002L))).thenReturn(query);
        when(query.list()).thenReturn(Arrays.asList(
                project(5, 15L, 1),
                project(12, 21L, 0)
        ));
        ReflectionTestUtils.setField(service, "insuranceProjectService", projectService);

        InsuranceSchemeVO result = service.queryInsuranceSchemeById(1002L);

        AddInsuranceSchemeBO.HrmInsuranceProjectBO longTermCare = result.getSocialSecurityProjectList().get(1);
        Assert.assertEquals(Integer.valueOf(12), longTermCare.getType());
        Assert.assertEquals(Integer.valueOf(0), longTermCare.getIsEnabled());
    }

    @Test
    public void insuranceProjectModels_shouldExposeEnabledStateForSaveDetailAndMonthRecord() {
        InsuranceSchemeDto.HrmInsuranceProjectBO saveBo = new InsuranceSchemeDto.HrmInsuranceProjectBO();
        saveBo.setIsEnabled(0);
        Assert.assertEquals(Integer.valueOf(0), saveBo.getIsEnabled());

        HrmInsuranceProject schemeProject = new HrmInsuranceProject();
        schemeProject.setIsEnabled(0);
        Assert.assertEquals(Integer.valueOf(0), schemeProject.getIsEnabled());

        HrmInsuranceMonthEmpProjectRecord monthProject = new HrmInsuranceMonthEmpProjectRecord();
        monthProject.setIsEnabled(0);
        Assert.assertEquals(Integer.valueOf(0), monthProject.getIsEnabled());

        EmpInsuranceByIdVO.HrmInsuranceProjectBO monthProjectVO = new EmpInsuranceByIdVO.HrmInsuranceProjectBO();
        monthProjectVO.setIsEnabled(0);
        Assert.assertEquals(Integer.valueOf(0), monthProjectVO.getIsEnabled());
    }

    @Test
    public void saveInsuranceProject_shouldPersistMedicalLongTermCareEnabledState() {
        HrmInsuranceSchemeService service = spy(new HrmInsuranceSchemeService());
        HrmInsuranceProjectService projectService = mock(HrmInsuranceProjectService.class);
        ReflectionTestUtils.setField(service, "insuranceProjectService", projectService);
        doReturn(true).when(service).update(any(), any());
        when(projectService.saveOrUpdateBatch(any())).thenReturn(true);

        LoginUserInfo info = new LoginUserInfo();
        info.setUserId("9001");
        CompanyContext.set(info);
        try {
            InsuranceSchemeDto dto = new InsuranceSchemeDto();
            dto.setSchemeId(6001L);
            dto.setSchemeName("可禁用长期护理方案");
            dto.setCity(460100L);
            dto.setSocialSecurityProjectList(new ArrayList<>(Arrays.asList(
                    saveProject(5, 7001L, null),
                    saveProject(12, 7002L, 0)
            )));
            dto.setProvidentFundProjectList(new ArrayList<>(Arrays.asList(
                    saveProject(10, 7003L, 1)
            )));

            service.saveInsuranceProject(dto);

            ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
            verify(projectService).saveOrUpdateBatch(saveCaptor.capture());
            List<HrmInsuranceProject> savedProjects = saveCaptor.getValue();
            Assert.assertEquals(3, savedProjects.size());

            HrmInsuranceProject maternity = savedProjectByType(savedProjects, 5);
            HrmInsuranceProject longTermCare = savedProjectByType(savedProjects, 12);
            HrmInsuranceProject providentFund = savedProjectByType(savedProjects, 10);
            Assert.assertEquals("旧项目缺少启用状态时保存前应按启用处理", Integer.valueOf(1), maternity.getIsEnabled());
            Assert.assertEquals("医疗长期护理保险禁用状态必须随保存入库", Integer.valueOf(0), longTermCare.getIsEnabled());
            Assert.assertEquals(Integer.valueOf(1), providentFund.getIsEnabled());
            savedProjects.forEach(project -> Assert.assertEquals(Long.valueOf(6001L), project.getSchemeId()));
        } finally {
            CompanyContext.clear();
        }
    }

    @Test
    public void monthEmployeeRecordDetail_shouldExposeSavedProjectEnabledState() {
        HrmInsuranceMonthEmpRecordService service = spy(new HrmInsuranceMonthEmpRecordService());
        HrmInsuranceMonthEmpRecord monthEmpRecord = new HrmInsuranceMonthEmpRecord();
        monthEmpRecord.setIEmpRecordId(3001L);
        monthEmpRecord.setEmployeeId(4001L);
        monthEmpRecord.setSchemeId(5001L);
        doReturn(monthEmpRecord).when(service).getById("3001");

        HrmInsuranceSchemeService schemeService = mock(HrmInsuranceSchemeService.class);
        HrmInsuranceScheme scheme = new HrmInsuranceScheme();
        scheme.setSchemeId(5001L);
        scheme.setSchemeName("月度方案");
        scheme.setCity("460100");
        when(schemeService.getById(5001L)).thenReturn(scheme);
        ReflectionTestUtils.setField(service, "schemeService", schemeService);

        com.tianye.hrsystem.service.employee.IHrmEmployeeService employeeService =
                mock(com.tianye.hrsystem.service.employee.IHrmEmployeeService.class);
        HrmEmployee employee = new HrmEmployee();
        employee.setIdNumber("420000199001010000");
        when(employeeService.getById(4001L)).thenReturn(employee);
        ReflectionTestUtils.setField(service, "employeeService", employeeService);

        HrmEmployeeSocialSecurityService socialSecurityService = mock(HrmEmployeeSocialSecurityService.class);
        LambdaQueryChainWrapper<HrmEmployeeSocialSecurityInfo> socialSecurityQuery = mock(LambdaQueryChainWrapper.class);
        when(socialSecurityService.lambdaQuery()).thenReturn(socialSecurityQuery);
        when(socialSecurityQuery.eq(any(), eq(4001L))).thenReturn(socialSecurityQuery);
        when(socialSecurityQuery.oneOpt()).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(service, "socialSecurityService", socialSecurityService);

        HrmInsuranceMonthEmpProjectRecordService empProjectRecordService = mock(HrmInsuranceMonthEmpProjectRecordService.class);
        LambdaQueryChainWrapper<HrmInsuranceMonthEmpProjectRecord> projectQuery = mock(LambdaQueryChainWrapper.class);
        when(empProjectRecordService.lambdaQuery()).thenReturn(projectQuery);
        when(projectQuery.eq(any(), eq("3001"))).thenReturn(projectQuery);
        when(projectQuery.list()).thenReturn(Arrays.asList(
                monthProject(1, 31L, null),
                monthProject(12, 32L, 0),
                monthProject(10, 33L, 1)
        ));
        ReflectionTestUtils.setField(service, "empProjectRecordService", empProjectRecordService);

        EmpInsuranceByIdVO result = service.queryById("3001");

        Assert.assertEquals(Integer.valueOf(1), result.getSocialSecurityList().get(0).getIsEnabled());
        Assert.assertEquals(Integer.valueOf(12), result.getSocialSecurityList().get(1).getType());
        Assert.assertEquals(Integer.valueOf(0), result.getSocialSecurityList().get(1).getIsEnabled());
        Assert.assertEquals(Integer.valueOf(1), result.getProvidentFundList().get(0).getIsEnabled());
    }

    @Test
    public void updateInsuranceProject_shouldPersistBackfilledMedicalLongTermCareProjectWhenProjectIdMissing() {
        HrmInsuranceMonthEmpRecordService service = spy(new HrmInsuranceMonthEmpRecordService());

        HrmInsuranceMonthEmpProjectRecordService empProjectRecordService = mock(HrmInsuranceMonthEmpProjectRecordService.class);
        LambdaUpdateChainWrapper<HrmInsuranceMonthEmpProjectRecord> updateWrapper = mock(LambdaUpdateChainWrapper.class);
        when(empProjectRecordService.lambdaUpdate()).thenReturn(updateWrapper);
        when(updateWrapper.eq(any(), eq(3002L))).thenReturn(updateWrapper);
        when(updateWrapper.remove()).thenReturn(true);
        when(empProjectRecordService.saveBatch(any())).thenReturn(true);
        Map<String, Object> projectCount = new HashMap<>();
        projectCount.put("personalInsuranceAmount", BigDecimal.ONE);
        projectCount.put("corporateInsuranceAmount", BigDecimal.ONE);
        when(empProjectRecordService.queryProjectCount(3002L)).thenReturn(projectCount);
        ReflectionTestUtils.setField(service, "empProjectRecordService", empProjectRecordService);

        HrmInsuranceProjectService projectService = mock(HrmInsuranceProjectService.class);
        HrmInsuranceProject maternityProject = project(5, 7101L, 1);
        maternityProject.setSchemeId(5002L);
        when(projectService.getById(7101L)).thenReturn(maternityProject);
        when(projectService.save(any(HrmInsuranceProject.class))).thenAnswer(invocation -> {
            HrmInsuranceProject savedProject = invocation.getArgument(0);
            savedProject.setProjectId(7201L);
            return true;
        });
        ReflectionTestUtils.setField(service, "projectService", projectService);

        HrmInsuranceSchemeService schemeService = mock(HrmInsuranceSchemeService.class);
        HrmInsuranceScheme scheme = new HrmInsuranceScheme();
        scheme.setSchemeId(5002L);
        scheme.setSchemeName("旧方案补长期护理");
        when(schemeService.getById(5002L)).thenReturn(scheme);
        ReflectionTestUtils.setField(service, "schemeService", schemeService);

        HrmInsuranceMonthEmpRecord monthEmpRecord = new HrmInsuranceMonthEmpRecord();
        monthEmpRecord.setIEmpRecordId(3002L);
        monthEmpRecord.setEmployeeId(4002L);
        monthEmpRecord.setSchemeId(5002L);
        doReturn(monthEmpRecord).when(service).getById(3002L);
        doReturn(true).when(service).updateById(any(HrmInsuranceMonthEmpRecord.class));

        com.tianye.hrsystem.service.employee.IHrmEmployeeService employeeService =
                mock(com.tianye.hrsystem.service.employee.IHrmEmployeeService.class);
        HrmEmployee employee = new HrmEmployee();
        employee.setEmployeeName("张三");
        when(employeeService.getById(4002L)).thenReturn(employee);
        ReflectionTestUtils.setField(service, "employeeService", employeeService);

        HrmEmployeeSocialSecurityService socialSecurityService = mock(HrmEmployeeSocialSecurityService.class);
        LambdaQueryChainWrapper<HrmEmployeeSocialSecurityInfo> socialSecurityQuery = mock(LambdaQueryChainWrapper.class);
        when(socialSecurityService.lambdaQuery()).thenReturn(socialSecurityQuery);
        when(socialSecurityQuery.eq(any(), eq(4002L))).thenReturn(socialSecurityQuery);
        when(socialSecurityQuery.oneOpt()).thenReturn(Optional.empty());
        when(socialSecurityService.save(any(HrmEmployeeSocialSecurityInfo.class))).thenReturn(true);
        ReflectionTestUtils.setField(service, "socialSecurityService", socialSecurityService);

        UpdateInsuranceProjectBO bo = new UpdateInsuranceProjectBO();
        bo.setIEmpRecordId(3002L);
        bo.setSchemeId(5002L);
        bo.setProjectList(Arrays.asList(
                updateProject(7101L, 5, "", 1),
                updateProject(null, 12, "医疗长期护理保险", 0)
        ));

        service.updateInsuranceProject(bo);

        ArgumentCaptor<HrmInsuranceProject> schemeProjectCaptor = ArgumentCaptor.forClass(HrmInsuranceProject.class);
        verify(projectService).save(schemeProjectCaptor.capture());
        HrmInsuranceProject savedSchemeProject = schemeProjectCaptor.getValue();
        Assert.assertEquals(Long.valueOf(5002L), savedSchemeProject.getSchemeId());
        Assert.assertEquals(Integer.valueOf(12), savedSchemeProject.getType());
        Assert.assertEquals("医疗长期护理保险", savedSchemeProject.getProjectName());
        Assert.assertEquals(Integer.valueOf(0), savedSchemeProject.getIsEnabled());

        ArgumentCaptor<List> monthProjectCaptor = ArgumentCaptor.forClass(List.class);
        verify(empProjectRecordService).saveBatch(monthProjectCaptor.capture());
        List<HrmInsuranceMonthEmpProjectRecord> savedMonthProjects = monthProjectCaptor.getValue();
        HrmInsuranceMonthEmpProjectRecord savedLongTermCare = monthSavedProjectByType(savedMonthProjects, 12);
        Assert.assertEquals(Long.valueOf(3002L), savedLongTermCare.getIEmpRecordId());
        Assert.assertEquals(Long.valueOf(7201L), savedLongTermCare.getProjectId());
        Assert.assertEquals(Integer.valueOf(0), savedLongTermCare.getIsEnabled());
    }

    private HrmInsuranceProject project(Integer type, Long projectId) {
        return project(type, projectId, null);
    }

    private HrmInsuranceProject project(Integer type, Long projectId, Integer isEnabled) {
        HrmInsuranceProject project = new HrmInsuranceProject();
        project.setProjectId(projectId);
        project.setType(type);
        project.setIsEnabled(isEnabled);
        project.setDefaultAmount(1000D);
        project.setCorporateProportion(1D);
        project.setPersonalProportion(1D);
        project.setCorporateAmount(10D);
        project.setPersonalAmount(10D);
        return project;
    }

    private HrmInsuranceMonthEmpProjectRecord monthProject(Integer type, Long projectId, Integer isEnabled) {
        HrmInsuranceMonthEmpProjectRecord project = new HrmInsuranceMonthEmpProjectRecord();
        project.setProjectId(projectId);
        project.setType(type);
        project.setIsEnabled(isEnabled);
        project.setDefaultAmount(BigDecimal.TEN);
        project.setCorporateProportion(BigDecimal.ONE);
        project.setPersonalProportion(BigDecimal.ONE);
        project.setCorporateAmount(BigDecimal.ONE);
        project.setPersonalAmount(BigDecimal.ONE);
        return project;
    }

    private InsuranceSchemeDto.HrmInsuranceProjectBO saveProject(Integer type, Long projectId, Integer isEnabled) {
        InsuranceSchemeDto.HrmInsuranceProjectBO project = new InsuranceSchemeDto.HrmInsuranceProjectBO();
        project.setProjectId(projectId);
        project.setType(type);
        project.setIsEnabled(isEnabled);
        project.setProjectName(type == 12 ? "医疗长期护理保险" : "");
        project.setDefaultAmount(BigDecimal.TEN);
        project.setCorporateProportion(BigDecimal.ONE);
        project.setPersonalProportion(BigDecimal.ONE);
        project.setCorporateAmount(BigDecimal.ONE);
        project.setPersonalAmount(BigDecimal.ONE);
        return project;
    }

    private HrmInsuranceProject savedProjectByType(List<HrmInsuranceProject> projects, Integer type) {
        return projects.stream()
                .filter(project -> type.equals(project.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未保存 type=" + type + " 的参保项目"));
    }

    private UpdateInsuranceProjectBO.Project updateProject(Long projectId, Integer type, String projectName, Integer isEnabled) {
        UpdateInsuranceProjectBO.Project project = new UpdateInsuranceProjectBO.Project();
        project.setProjectId(projectId);
        project.setType(type);
        project.setProjectName(projectName);
        project.setIsEnabled(isEnabled);
        project.setDefaultAmount(BigDecimal.TEN);
        project.setCorporateProportion(BigDecimal.ONE);
        project.setPersonalProportion(BigDecimal.ONE);
        project.setCorporateAmount(BigDecimal.ONE);
        project.setPersonalAmount(BigDecimal.ONE);
        return project;
    }

    private HrmInsuranceMonthEmpProjectRecord monthSavedProjectByType(List<HrmInsuranceMonthEmpProjectRecord> projects, Integer type) {
        return projects.stream()
                .filter(project -> type.equals(project.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未保存月度 type=" + type + " 的参保项目"));
    }
}
