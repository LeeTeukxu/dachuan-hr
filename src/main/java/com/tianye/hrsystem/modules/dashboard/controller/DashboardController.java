package com.tianye.hrsystem.modules.dashboard.controller;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.modules.dashboard.bo.DashboardQueryBO;
import com.tianye.hrsystem.modules.dashboard.bo.KeyPostSaveBO;
import com.tianye.hrsystem.modules.dashboard.bo.PerfIndicatorBO;
import com.tianye.hrsystem.modules.dashboard.service.DashboardPermissionSupport;
import com.tianye.hrsystem.modules.dashboard.service.DashboardService;
import com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 人力资源数据看板
 */
@Api(tags = "数据看板")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Map<Integer, String> PERF_KPI_BY_TYPE = new HashMap<>();
    private static final Map<String, String> PERSONNEL_KPI_FIELDS = new HashMap<>();
    private static final Map<String, String> FLOW_KPI_FIELDS = new HashMap<>();
    private static final Map<String, String> SALARY_KPI_FIELDS = new HashMap<>();

    static {
        PERF_KPI_BY_TYPE.put(1, "安全指标达成率");
        PERF_KPI_BY_TYPE.put(2, "质量指标达成率");
        PERF_KPI_BY_TYPE.put(3, "营收指标完成率");
        PERF_KPI_BY_TYPE.put(4, "成本控制率");
        PERF_KPI_BY_TYPE.put(5, "产能指标达成率");

        PERSONNEL_KPI_FIELDS.put("期末在职人数", "activeCount");
        PERSONNEL_KPI_FIELDS.put("本期内新入职", "hiredCount");
        PERSONNEL_KPI_FIELDS.put("本期内离职", "quitCount");
        PERSONNEL_KPI_FIELDS.put("平均年龄", "avgAge");
        PERSONNEL_KPI_FIELDS.put("平均司龄", "avgTenure");

        FLOW_KPI_FIELDS.put("本期内新入职", "hiredCount");
        FLOW_KPI_FIELDS.put("本期内离职", "quitCount");
        FLOW_KPI_FIELDS.put("主动 / 被动离职占比", "typeDist");
        FLOW_KPI_FIELDS.put("关键岗位流失", "keyLostCount");

        SALARY_KPI_FIELDS.put("应发工资合计", "expectedPay");
        SALARY_KPI_FIELDS.put("实发工资合计", "realPay");
        SALARY_KPI_FIELDS.put("社保公积金（企业）", "corpSocialFund");
        SALARY_KPI_FIELDS.put("奖金", "bonus");
        SALARY_KPI_FIELDS.put("代扣个税合计", "personalTax");
    }

    private static final String[] PERSONNEL_KPIS = {"期末在职人数", "本期内新入职", "本期内离职", "净增员", "平均年龄", "平均司龄"};
    private static final String[] FLOW_KPIS = {"本期内新入职", "本期内离职", "净增员", "离职率", "主动 / 被动离职占比", "关键岗位流失"};
    private static final String[] SALARY_KPIS = {"应发工资合计", "实发工资合计", "社保公积金（企业）", "奖金", "人均应发", "代扣个税合计"};
    private static final String[] PERF_KPIS = {"综合达成率", "安全指标达成率", "质量指标达成率", "营收指标完成率", "成本控制率", "产能指标达成率"};

    @Resource
    private DashboardService dashboardService;

    @Resource
    private DashboardPermissionSupport permissionSupport;

    /* ==================== 人员信息看板 ==================== */

    @PostMapping("/personnel/overview")
    @ApiOperation("人员信息-指标总览")
    public Result<Map<String, Object>> personnelOverview(@RequestBody DashboardQueryBO bo) {
        guardBoardKpis("personnel", PERSONNEL_KPIS);
        Map<String, Object> result = dashboardService.personnelOverview(bo);
        stripHiddenKpis(result, "personnel", PERSONNEL_KPI_FIELDS);
        return Result.ok(result);
    }

    @PostMapping("/personnel/structure")
    @ApiOperation("人员信息-结构分布（性别/学历/年龄段/司龄段/部门/聘用形式）")
    public Result<List<Map<String, Object>>> structure(@RequestBody DashboardQueryBO bo) {
        String dim = bo.getDim() == null ? "dept" : bo.getDim();
        if ("dept".equals(dim)) {
            guardAny("personnel:chart:dept", "personnel:table:deptSummary");
        } else {
            guardElement("sex".equals(dim) ? "personnel:chart:gender"
                    : "edu".equals(dim) ? "personnel:chart:edu"
                    : "age".equals(dim) ? "personnel:chart:age"
                    : "tenure".equals(dim) ? "personnel:chart:tenure"
                    : "personnel:chart:dept");
        }
        return Result.ok(dashboardService.structure(bo));
    }

    @PostMapping("/personnel/cross")
    @ApiOperation("人员信息-交叉对比矩阵（维度×堆叠维度）")
    public Result<Map<String, Object>> crossMatrix(@RequestBody DashboardQueryBO bo) {
        guardAny("personnel:chart:cross1", "personnel:chart:cross2");
        return Result.ok(dashboardService.crossMatrix(bo));
    }

    @PostMapping("/personnel/pageList")
    @ApiOperation("人员信息-员工明细分页")
    public Result<BasePage<Map<String, Object>>> personnelPageList(@RequestBody DashboardQueryBO bo) {
        guardElement("personnel:table:detail");
        return Result.ok(dashboardService.personnelPageList(bo));
    }

    /* ==================== 薪酬成本看板 ==================== */

    @PostMapping("/salary/overview")
    @ApiOperation("薪酬成本-指标总览（含同比上期）")
    public Result<Map<String, Object>> salaryOverview(@RequestBody DashboardQueryBO bo) {
        guardBoardKpis("salary", SALARY_KPIS);
        Map<String, Object> result = dashboardService.salaryOverview(bo);
        stripHiddenKpis(result, "salary", SALARY_KPI_FIELDS);
        return Result.ok(result);
    }

    @PostMapping("/salary/trend")
    @ApiOperation("薪酬成本-成本趋势")
    public Result<List<Map<String, Object>>> salaryTrend(@RequestBody DashboardQueryBO bo) {
        guardAny("salary:chart:trend", "salary:chart:yoy", "salary:table:sum");
        return Result.ok(dashboardService.salaryTrend(bo));
    }

    @PostMapping("/salary/insuranceTrend")
    @ApiOperation("薪酬成本-社保公积金趋势（按周期合并到成本趋势）")
    public Result<List<Map<String, Object>>> salaryInsuranceTrend(@RequestBody DashboardQueryBO bo) {
        guardAny("salary:chart:trend", "salary:chart:yoy", "salary:table:sum");
        return Result.ok(dashboardService.salaryInsuranceTrend(bo));
    }

    @PostMapping("/salary/deptCompare")
    @ApiOperation("薪酬成本-部门横向对比")
    public Result<List<Map<String, Object>>> salaryDeptCompare(@RequestBody DashboardQueryBO bo) {
        guardElement("salary:chart:dept");
        return Result.ok(dashboardService.salaryDeptCompare(bo));
    }

    @PostMapping("/salary/empDetail")
    @ApiOperation("薪酬成本-员工薪酬明细分页")
    public Result<BasePage<Map<String, Object>>> salaryEmpDetail(@RequestBody DashboardQueryBO bo) {
        guardElement("salary:table:detail");
        return Result.ok(dashboardService.salaryEmpDetail(bo));
    }

    /* ==================== 入/离职流动分析看板 ==================== */

    @PostMapping("/flow/overview")
    @ApiOperation("入离职-指标总览")
    public Result<Map<String, Object>> flowOverview(@RequestBody DashboardQueryBO bo) {
        guardBoardKpis("flow", FLOW_KPIS);
        Map<String, Object> result = dashboardService.flowOverview(bo);
        stripHiddenKpis(result, "flow", FLOW_KPI_FIELDS);
        return Result.ok(result);
    }

    @PostMapping("/flow/trend")
    @ApiOperation("入离职-趋势（含去年同期与关键岗位按部门堆叠）")
    public Result<Map<String, Object>> flowTrend(@RequestBody DashboardQueryBO bo) {
        guardAny("flow:chart:trend", "flow:chart:keyTrend", "flow:chart:keyDept", "personnel:chart:trend");
        return Result.ok(dashboardService.flowTrend(bo));
    }

    @PostMapping("/flow/deptCompare")
    @ApiOperation("入离职-各部门入职vs离职")
    public Result<List<Map<String, Object>>> flowDeptCompare(@RequestBody DashboardQueryBO bo) {
        guardAny("flow:chart:deptFlow", "personnel:table:deptSummary");
        return Result.ok(dashboardService.flowDeptCompare(bo));
    }

    @PostMapping("/flow/dist")
    @ApiOperation("入离职-分布（type类型/reason原因/tenure司龄/age年龄/edu学历/dept部门）")
    public Result<List<Map<String, Object>>> quitDist(@RequestBody DashboardQueryBO bo) {
        String dim = bo.getDim() == null ? "type" : bo.getDim();
        guardElement("reason".equals(dim) ? "flow:chart:reason"
                : "tenure".equals(dim) ? "flow:chart:tenure"
                : "age".equals(dim) ? "flow:chart:age"
                : "edu".equals(dim) ? "flow:chart:eduPie"
                : "flow:chart:typePie");
        return Result.ok(dashboardService.quitDist(bo));
    }

    @PostMapping("/flow/pageList")
    @ApiOperation("入离职-流动明细分页")
    public Result<BasePage<Map<String, Object>>> flowPageList(@RequestBody DashboardQueryBO bo) {
        guardElement("flow:table:detail");
        return Result.ok(dashboardService.flowPageList(bo));
    }

    /* ==================== 绩效考核指标看板 ==================== */

    @PostMapping("/perf/trend")
    @ApiOperation("绩效-指标趋势（目标vs实际）")
    public Result<Map<String, Object>> perfTrend(@RequestBody DashboardQueryBO bo) {
        guardElement("perf:chart:trend");
        return Result.ok(dashboardService.perfTrend(bo));
    }

    @PostMapping("/perf/completion")
    @ApiOperation("绩效-各指标完成率")
    public Result<List<Map<String, Object>>> perfCompletion(@RequestBody DashboardQueryBO bo) {
        guardPerfElements();
        List<Map<String, Object>> rows = dashboardService.perfCompletion(bo);
        stripPerfRows(rows);
        return Result.ok(rows);
    }

    @PostMapping("/perf/pageList")
    @ApiOperation("绩效-指标明细列表")
    public Result<List<Map<String, Object>>> perfPageList(@RequestBody DashboardQueryBO bo) {
        guardElement("perf:table:list");
        return Result.ok(dashboardService.perfPageList(bo));
    }

    @PostMapping("/perf/save")
    @ApiOperation("绩效-指标批量保存（按部门年月类型upsert）")
    public Result<Void> perfSave(@RequestBody List<PerfIndicatorBO> rows) {
        guardElement("perf:table:list");
        dashboardService.perfSave(rows);
        return Result.ok();
    }

    @PostMapping("/perf/delete")
    @ApiOperation("绩效-指标删除")
    public Result<Void> perfDelete(@RequestBody List<Long> ids) {
        guardElement("perf:table:list");
        dashboardService.perfDelete(ids);
        return Result.ok();
    }

    /* ==================== 关键岗位设置 ==================== */

    @PostMapping("/keyPost/list")
    @ApiOperation("关键岗位-岗位清单及标记状态")
    public Result<List<Map<String, Object>>> keyPostList() {
        guardAny("flow:chart:keyTrend", "flow:chart:keyDept");
        return Result.ok(dashboardService.keyPostList());
    }

    @PostMapping("/keyPost/save")
    @ApiOperation("关键岗位-全量保存标记")
    public Result<Void> keyPostSave(@RequestBody KeyPostSaveBO bo) {
        guardAny("flow:chart:keyTrend", "flow:chart:keyDept");
        dashboardService.keyPostSave(bo);
        return Result.ok();
    }

    /* ==================== 看板角色权限 ==================== */

    @PostMapping("/permission/all")
    @ApiOperation("看板权限-角色权限清单（管理端，按菜单授权）")
    public Result<List<Map<String, Object>>> dashboardPermissionAll() {
        return Result.ok(dashboardService.dashboardPermissionList());
    }

    @PostMapping("/permission/save")
    @ApiOperation("看板权限-保存角色配置（管理端，按菜单授权）")
    public Result<Void> dashboardPermissionSave(@RequestBody Map<String, String> params) {
        dashboardService.saveDashboardPermission(params.get("roleId"), params.get("configJson"));
        return Result.ok();
    }

    @PostMapping("/permission/effective")
    @ApiOperation("看板权限-当前用户所属角色的生效配置")
    public Result<String> dashboardPermissionEffective() {
        return Result.ok(dashboardService.effectiveDashboardPermission());
    }

    /* ==================== 用户看板显示偏好 ==================== */

    @PostMapping("/userConfig/get")
    @ApiOperation("看板偏好-读取当前用户配置")
    public Result<String> userConfigGet(@RequestBody Map<String, String> params) {
        return Result.ok(dashboardService.userConfigGet(params.get("boardKey")));
    }

    @PostMapping("/userConfig/save")
    @ApiOperation("看板偏好-保存当前用户配置")
    public Result<Void> userConfigSave(@RequestBody Map<String, String> params) {
        dashboardService.userConfigSave(params.get("boardKey"), params.get("configJson"));
        return Result.ok();
    }

    /* ==================== 集团/分公司切换 ==================== */

    @PostMapping("/company/checkRole")
    @ApiOperation("检查当前用户是否为行政经理")
    public Result<Boolean> checkAdminRole() {
        return Result.ok(dashboardService.isAdminManager());
    }

    @PostMapping("/company/list")
    @ApiOperation("获取公司列表")
    public Result<List<QueryCompanyListVO>> companyList() {
        return Result.ok(dashboardService.companyList());
    }

    @PostMapping("/flow/groupOverview")
    @ApiOperation("集团入离职-汇总指标总览（含各公司明细）")
    public Result<Map<String, Object>> flowOverviewGroup(@RequestBody DashboardQueryBO bo) {
        guardBoardKpis("flow", FLOW_KPIS);
        return Result.ok(dashboardService.flowOverviewGroup(bo));
    }

    @PostMapping("/flow/groupTrend")
    @ApiOperation("集团入离职-趋势汇总")
    public Result<Map<String, Object>> flowTrendGroup(@RequestBody DashboardQueryBO bo) {
        guardAny("flow:chart:trend", "flow:chart:keyTrend", "flow:chart:keyDept", "personnel:chart:trend");
        return Result.ok(dashboardService.flowTrendGroup(bo));
    }

    @PostMapping("/flow/groupDeptCompare")
    @ApiOperation("集团入离职-各部门入职vs离职汇总")
    public Result<List<Map<String, Object>>> flowDeptCompareGroup(@RequestBody DashboardQueryBO bo) {
        guardAny("flow:chart:deptFlow", "personnel:table:deptSummary");
        return Result.ok(dashboardService.flowDeptCompareGroup(bo));
    }

    @PostMapping("/flow/groupDist")
    @ApiOperation("集团入离职-分布汇总")
    public Result<List<Map<String, Object>>> flowDistGroup(@RequestBody DashboardQueryBO bo) {
        String dim = bo.getDim() == null ? "type" : bo.getDim();
        guardElement("reason".equals(dim) ? "flow:chart:reason"
                : "tenure".equals(dim) ? "flow:chart:tenure"
                : "age".equals(dim) ? "flow:chart:age"
                : "edu".equals(dim) ? "flow:chart:eduPie"
                : "flow:chart:typePie");
        return Result.ok(dashboardService.flowDistGroup(bo));
    }

    @PostMapping("/flow/groupPageList")
    @ApiOperation("集团入离职-流动明细分页（含公司列）")
    public Result<BasePage<Map<String, Object>>> flowPageListGroup(@RequestBody DashboardQueryBO bo) {
        guardElement("flow:table:detail");
        return Result.ok(dashboardService.flowPageListGroup(bo));
    }

    /* ==================== 其他看板集团聚合 ==================== */

    @PostMapping("/personnel/groupOverview")
    @ApiOperation("集团人员信息-汇总指标总览（含各公司明细）")
    public Result<Map<String, Object>> personnelGroupOverview(@RequestBody DashboardQueryBO bo) {
        guardBoardKpis("personnel", PERSONNEL_KPIS);
        return Result.ok(dashboardService.personnelGroupOverview(bo));
    }

    @PostMapping("/salary/groupOverview")
    @ApiOperation("集团薪酬成本-汇总指标总览（含各公司明细）")
    public Result<Map<String, Object>> salaryGroupOverview(@RequestBody DashboardQueryBO bo) {
        guardBoardKpis("salary", SALARY_KPIS);
        return Result.ok(dashboardService.salaryGroupOverview(bo));
    }

    @PostMapping("/perf/groupCompletion")
    @ApiOperation("集团绩效-各指标完成率汇总")
    public Result<List<Map<String, Object>>> perfGroupCompletion(@RequestBody DashboardQueryBO bo) {
        guardPerfElements();
        return Result.ok(dashboardService.perfGroupCompletion(bo));
    }

    /* ==================== 权限守卫工具 ==================== */

    private void guardElement(String elKey) {
        permissionSupport.assertPermitted(elKey);
    }

    private void guardAny(String... elKeys) {
        permissionSupport.assertAnyPermitted(elKeys);
    }

    private void guardBoardKpis(String board, String[] kpis) {
        String[] els = new String[kpis.length];
        for (int i = 0; i < kpis.length; i++) {
            els[i] = board + ":kpi:" + kpis[i];
        }
        permissionSupport.assertAnyPermitted(els);
    }

    private void guardPerfElements() {
        String[] els = new String[PERF_KPIS.length + 2];
        for (int i = 0; i < PERF_KPIS.length; i++) {
            els[i] = "perf:kpi:" + PERF_KPIS[i];
        }
        els[PERF_KPIS.length] = "perf:chart:radar";
        els[PERF_KPIS.length + 1] = "perf:chart:comp";
        permissionSupport.assertAnyPermitted(els);
    }

    private void stripHiddenKpis(Map<String, Object> map, String board, Map<String, String> kpiToField) {
        if (map == null) {
            return;
        }
        Set<String> hidden = permissionSupport.hiddenKpis(board);
        if (hidden == null || hidden.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : kpiToField.entrySet()) {
            if (hidden.contains(entry.getKey())) {
                map.remove(entry.getValue());
            }
        }
    }

    private void stripPerfRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<String> hidden = permissionSupport.hiddenKpis("perf");
        if (hidden == null || hidden.isEmpty()) {
            return;
        }
        rows.removeIf(row -> {
            Object type = row.get("indicatorType");
            if (!(type instanceof Number)) {
                return false;
            }
            String kpi = PERF_KPI_BY_TYPE.getOrDefault(((Number) type).intValue(), "");
            return hidden.contains(kpi);
        });
    }
}