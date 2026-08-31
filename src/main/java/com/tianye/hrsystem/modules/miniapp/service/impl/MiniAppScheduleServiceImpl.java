package com.tianye.hrsystem.modules.miniapp.service.impl;

import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentBO;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentsBO;
import com.tianye.hrsystem.model.HrmDept;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmWorkPlanCustomShift;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.modules.miniapp.service.IMiniAppScheduleService;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppProductScheduleVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppScheduleEmployeeVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppScheduleSaveBO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppStandardProductVO;
import com.tianye.hrsystem.modules.workplan.service.IWorkPlanProductService;
import com.tianye.hrsystem.modules.workplan.vo.WorkPlanPositionVO;
import com.tianye.hrsystem.modules.workplan.vo.WorkPlanProductTreeVO;
import com.tianye.hrsystem.repository.hrmDeptRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmWorkPlanCustomShiftRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IWorkPlanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 小程序「添加排班」（生产排班）：与 PC 端排班管理共用同一套数据模型。
 * 保存 = 按员工拆分后复用 IWorkPlanService.saveEmployeeDayAssignments，落库 tbplanlist
 * （ProductName=产品、LinkName=岗位、shift_source=班次类型），PC 排班管理与考勤立即可见；
 * 查询 = 按日期读 tbplanlist 还原为前端表单结构。
 */
@Service
public class MiniAppScheduleServiceImpl implements IMiniAppScheduleService {

    /** tbplanlist 无 productId 列，休息行（无产品/岗位）回读时以该虚拟产品名聚合 */
    private static final String REST_PRODUCT_NAME = "休息/调休";

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Autowired
    private hrmDeptRepository deptRepository;

    @Autowired
    private tbPlanListRepository planListRepository;

    @Autowired
    private tbattendanceuserRepository attendanceUserRepository;

    @Autowired
    private hrmWorkPlanCustomShiftRepository customShiftRepository;

    @Autowired
    private IWorkPlanService workPlanService;

    @Autowired
    private IWorkPlanProductService workPlanProductService;

    @Override
    public List<MiniAppScheduleEmployeeVO> listSchedulableEmployees() {
        List<HrmEmployee> employees =
                employeeRepository.findAllByIsDelAndEntryStatusIn(0, Arrays.asList(1, 3, 4));
        Map<Long, String> deptNames = loadDeptNames(employees);
        List<MiniAppScheduleEmployeeVO> result = new ArrayList<>();
        for (HrmEmployee employee : employees) {
            result.add(new MiniAppScheduleEmployeeVO(
                    employee.getEmployeeId(),
                    employee.getEmployeeName(),
                    employee.getMobile(),
                    deptNames.get(employee.getDeptId())));
        }
        return result;
    }

    @Override
    public List<MiniAppStandardProductVO> listStandardProducts() {
        List<WorkPlanProductTreeVO> tree = workPlanProductService.queryTree();
        List<MiniAppStandardProductVO> result = new ArrayList<>();
        if (tree == null) {
            return result;
        }
        for (WorkPlanProductTreeVO product : tree) {
            MiniAppStandardProductVO vo = new MiniAppStandardProductVO();
            vo.setId(product.getId());
            vo.setName(product.getProductName());
            List<String> positionNames = new ArrayList<>();
            if (product.getPositions() != null) {
                for (WorkPlanPositionVO position : product.getPositions()) {
                    positionNames.add(position.getPositionName());
                }
            }
            vo.setPositions(positionNames);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveProductSchedule(Long operatorEmployeeId, MiniAppScheduleSaveBO request) throws Exception {
        if (request == null || StringUtils.isBlank(request.getWorkDate())) {
            throw new Exception("排班日期不能为空");
        }
        String workDateText = request.getWorkDate().trim();
        Date workDate = parseDate(workDateText);
        List<MiniAppScheduleSaveBO.ProductItem> products = request.getProducts();
        if (products == null || products.isEmpty()) {
            throw new Exception("请至少添加一个产品");
        }

        // 表单校验 + 按员工聚合
        Map<String, Set<String>> seenPositionNames = new HashMap<>();
        Map<Long, List<SaveWorkPlanEmployeeDayAssignmentBO>> byEmployee = new LinkedHashMap<>();
        Map<Long, Boolean> employeeHasWork = new HashMap<>();
        Map<Long, Boolean> employeeHasRest = new HashMap<>();
        for (MiniAppScheduleSaveBO.ProductItem product : products) {
            if (product == null || StringUtils.isBlank(product.getProductName())) {
                throw new Exception("产品名称不能为空");
            }
            if (product.getPositions() == null || product.getPositions().isEmpty()) {
                throw new Exception("产品\"" + product.getProductName() + "\"请至少添加一个岗位");
            }
            Set<String> names = seenPositionNames.computeIfAbsent(productKey(product), k -> new HashSet<>());
            for (MiniAppScheduleSaveBO.PositionItem position : product.getPositions()) {
                if (position == null || StringUtils.isBlank(position.getName())) {
                    throw new Exception("岗位名称不能为空");
                }
                if (!names.add(position.getName().trim())) {
                    throw new Exception("同一产品中岗位\"" + position.getName() + "\"不能重复");
                }
                String shiftType = normalizeShiftType(position.getShiftType());
                if (shiftType == null) {
                    throw new Exception("岗位\"" + position.getName() + "\"班次类型无效");
                }
                if (("day".equals(shiftType) || "night".equals(shiftType))
                        && StringUtils.isBlank(position.getStartTime())) {
                    throw new Exception("产品\"" + product.getProductName() + "\"的岗位\""
                            + position.getName() + "\"请选择上班时间");
                }
                if (position.getEmployeeIds() == null || position.getEmployeeIds().isEmpty()) {
                    throw new Exception("产品\"" + product.getProductName() + "\"的岗位\""
                            + position.getName() + "\"请至少选择一个人员");
                }
                for (Long id : position.getEmployeeIds()) {
                    byEmployee.computeIfAbsent(id, k -> new ArrayList<>())
                            .add(buildAssignment(product, position, shiftType));
                    boolean isRest = "rest".equals(shiftType);
                    employeeHasWork.put(id, employeeHasWork.getOrDefault(id, false) || !isRest);
                    employeeHasRest.put(id, employeeHasRest.getOrDefault(id, false) || isRest);
                }
            }
        }

        // 员工存在性 + 同日"生产班/休息"互斥校验
        Map<Long, HrmEmployee> employees = loadEmployees(new ArrayList<>(byEmployee.keySet()));
        for (Map.Entry<Long, List<SaveWorkPlanEmployeeDayAssignmentBO>> entry : byEmployee.entrySet()) {
            HrmEmployee employee = employees.get(entry.getKey());
            if (employee == null) {
                throw new Exception("员工不存在(ID:" + entry.getKey() + ")");
            }
            if (employeeHasWork.get(entry.getKey()) && employeeHasRest.get(entry.getKey())) {
                throw new Exception("员工\"" + employee.getEmployeeName()
                        + "\"同一天不能既排生产班又排休息/调休");
            }
        }

        // 复用 PC 端保存逻辑：每员工一次"当日分配"整表替换，落 tbplanlist
        int total = 0;
        for (Map.Entry<Long, List<SaveWorkPlanEmployeeDayAssignmentBO>> entry : byEmployee.entrySet()) {
            SaveWorkPlanEmployeeDayAssignmentsBO bo = new SaveWorkPlanEmployeeDayAssignmentsBO();
            bo.setEmployeeId(entry.getKey());
            bo.setWorkDate(workDateText);
            bo.setWorkshopName("");
            bo.setAssignments(entry.getValue());
            workPlanService.saveEmployeeDayAssignments(bo);
            total += entry.getValue().size();
        }

        // 表单是"按天"视角：该日已有生产/休息排班、但本次表单中未出现的员工视为已删除，清理其当日生产排班
        Set<Long> existingEmployeeIds = queryEmployeeIdsWithScheduleOn(workDate);
        for (Long employeeId : existingEmployeeIds) {
            if (!byEmployee.containsKey(employeeId)) {
                workPlanService.removeEmployeeDayShift(employeeId, workDate);
            }
        }
        return total;
    }

    /** 该日所有出现在生产/休息排班行（detectRowType 非空）中的员工 */
    private Set<Long> queryEmployeeIdsWithScheduleOn(Date workDate) throws ParseException {
        Set<Long> result = new HashSet<>();
        List<tbplanlist> rows = planListRepository.findAllByWorkDateBetweenOrderByIdDesc(workDate, workDate);
        Map<String, tbattendanceuser> attendanceUsers = loadAttendanceUsers(rows);
        for (tbplanlist row : rows) {
            if (detectRowType(row) == null) {
                continue;
            }
            for (String userId : splitUserIds(row.getUserId())) {
                tbattendanceuser user = attendanceUsers.get(userId);
                if (user != null && user.getEmpId() != null) {
                    result.add(user.getEmpId());
                }
            }
        }
        return result;
    }

    @Override
    public MiniAppProductScheduleVO queryProductSchedule(Long operatorEmployeeId, String workDateText) throws Exception {
        if (StringUtils.isBlank(workDateText)) {
            throw new Exception("排班日期不能为空");
        }
        Date workDate = parseDate(workDateText);
        List<tbplanlist> rows = planListRepository.findAllByWorkDateBetweenOrderByIdDesc(workDate, workDate);

        MiniAppProductScheduleVO result = new MiniAppProductScheduleVO();
        result.setWorkDate(workDateText.trim());
        if (rows.isEmpty()) {
            return result;
        }

        // tbplanlist.UserID 可能逗号分隔多人共享行 → 拆分后映射回员工
        Map<String, tbattendanceuser> attendanceUsers = loadAttendanceUsers(rows);
        List<Long> employeeIds = new ArrayList<>();
        for (tbattendanceuser user : attendanceUsers.values()) {
            if (user.getEmpId() != null && !employeeIds.contains(user.getEmpId())) {
                employeeIds.add(user.getEmpId());
            }
        }
        Map<Long, HrmEmployee> employees = loadEmployees(employeeIds);
        Map<Long, String> deptNames = loadDeptNames(new ArrayList<>(employees.values()));
        Map<Long, HrmWorkPlanCustomShift> shiftMap = loadCustomShifts(rows);

        Map<String, MiniAppProductScheduleVO.ProductVO> productMap = new LinkedHashMap<>();
        Map<String, MiniAppProductScheduleVO.PositionVO> positionMap = new LinkedHashMap<>();
        for (tbplanlist row : rows) {
            String rowType = detectRowType(row);
            if (rowType == null) {
                continue;
            }
            for (String userId : splitUserIds(row.getUserId())) {
                tbattendanceuser user = attendanceUsers.get(userId);
                if (user == null || user.getEmpId() == null) {
                    continue;
                }
                HrmEmployee employee = employees.get(user.getEmpId());
                if (employee == null) {
                    continue;
                }
                appendRow(productMap, positionMap, row, rowType, employee, deptNames, shiftMap);
            }
        }
        result.getProducts().addAll(productMap.values());
        return result;
    }

    // ============ 保存辅助 ============

    private SaveWorkPlanEmployeeDayAssignmentBO buildAssignment(MiniAppScheduleSaveBO.ProductItem product,
                                                                MiniAppScheduleSaveBO.PositionItem position,
                                                                String shiftType) {
        SaveWorkPlanEmployeeDayAssignmentBO assignment = new SaveWorkPlanEmployeeDayAssignmentBO();
        if ("rest".equals(shiftType)) {
            // PC 端休息行不带产品/岗位
            assignment.setShiftType("rest");
            assignment.setRestShiftType("adjust".equalsIgnoreCase(StringUtils.trimToEmpty(position.getShiftType()))
                    ? "adjust" : "rest");
            return assignment;
        }
        assignment.setProductMode(product.getProductId() != null ? "standard" : "custom");
        assignment.setProductId(product.getProductId());
        assignment.setProductName(product.getProductName().trim());
        assignment.setPositionName(position.getName().trim());
        assignment.setShiftType("custom");
        assignment.setCustomStart(position.getStartTime());
        assignment.setCustomEnd(Boolean.TRUE.equals(position.getEndTimeUnknown()) ? null : position.getEndTime());
        assignment.setCustomShiftPeriod(shiftType);
        assignment.setCustomContinuousShift(Boolean.TRUE.equals(position.getIsContinuous()));
        return assignment;
    }

    // ============ 查询辅助 ============

    private void appendRow(Map<String, MiniAppProductScheduleVO.ProductVO> productMap,
                           Map<String, MiniAppProductScheduleVO.PositionVO> positionMap,
                           tbplanlist row, String rowType,
                           HrmEmployee employee, Map<Long, String> deptNames,
                           Map<Long, HrmWorkPlanCustomShift> shiftMap) {
        String productName;
        String positionName;
        String shiftType;
        String startTime;
        String endTime;
        boolean endTimeUnknown;
        boolean isContinuous;
        boolean isCustom;
        if ("rest".equals(rowType)) {
            productName = REST_PRODUCT_NAME;
            positionName = "adjust".equalsIgnoreCase(StringUtils.trimToEmpty(row.getRestShiftType()))
                    ? "调休" : "休息";
            shiftType = "rest";
            startTime = null;
            endTime = null;
            endTimeUnknown = false;
            isContinuous = false;
            isCustom = true;
        } else {
            productName = StringUtils.trimToEmpty(row.getProductName());
            positionName = StringUtils.trimToEmpty(row.getLinkName());
            if (productName.isEmpty() || positionName.isEmpty()) {
                return;
            }
            // 时间/时段/连班以自定义班次字典为准（tbplanlist 上是 @Transient）
            HrmWorkPlanCustomShift shift = row.getCustomShiftId() == null
                    ? null : shiftMap.get(row.getCustomShiftId());
            if (shift != null) {
                shiftType = "night".equalsIgnoreCase(StringUtils.trimToEmpty(shift.getShiftPeriod()))
                        ? "night" : "day";
                startTime = StringUtils.trimToNull(shift.getStart1());
                endTime = StringUtils.trimToNull(shift.getEnd1());
                endTimeUnknown = endTime == null;
                isContinuous = Integer.valueOf(1).equals(shift.getContinuousShift());
            } else {
                shiftType = "night".equalsIgnoreCase(StringUtils.trimToEmpty(row.getCustomShiftPeriod()))
                        ? "night" : "day";
                startTime = null;
                endTime = null;
                endTimeUnknown = true;
                isContinuous = Boolean.TRUE.equals(row.getCustomContinuousShift());
            }
            isCustom = false;
        }

        MiniAppProductScheduleVO.ProductVO product = productMap.get(productName);
        if (product == null) {
            product = new MiniAppProductScheduleVO.ProductVO();
            product.setType("custom");
            product.setProductName(productName);
            productMap.put(productName, product);
        }
        String posKey = productName + "#" + positionName;
        MiniAppProductScheduleVO.PositionVO position = positionMap.get(posKey);
        if (position == null) {
            position = new MiniAppProductScheduleVO.PositionVO();
            position.setName(positionName);
            position.setShiftType(shiftType);
            position.setStartTime(startTime);
            position.setEndTime(endTime);
            position.setEndTimeUnknown(endTimeUnknown);
            position.setIsContinuous(isContinuous);
            position.setIsCustom(isCustom);
            positionMap.put(posKey, position);
            product.getPositions().add(position);
        }
        if (!position.getEmployeeIds().contains(employee.getEmployeeId())) {
            position.getEmployeeIds().add(employee.getEmployeeId());
            position.getEmployees().add(new MiniAppProductScheduleVO.EmployeeTagVO(
                    employee.getEmployeeId(), employee.getEmployeeName(),
                    deptNames.get(employee.getDeptId())));
        }
    }

    /** 行类型：rest / production（带产品岗位）；与生产排班无关的行返回 null 跳过 */
    private String detectRowType(tbplanlist row) {
        String value = StringUtils.trimToEmpty(row.getShiftType()).toLowerCase();
        if ("rest".equals(value)) {
            return "rest";
        }
        if (StringUtils.isNotBlank(row.getProductName())) {
            return "production";
        }
        return null;
    }

    private List<String> splitUserIds(String userId) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(userId)) {
            return result;
        }
        for (String part : userId.split(",")) {
            if (StringUtils.isNotBlank(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private Map<String, tbattendanceuser> loadAttendanceUsers(List<tbplanlist> rows) {
        Set<String> userIds = new HashSet<>();
        for (tbplanlist row : rows) {
            userIds.addAll(splitUserIds(row.getUserId()));
        }
        Map<String, tbattendanceuser> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        for (tbattendanceuser user : attendanceUserRepository.findAllByUserIdIn(new ArrayList<>(userIds))) {
            result.put(user.getUserId(), user);
        }
        return result;
    }

    private Map<Long, HrmWorkPlanCustomShift> loadCustomShifts(List<tbplanlist> rows) {
        Set<Long> ids = new HashSet<>();
        for (tbplanlist row : rows) {
            if (row.getCustomShiftId() != null) {
                ids.add(row.getCustomShiftId());
            }
        }
        Map<Long, HrmWorkPlanCustomShift> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        for (HrmWorkPlanCustomShift shift : customShiftRepository.findAllById(ids)) {
            result.put(shift.getId(), shift);
        }
        return result;
    }

    private Map<Long, HrmEmployee> loadEmployees(List<Long> employeeIds) {
        Map<Long, HrmEmployee> result = new HashMap<>();
        if (employeeIds == null || employeeIds.isEmpty()) {
            return result;
        }
        for (HrmEmployee employee : employeeRepository.findAllByEmployeeIdIn(employeeIds)) {
            result.put(employee.getEmployeeId(), employee);
        }
        return result;
    }

    private String productKey(MiniAppScheduleSaveBO.ProductItem product) {
        return product.getProductId() != null ? "s:" + product.getProductId() : "c:" + product.getProductName().trim();
    }

    private Map<Long, String> loadDeptNames(List<HrmEmployee> employees) {
        Map<Long, String> result = new HashMap<>();
        if (employees == null || employees.isEmpty()) {
            return result;
        }
        Set<Long> deptIds = new HashSet<>();
        for (HrmEmployee employee : employees) {
            if (employee != null && employee.getDeptId() != null) {
                deptIds.add(employee.getDeptId());
            }
        }
        if (deptIds.isEmpty()) {
            return result;
        }
        for (HrmDept dept : deptRepository.findAllByDeptIdIn(new ArrayList<>(deptIds))) {
            result.put(dept.getDeptId(), dept.getName());
        }
        return result;
    }

    private String normalizeShiftType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String text = value.trim().toLowerCase();
        if ("day".equals(text) || "night".equals(text) || "adjust".equals(text) || "rest".equals(text)) {
            return text;
        }
        return null;
    }

    private Date parseDate(String date) throws ParseException {
        if (StringUtils.isBlank(date)) {
            throw new ParseException("日期不能为空", 0);
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        fmt.setLenient(false);
        return fmt.parse(date.trim());
    }
}
