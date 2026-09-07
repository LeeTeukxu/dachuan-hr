package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.common.EmployeeNotInDingTalkException;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.config.CompanyDataSourceProvider;
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
     * 手动重映射指定员工（员工管理「重新映射」按钮）。请求线程已带租户上下文。
     *
     * @return mapped=本次成功映射的员工；failed=失败清单（employeeId/employeeName/message）
     */
    public Map<String, Object> remapEmployees(List<Long> employeeIds) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        if (employeeIds == null || employeeIds.isEmpty()) {
            return result(mapped, failed);
        }
        for (Long employeeId : employeeIds) {
            if (employeeId == null) {
                continue;
            }
            HrmEmployee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                failed.add(failRow(employeeId, "", "员工不存在"));
                continue;
            }
            if (StringUtils.isNotBlank(employee.getDingtalkUserId())) {
                mapped.add(okRow(employee));
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
        return result(mapped, failed);
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

    private Map<String, Object> result(List<Map<String, Object>> mapped, List<Map<String, Object>> failed) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mapped", mapped);
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
