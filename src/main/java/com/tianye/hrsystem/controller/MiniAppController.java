package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.model.HrmWorkplanApplication;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.modules.miniapp.service.IMiniAppScheduleService;
import com.tianye.hrsystem.modules.miniapp.service.IMiniAppService;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppDayShiftVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppLoginVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppMonthScheduleVO;
import com.tianye.hrsystem.modules.miniapp.vo.CompanyOptionVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppProductScheduleVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppScheduleEmployeeVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppScheduleSaveBO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppStandardProductVO;
import com.tianye.hrsystem.modules.workplanapplication.service.IWorkPlanApplicationService;
import com.tianye.hrsystem.service.IWorkPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信小程序端接口（/mp/*）。
 * 该前缀在 CompanyInterceptor 中仅校验 token，不做菜单权限（小程序用户无菜单树）。
 * 认证后身份以 CompanyContext.get().getEmployeeId() 为准，前端传参仅作展示。
 */
@RestController
@RequestMapping("/mp")
public class MiniAppController {

    @Autowired
    private IMiniAppService miniAppService;

    @Autowired
    private IMiniAppScheduleService scheduleService;

    @Autowired
    private IWorkPlanApplicationService applicationService;

    @Autowired
    private com.tianye.hrsystem.modules.miniapp.service.IMiniAppPermissionService permissionService;

    @Autowired
    private IWorkPlanService workPlanService;

    // ============ 登录/绑定 ============

    @PostMapping("/login")
    public successResult login(String code) {
        successResult result = new successResult();
        try {
            MiniAppLoginVO vo = miniAppService.login(code);
            result.setData(vo);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/login/bindEmployee")
    public successResult bindEmployee(String ticket, String companyId,
                                      String employeeName, String idNumber) {
        successResult result = new successResult();
        try {
            MiniAppLoginVO vo = miniAppService.bindEmployee(ticket, companyId, employeeName, idNumber);
            result.setData(vo);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/login/bindCompany")
    public successResult bindCompany(String ticket, String companyId) {
        successResult result = new successResult();
        try {
            MiniAppLoginVO vo = miniAppService.bindCompany(ticket, companyId);
            result.setData(vo);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/login/switchCompany")
    public successResult switchCompany() {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            Long employeeId = info.getEmployeeId();
            List<CompanyOptionVO> candidates = miniAppService.switchCompany(employeeId);
            result.setData(candidates);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/login/confirmSwitch")
    public successResult confirmSwitch(String companyId) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            Long employeeId = info.getEmployeeId();
            MiniAppLoginVO vo = miniAppService.confirmSwitch(employeeId, companyId);
            result.setData(vo);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    // ============ 排班查看 ============

    @PostMapping("/mySchedule")
    public successResult mySchedule(String month) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            MiniAppMonthScheduleVO vo = miniAppService.queryMonthSchedule(info.getEmployeeId(), month);
            result.setData(vo);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/mySchedule/day")
    public successResult dayDetail(String date) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            MiniAppDayShiftVO vo = miniAppService.queryDayDetail(info.getEmployeeId(), parseDate(date));
            Map<String, Object> data = new HashMap<>();
            data.put("current", vo);
            data.put("applications", applicationService.listMy(info.getEmployeeId(), null));
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    // ============ 申请 ============

    @PostMapping("/application/submit")
    public successResult submit(String workDate, String shiftType,
                                String customStart, String customEnd,
                                String customShiftPeriod, Boolean customContinuousShift,
                                String restShiftType, String remark) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            HrmWorkplanApplication app = applicationService.submit(
                    info.getEmployeeId(), parseDate(workDate), shiftType,
                    customStart, customEnd, customShiftPeriod, customContinuousShift,
                    restShiftType, remark);
            result.setData(app);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/application/myList")
    public successResult myApplications(String status) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            List<HrmWorkplanApplication> list = applicationService.listMy(info.getEmployeeId(), status);
            result.setData(list);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/application/cancel")
    public successResult cancel(Long id) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            applicationService.cancel(info.getEmployeeId(), id);
            result.setMessage("已撤销");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    // ============ 审批 ============

    @PostMapping("/application/toApprove")
    public successResult toApprove(String scope) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            List<HrmWorkplanApplication> list =
                    applicationService.listToApprove(info.getEmployeeId(), scope);
            result.setData(list);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/application/approve")
    public successResult approve(Long id, String action, String reason) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            // 越权防护：仅申请人的直属上级可审批（PC 管理员审批走 /workPlanApplication 不受限）
            miniAppService.checkApprovalPermission(info.getEmployeeId(), id);
            Object data = applicationService.approve(id, action, reason);
            result.setData(data);
            result.setMessage("操作成功");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    // ============ 生产排班（添加排班） ============

    /** 小程序首页入口显隐：isSupervisor=存在直属下级；canSchedule=被授予添加排班权限（2026-09-06 起两者分开判断） */
    @GetMapping("/isSupervisor")
    public successResult isSupervisor() {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("isSupervisor", miniAppService.isSupervisor(info.getEmployeeId()));
            data.put("canSchedule", permissionService.canSchedule(info.getEmployeeId()));
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /** 可排班员工池（在职员工，含姓名/手机号/部门名，供搜索与展示）。仅被授予添加排班权限的员工可用，防止全员通讯录泄露 */
    @GetMapping("/schedule/employees")
    public successResult scheduleEmployees() {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            if (!permissionService.canSchedule(info.getEmployeeId())) {
                throw new Exception("当前员工没有被授予\"添加排班\"权限，请联系管理员在【排班小程序权限】中配置");
            }
            List<MiniAppScheduleEmployeeVO> list = scheduleService.listSchedulableEmployees();
            result.setData(list);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /** 标准产品列表（复用 PC 端产品管理数据），positions 为岗位名称列表 */
    @GetMapping("/schedule/standardProducts")
    public successResult scheduleStandardProducts() {
        successResult result = new successResult();
        try {
            List<MiniAppStandardProductVO> list = scheduleService.listStandardProducts();
            result.setData(list);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /** 按天保存产品排班（整表替换该日数据），body 为 JSON：{workDate, products:[...]}。仅上级账号可保存 */
    @PostMapping("/schedule/save")
    public successResult scheduleSave(@RequestBody MiniAppScheduleSaveBO request) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            if (!permissionService.canSchedule(info.getEmployeeId())) {
                throw new Exception("当前员工没有被授予\"添加排班\"权限，请联系管理员在【排班小程序权限】中配置");
            }
            int saved = scheduleService.saveProductSchedule(info.getEmployeeId(), request);
            Map<String, Object> data = new HashMap<>();
            data.put("saved", saved);
            result.setData(data);
            result.setMessage("保存成功");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /** 查询某天的产品排班（date yyyy-MM-dd），用于切日预填 */
    @GetMapping("/schedule/query")
    public successResult scheduleQuery(String date) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            MiniAppProductScheduleVO vo = scheduleService.queryProductSchedule(info.getEmployeeId(), date);
            result.setData(vo);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /** 批量设置休息日：将生产体系月休四天的所有在职员工在指定日期设为休息 */
    @GetMapping("/schedule/batchSetRestDay")
    public successResult batchSetRestDay(String workDate) {
        successResult result = new successResult();
        try {
            LoginUserInfo info = CompanyContext.get();
            if (info.getEmployeeId() == null) {
                throw new Exception("当前登录身份缺少员工信息");
            }
            if (!permissionService.canSchedule(info.getEmployeeId())) {
                throw new Exception("当前员工没有被授予\"添加排班\"权限");
            }
            if (workDate == null || workDate.trim().isEmpty()) {
                throw new Exception("日期不能为空");
            }
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            fmt.setLenient(false);
            Date date = fmt.parse(workDate.trim());
            int count = workPlanService.batchSetRestDay(date);
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    private Date parseDate(String date) throws ParseException {
        if (date == null || date.trim().isEmpty()) {
            throw new ParseException("日期不能为空", 0);
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        fmt.setLenient(false);
        return fmt.parse(date);
    }
}
