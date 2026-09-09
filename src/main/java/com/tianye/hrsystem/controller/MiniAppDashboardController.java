package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.modules.dashboard.bo.DashboardQueryBO;
import com.tianye.hrsystem.modules.dashboard.service.DashboardService;
import com.tianye.hrsystem.modules.menu.service.ApiPermissionPathSupport;
import com.tianye.hrsystem.modules.miniapp.service.IMiniAppPermissionService;
import com.tianye.hrsystem.modules.miniapp.support.MiniAppVisibleCompanySupport;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * 小程序数据统计接口（/mp/dashboard/*）
 * 复用PC端DashboardService，为小程序提供简化的统计接口。
 *
 * <p>越权边界（2026-09-10，B方案「公司切换」可见公司）：
 * 员工 token（CompanyContext.employeeId 非空）下的所有取数只在「可见公司集合」内：
 * 可见集合 = {本登录公司} ∪ (系统表 mp_employee_visible_company 按 openid 授权的 company_id)；
 * 无授权 = 只自家公司。单公司接口收到越界 companyId 直接拒绝；集团聚合接口由服务端注入
 * scopeCompanyIds 白名单，只聚合可见公司。PC 操作员 token（含 account）不收紧，沿用菜单权限。</p>
 */
@RestController
@RequestMapping("/mp/dashboard")
public class MiniAppDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private MiniAppVisibleCompanySupport visibleCompanySupport;

    @Autowired
    private IMiniAppPermissionService permissionService;

    @Autowired
    private hrmDeptRepository deptRepository;

    /**
     * 当前请求是否员工 token（无 account），仅员工 token 需要收敛可见公司。
     */
    private boolean isEmployeeRequest() {
        LoginUserInfo info = CompanyContext.get();
        return info != null && info.getEmployeeId() != null
                && (info.getAccount() == null || info.getAccount().isEmpty());
    }

    /**
     * 员工请求下的可见公司集合（含本登录公司）。PC 操作员返回 null=不限制。
     */
    private Set<String> employeeVisible() {
        if (!isEmployeeRequest()) {
            return null;
        }
        return visibleCompanySupport.resolveVisibleCompanyIds();
    }

    /**
     * 单公司接口越权校验：员工请求下，若指定了非本司 companyId，则必须 ∈ 可见集合。
     * 校验通过后为集团聚合注入白名单。PC 操作员直接放行。
     */
    private void guardCompany(DashboardQueryBO bo) throws Exception {
        Set<String> visible = employeeVisible();
        if (visible == null) {
            return;
        }
        String target = bo == null ? null : bo.getCompanyId();
        String home = currentCompanyId();
        if (target != null && !target.isEmpty() && !visible.contains(target)) {
            throw new Exception("您未被授权查看该公司数据，请在公司选择中切换可见公司");
        }
        // 切到非本登录公司需「可切换公司」能力（can_view_statistics=1 且 can_switch_company=1）
        if (target != null && !target.isEmpty() && home != null && !home.equals(target)) {
            LoginUserInfo info = CompanyContext.get();
            if (info == null || !permissionService.hasAbility(info.getEmployeeId(), ApiPermissionPathSupport.ABILITY_SWITCH_COMPANY)) {
                throw new Exception("您未被授予「可切换公司」权限，无法查看其它公司的看板数据");
            }
        }
        // 集团聚合白名单：员工请求一律只聚合可见集合
        bo.setScopeCompanyIds(new ArrayList<>(visible));
    }

    private void guardGroup(DashboardQueryBO bo) throws Exception {
        // 集团总表 = 全集团聚合，授权边界是「能否访问数据统计(can_view_statistics)」——该能力已在
        // CompanyInterceptor 对 /mp/dashboard/* 统一门控，能走到本接口的员工必然已具备看统计权限。
        // 故这里【不再按“员工可见公司”收敛集团聚合】(scopeCompanyIds 不注入)，集团即聚合 getCompanyList()
        // 全部公司；仅保留：若客户端显式传了 companyId，仍须 ∈ 可见集合（防绕过切到未授权单公司）。
        Set<String> visible = employeeVisible();
        String target = bo == null ? null : bo.getCompanyId();
        String home = currentCompanyId();
        // 员工请求：显式带越界 companyId 拒绝
        if (visible != null && target != null && !target.isEmpty() && !visible.contains(target)) {
            throw new Exception("您未被授权查看该公司数据，请在公司选择中切换可见公司");
        }
        // 集团总表内不再注入 scopeCompanyIds 收敛，集团聚合全量公司（见 DashboardServiceImpl.resolveScopeCompanies）
        bo.setScopeCompanyIds(null);
    }

    /** 当前请求员工的本登录公司 id（CompanyContext），无/非员工则返回 null */
    private String currentCompanyId() {
        LoginUserInfo info = CompanyContext.get();
        if (info == null) return null;
        return info.getCompanyId();
    }

    /**
     * 人员信息统计
     * 包含：期末在职人数、入职人数、离职人数、净增员、平均年龄、平均司龄
     * 图表：入离职趋势、性别结构、学历结构、年龄段分布、司龄段分布、各部门人数
     */
    @PostMapping("/personnel")
    public successResult personnel(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
Map<String, Object> data = dashboardService.personnelOverview(bo);
            result.setData(data);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 人员信息 - 结构分布（学历/年龄/司龄/性别）
     */
    @PostMapping("/personnel/structure")
    public successResult personnelStructure(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
            result.setData(dashboardService.structure(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 人员信息 - 员工明细分页（2b）
     * 入参 bo.page / bo.pageSize 控制分页；其它筛选（deptId/period）与 DashboardQueryBO 一致
     */
    @PostMapping("/personnel/detail")
    public successResult personnelDetail(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
            result.setData(dashboardService.personnelPageList(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 薪酬成本统计
     * 包含：应发工资、实发工资、社保、公积金、奖金、人均应发、个税
     * 图表：薪酬趋势、成本构成、部门人均
     */
    @PostMapping("/salary")
    public successResult salary(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
Map<String, Object> data = dashboardService.salaryOverview(bo);
            result.setData(data);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 薪酬成本 - 趋势
     */
    @PostMapping("/salary/trend")
    public successResult salaryTrend(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
result.setData(dashboardService.salaryTrend(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 薪酬成本 - 部门对比
     */
    @PostMapping("/salary/deptCompare")
    public successResult salaryDeptCompare(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
result.setData(dashboardService.salaryDeptCompare(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 绩效考核指标统计
     * 包含：综合达成率、安全/质量/营收/成本/产能指标达成率
     * 图表：雷达图、目标完成率、趋势、部门达成
     */
    @PostMapping("/performance")
    public successResult performance(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
Map<String, Object> data = dashboardService.perfTrend(bo);
            result.setData(data);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 绩效考核指标 - 完成率
     */
    @PostMapping("/performance/completion")
    public successResult performanceCompletion(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
result.setData(dashboardService.perfCompletion(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 入/离职分析统计
     * 包含：入职人数、离职人数、净增员、离职率、主动/被动占比、关键岗位流失
     * 图表：入离职趋势、离职类型分布、离职原因、部门对比
     */
    @PostMapping("/flow")
    public successResult flow(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
Map<String, Object> data = dashboardService.flowOverview(bo);
            result.setData(data);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 入/离职分析 - 趋势
     */
    @PostMapping("/flow/trend")
    public successResult flowTrend(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
result.setData(dashboardService.flowTrend(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 入/离职分析 - 离职分布
     */
    @PostMapping("/flow/quitDist")
    public successResult flowQuitDist(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
result.setData(dashboardService.quitDist(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 入/离职分析 - 部门对比
     */
    @PostMapping("/flow/deptCompare")
    public successResult flowDeptCompare(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
result.setData(dashboardService.flowDeptCompare(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 入/离职分析 - 人员明细（入/离职混合，recType=in/quit）
     * 入参 bo.pageType/pageNum/pageSize 分页；bo.deptId/startDate/endDate 圈定部门与时间窗口。
     * 供「各部门入职/离职汇总表」点击数字钻取该部门离职/入职人员明细。
     */
    @PostMapping("/flow/detail")
    public successResult flowDetail(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
            com.tianye.hrsystem.common.BasePage<Map<String, Object>> page = dashboardService.flowPageList(bo);
            Map<String, Object> out = new java.util.HashMap<>();
            out.put("records", page.getList() == null ? new java.util.ArrayList<>() : page.getList());
            out.put("total", page.getTotalRow() == null ? 0L : page.getTotalRow());
            result.setData(out);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 集团总表统计
     * 包含：集团总人数、各公司人数、各公司成本
     */
    @PostMapping("/group")
    public successResult group(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardGroup(bo);
Map<String, Object> data = dashboardService.personnelGroupOverview(bo);
            result.setData(data);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 集团总表 - 薪酬概览
     */
    @PostMapping("/group/salary")
    public successResult groupSalary(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardGroup(bo);
Map<String, Object> data = dashboardService.salaryGroupOverview(bo);
            result.setData(data);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 部门统计 - 部门树结构（不含编制数据）
     * 返回部门层级结构和人数
     */
    @GetMapping("/deptTree")
    public successResult deptTree() {
        successResult result = new successResult();
        try {
            DashboardQueryBO bo = new DashboardQueryBO();
            result.setData(dashboardService.structure(bo));
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 部门统计 - 部门人员概览
     * 返回各部门 {name, value=在职人数, planNum=编制（NULL=未配置）}（2026-09-10，4 部门统计编制真实化）
     */
    @PostMapping("/deptOverview")
    public successResult deptOverview(DashboardQueryBO bo) {
        successResult result = new successResult();
        try {
            guardCompany(bo);
            bo.setDim("dept");
            java.util.List<java.util.Map<String, Object>> rows = dashboardService.structure(bo);
            if (rows != null && !rows.isEmpty()) {
                java.util.List<String> names = new java.util.ArrayList<>();
                for (java.util.Map<String, Object> row : rows) {
                    Object n = row.get("name");
                    if (n != null) names.add(String.valueOf(n));
                }
                java.util.Map<String, Integer> planByName = new java.util.HashMap<>();
                for (java.util.Map<String, Object> p : deptRepository.findPlanNumByNames(names)) {
                    planByName.put(String.valueOf(p.get("name")), (Integer) p.get("planNum"));
                }
                for (java.util.Map<String, Object> row : rows) {
                    Integer plan = planByName.get(String.valueOf(row.get("name")));
                    row.put("planNum", plan); // null=未配置，前端按真实在职显示
                }
            }
            result.setData(rows);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 公司切换 - 当前员工可见公司清单 [{companyId, companyName}]（含本登录公司）。
     * PC 操作员调用返回全量公司。
     */
    @GetMapping("/visibleCompanies")
    public successResult visibleCompanies() {
        successResult result = new successResult();
        try {
            java.util.List<Map<String, Object>> out = new java.util.ArrayList<>();
            Set<String> visible = employeeVisible();
            for (com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO comp : dashboardService.companyList()) {
                if (visible != null && !visible.contains(comp.getCompanyId())) {
                    continue;
                }
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("companyId", comp.getCompanyId());
                item.put("companyName", comp.getCompanyName());
                out.add(item);
            }
            result.setData(out);
        } catch (Exception e) {
            result.raiseException(e);
        }
        return result;
    }
}
