package com.tianye.hrsystem.modules.dashboard.service;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.modules.dashboard.bo.DashboardQueryBO;
import com.tianye.hrsystem.modules.dashboard.bo.KeyPostSaveBO;
import com.tianye.hrsystem.modules.dashboard.bo.PerfIndicatorBO;
import com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO;

import java.util.List;
import java.util.Map;

/**
 * 数据看板
 */
public interface DashboardService {

    Map<String, Object> personnelOverview(DashboardQueryBO bo);

    Map<String, Object> flowOverview(DashboardQueryBO bo);

    List<Map<String, Object>> structure(DashboardQueryBO bo);

    Map<String, Object> flowTrend(DashboardQueryBO bo);

    Map<String, Object> crossMatrix(DashboardQueryBO bo);

    BasePage<Map<String, Object>> personnelPageList(DashboardQueryBO bo);

    List<Map<String, Object>> flowDeptCompare(DashboardQueryBO bo);

    List<Map<String, Object>> quitDist(DashboardQueryBO bo);

    BasePage<Map<String, Object>> flowPageList(DashboardQueryBO bo);

    Map<String, Object> salaryOverview(DashboardQueryBO bo);

    List<Map<String, Object>> salaryTrend(DashboardQueryBO bo);

    List<Map<String, Object>> salaryInsuranceTrend(DashboardQueryBO bo);

    List<Map<String, Object>> salaryDeptCompare(DashboardQueryBO bo);

    BasePage<Map<String, Object>> salaryEmpDetail(DashboardQueryBO bo);

    Map<String, Object> perfTrend(DashboardQueryBO bo);

    List<Map<String, Object>> perfCompletion(DashboardQueryBO bo);

    List<Map<String, Object>> perfPageList(DashboardQueryBO bo);

    void perfSave(List<PerfIndicatorBO> rows);

    void perfDelete(List<Long> ids);

    List<Map<String, Object>> keyPostList();

    void keyPostSave(KeyPostSaveBO bo);

    String userConfigGet(String boardKey);

    void userConfigSave(String boardKey, String configJson);

    boolean isAdminManager();

    List<QueryCompanyListVO> companyList();

    Map<String, Object> flowOverviewGroup(DashboardQueryBO bo);

    Map<String, Object> flowTrendGroup(DashboardQueryBO bo);

    List<Map<String, Object>> flowDeptCompareGroup(DashboardQueryBO bo);

    List<Map<String, Object>> flowDistGroup(DashboardQueryBO bo);

    BasePage<Map<String, Object>> flowPageListGroup(DashboardQueryBO bo);

    Map<String, Object> personnelGroupOverview(DashboardQueryBO bo);

    Map<String, Object> salaryGroupOverview(DashboardQueryBO bo);

    List<Map<String, Object>> perfGroupCompletion(DashboardQueryBO bo);

    List<Map<String, Object>> dashboardPermissionList();

    void saveDashboardPermission(String roleId, String configJson);

    String effectiveDashboardPermission();
}
