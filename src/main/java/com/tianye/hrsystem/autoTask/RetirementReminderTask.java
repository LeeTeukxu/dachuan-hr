package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.config.CompanyDataSourceProvider;
import com.tianye.hrsystem.entity.po.AdminMessage;
import com.tianye.hrsystem.imple.AdminMessageServiceImpl;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.modules.company.service.TbCompanyListService;
import com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 员工到龄退休提醒定时任务
 *
 * 规则（简化口径）：男性 63 岁、女性 58 岁到龄退休（2025 年延迟退休政策的目标年龄）。
 * 每天上午 9 点遍历所有租户，对"本月达到法定退休年龄"的在职员工发送站内信提醒，
 * 同一员工同一月份只提醒一次（Redis 去重）。
 *
 * 配置开关：hrm.retirement-reminder.enabled=false 可关闭（默认开启）
 */
@Component
@ConditionalOnProperty(name = "hrm.retirement-reminder.enabled", havingValue = "true", matchIfMissing = true)
public class RetirementReminderTask {

    private static final Logger logger = LoggerFactory.getLogger(RetirementReminderTask.class);

    private static final int SEX_MALE = 1;
    private static final int SEX_FEMALE = 2;
    private static final int RETIREMENT_AGE_MALE = 63;
    private static final int RETIREMENT_AGE_FEMALE = 58;
    private static final int ENTRY_STATUS_QUIT = 4;
    private static final int IS_DEL_DELETED = 1;
    private static final int DEDUPE_KEY_EXPIRE_SECONDS = 40 * 24 * 3600;

    @Autowired
    private TbCompanyListService companyListService;

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private AdminMessageServiceImpl adminMessageService;

    @Autowired
    private Redis redis;

    /**
     * 每天上午 9 点全租户检查。
     * 注意：全项目 @Scheduled 调度受 scheduling.enabled 开关控制（各环境目前均为 false，
     * 见 SchedulerConfig），关闭时由 /adminMessage/unreadCount 每天首个进页面请求触发当天检查，
     * 或经 /hrmEmployee/retirementRemind 手动触发。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void remind() {
        try {
            List<QueryCompanyListVO> companies = companyListService.getCompanyList();
            logger.info("[到龄退休提醒] 共{}个租户需要检查", companies.size());
            for (QueryCompanyListVO company : companies) {
                try {
                    processCompany(company.getCompanyId());
                } catch (Exception e) {
                    logger.error("[到龄退休提醒][{}] 处理失败", company.getCompanyId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("[到龄退休提醒] 任务执行异常", e);
        }
    }

    /**
     * 供"获取未读数"接口顺带调用：每个公司每天只真正检查一次（Redis 闸门），
     * 命中闸门时直接返回，几乎零开销。完成后恢复原租户上下文，不影响调用方。
     */
    public void checkCompanyQuietly(String companyId) {
        if (companyId == null || companyId.isEmpty()) {
            return;
        }
        String gateKey = "retirement:checked:" + companyId + ":" + LocalDate.now();
        try {
            if (redis.get(gateKey) != null) {
                return;
            }
            redis.setex(gateKey, 2 * 24 * 3600, "1");
        } catch (Exception e) {
            logger.warn("[到龄退休提醒][{}] 闸门检查失败，跳过本次", companyId, e);
            return;
        }
        LoginUserInfo previous = CompanyContext.get();
        try {
            processCompany(companyId);
        } catch (Exception e) {
            logger.error("[到龄退休提醒][{}] 页面触发检查失败", companyId, e);
        } finally {
            if (previous != null) {
                CompanyContext.set(previous);
            } else {
                CompanyContext.clear();
            }
        }
    }

    private void processCompany(String companyId) {
        LoginUserInfo info = new LoginUserInfo();
        info.setCompanyId(companyId);
        CompanyContext.set(info);
        CompanyDataSourceProvider.markActive(companyId);
        try {
            YearMonth currentMonth = YearMonth.now();
            List<HrmEmployee> employees = employeeRepository.findAll();
            int remindCount = 0;
            for (HrmEmployee employee : employees) {
                LocalDate retirementDate = resolveRetirementDate(employee);
                if (retirementDate == null || !YearMonth.from(retirementDate).equals(currentMonth)) {
                    continue;
                }
                String dedupeKey = "retirement:remind:" + companyId + ":" + employee.getEmployeeId() + ":" + currentMonth;
                if (redis.get(dedupeKey) != null) {
                    continue;
                }
                saveRemindMessage(employee, retirementDate);
                redis.setex(dedupeKey, DEDUPE_KEY_EXPIRE_SECONDS, "1");
                remindCount++;
            }
            if (remindCount > 0) {
                logger.info("[到龄退休提醒][{}] 本月到龄{}人，已发送提醒", companyId, remindCount);
            }
        } finally {
            CompanyContext.clear();
            CompanyDataSourceProvider.markInactive(companyId);
        }
    }

    /**
     * 计算员工的法定退休日期；不满足计算条件（已删除、已离职、缺性别或出生日期、性别值未知）返回 null
     */
    private LocalDate resolveRetirementDate(HrmEmployee employee) {
        if (employee.getIsDel() != null && employee.getIsDel() == IS_DEL_DELETED) {
            return null;
        }
        if (employee.getEntryStatus() != null && employee.getEntryStatus() == ENTRY_STATUS_QUIT) {
            return null;
        }
        Integer sex = employee.getSex();
        Date dateOfBirth = employee.getDateOfBirth();
        if (sex == null || dateOfBirth == null) {
            return null;
        }
        Integer retirementAge;
        if (sex == SEX_MALE) {
            retirementAge = RETIREMENT_AGE_MALE;
        } else if (sex == SEX_FEMALE) {
            retirementAge = RETIREMENT_AGE_FEMALE;
        } else {
            return null;
        }
        LocalDate birthDate = dateOfBirth.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return birthDate.plusYears(retirementAge);
    }

    private void saveRemindMessage(HrmEmployee employee, LocalDate retirementDate) {
        AdminMessage message = new AdminMessage();
        message.setTitle("员工到龄退休提醒");
        message.setContent("员工【" + employee.getEmployeeName() + "】将于 " + retirementDate
                + " 达到法定退休年龄，本月到龄，请及时办理退休手续");
        message.setLabel(8); // 人资
        message.setType(207); // HRM_EMPLOYEE_RETIREMENT_REMIND
        message.setLinkUrl("/hrm/employee");
        message.setCreateUser(0L); // 系统
        message.setRecipientUser(0L); // 系统级通知
        message.setCreateTime(java.time.LocalDateTime.now());
        message.setIsRead(0);
        adminMessageService.save(message);
    }
}
