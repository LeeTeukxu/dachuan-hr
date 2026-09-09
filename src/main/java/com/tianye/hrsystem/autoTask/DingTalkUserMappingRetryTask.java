package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.common.EmployeeNotInDingTalkException;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.config.CompanyDataSourceProvider;
import com.tianye.hrsystem.enums.EmployeeEntryStatus;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.company.service.TbCompanyListService;
import com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.service.IHrmAttendanceApprovalSyncService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工钉钉 userId 映射补齐任务（映射前置保证的兜底环节）。
 *
 * 背景：2026-09 决议员工保存时按「姓名+手机号」映射 dingtalk_user_id，查无此人拒绝保存；
 * 钉钉服务异常时允许保存为「待映射」（dingtalk_user_id 留空），本任务负责补齐：
 * - 每日定时遍历全部租户，对待映射的在职员工重试；
 * - 提供 remapEmployees 供员工管理页「重新映射」按钮手动触发（scheduling.enabled=false 环境下的主通道）。
 */
@Component
public class DingTalkUserMappingRetryTask {

    private static final Logger logger = LoggerFactory.getLogger(DingTalkUserMappingRetryTask.class);

    @Autowired
    private TbCompanyListService companyListService;

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private IHrmAttendanceApprovalSyncService approvalSyncService;

    @Scheduled(cron = "0 40 6 * * ?")
    public void retryAll() {
        try {
            List<QueryCompanyListVO> companies = companyListService.getCompanyList();
            logger.info("[钉钉映射补齐] 共{}个租户需要检查", companies.size());
            for (QueryCompanyListVO company : companies) {
                try {
                    retryCompany(company.getCompanyId());
                } catch (Exception e) {
                    logger.error("[钉钉映射补齐][{}] 处理失败", company.getCompanyId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("[钉钉映射补齐] 任务执行异常", e);
        }
    }

    /**
     * 手动重映射钉钉用户（员工管理「重新映射」按钮）。请求线程已带租户上下文。
     *
     * 勾选模式：employeeIds 非空，仅处理指定的员工；
     * 全量模式：employeeIds 为空（前端未勾选直接点按钮），自动补齐当前租户全部
     * 「待映射」员工，即 dingtalk_user_id 为空 且 在职(is_del=0, entry_status in(1,3)) 的员工，
     * 不再出现「未勾选=空数组=0人」的假结果。
     *
     * 已绑定 dingtalk_user_id 的员工一律跳过、不计数（避免「成功 N 人」里混入本就无需映射的人）。
     *
     * @return scope=selected|all（本次是勾选还是全量）
     *         scanned=本次参与判断的目标人数
     *         mapped=真正通过反查新补上 dingtalk_user_id 的员工
     *         skipped=已绑定、无需映射而被跳过的员工
     *         failed=映射失败清单（employeeId/employeeName/message）
     */
    public Map<String, Object> remapEmployees(List<Long> employeeIds) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        boolean selectedScope = employeeIds != null && !employeeIds.isEmpty();
        // 目标集合：勾选模式按 id 精确取；全量模式自动取待映射在职员工
        List<HrmEmployee> targets = new ArrayList<>();
        if (selectedScope) {
            for (Long employeeId : employeeIds) {
                if (employeeId == null) {
                    continue;
                }
                HrmEmployee emp = employeeRepository.findById(employeeId).orElse(null);
                if (emp == null) {
                    failed.add(failRow(employeeId, "", "员工不存在"));
                    continue;
                }
                targets.add(emp);
            }
        } else {
            targets = loadPendingEmployees();
        }
        for (HrmEmployee employee : targets) {
            Long employeeId = employee.getEmployeeId();
            // 已绑定钉钉号 -> 跳过，不计数
            if (StringUtils.isNotBlank(employee.getDingtalkUserId())) {
                skipped.add(okRow(employee));
                continue;
            }
            try {
                String userId = approvalSyncService.ensureDingTalkUserId(employee);
                employee.setDingtalkUserId(userId);
                employeeRepository.save(employee);
                mapped.add(okRow(employee));
            } catch (Exception ex) {
                failed.add(failRow(employeeId, employee.getEmployeeName(), ex.getMessage()));
            }
        }
        return result(selectedScope ? "selected" : "all", mapped, skipped, failed);
    }

    /**
     * 全量模式取数：当前租户下 dingtalk_user_id 为空 且 在职（is_del=0，entry_status in(1,3)）的员工。
     * 与员工管理其它在职口径一致（在职/待离职，排除待入职与已离职）。
     */
    private List<HrmEmployee> loadPendingEmployees() {
        List<HrmEmployee> all = employeeRepository.findAllByIsDelAndEntryStatusIn(0,
                Arrays.asList(EmployeeEntryStatus.IN.getValue(), EmployeeEntryStatus.TO_LEAVE.getValue()));
        List<HrmEmployee> pending = new ArrayList<>();
        for (HrmEmployee e : all) {
            if (StringUtils.isBlank(e.getDingtalkUserId())) {
                pending.add(e);
            }
        }
        return pending;
    }

    private void retryCompany(String companyId) {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        CompanyContext.set(info);
        CompanyDataSourceProvider.markActive(companyId);
        try {
            List<HrmEmployee> employees =
                    employeeRepository.findAllByIsDelAndEntryStatusIn(0, Arrays.asList(1));
            int fixed = 0;
            for (HrmEmployee employee : employees) {
                if (StringUtils.isNotBlank(employee.getDingtalkUserId())) {
                    continue;
                }
                try {
                    String userId = approvalSyncService.ensureDingTalkUserId(employee);
                    employee.setDingtalkUserId(userId);
                    employeeRepository.save(employee);
                    fixed++;
                } catch (EmployeeNotInDingTalkException ex) {
                    // 查无此人属于数据问题，等 HR 补齐钉钉/资料，不刷错误日志
                    logger.info("[钉钉映射补齐][{}] {}", companyId, ex.getMessage());
                } catch (Exception ex) {
                    logger.warn("[钉钉映射补齐][{}] 员工{} 映射暂缓", companyId, employee.getEmployeeId(), ex);
                }
            }
            if (fixed > 0) {
                logger.info("[钉钉映射补齐][{}] 本次补齐{}人", companyId, fixed);
            }
        } finally {
            CompanyContext.clear();
            CompanyDataSourceProvider.markInactive(companyId);
        }
    }

    private Map<String, Object> result(String scope,
                                       List<Map<String, Object>> mapped,
                                       List<Map<String, Object>> skipped,
                                       List<Map<String, Object>> failed) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", scope);
        result.put("scanned", mapped.size() + skipped.size() + failed.size());
        result.put("mapped", mapped);
        result.put("skipped", skipped);
        result.put("failed", failed);
        return result;
    }

    private Map<String, Object> okRow(HrmEmployee employee) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("employeeId", employee.getEmployeeId());
        row.put("employeeName", employee.getEmployeeName());
        return row;
    }

    private Map<String, Object> failRow(Long employeeId, String employeeName, String message) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("employeeId", employeeId);
        row.put("employeeName", employeeName);
        row.put("message", message);
        return row;
    }
}
