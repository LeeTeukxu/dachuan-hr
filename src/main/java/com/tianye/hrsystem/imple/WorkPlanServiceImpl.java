package com.tianye.hrsystem.imple;

import com.alibaba.fastjson.JSON;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceGetsimplegroupsRequest;
import com.dingtalk.api.request.OapiAttendanceGroupMemberusersListRequest;
import com.dingtalk.api.request.OapiAttendanceGroupScheduleAsyncRequest;
import com.dingtalk.api.request.OapiAttendanceGroupUsersAddRequest;
import com.dingtalk.api.request.OapiAttendanceGroupUsersRemoveRequest;
import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import com.dingtalk.api.response.OapiAttendanceGroupMemberusersListResponse;
import com.dingtalk.api.response.OapiAttendanceGroupScheduleAsyncResponse;
import com.dingtalk.api.response.OapiAttendanceGroupUsersAddResponse;
import com.dingtalk.api.response.OapiAttendanceGroupUsersRemoveResponse;
import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.QueryAttendanceDailyDetailBO;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentBO;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentsBO;
import com.tianye.hrsystem.entity.vo.HrmAttendanceShiftVO;
import com.tianye.hrsystem.entity.vo.WorkPlanCustomShiftOptionVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayAssignmentVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayAssignmentsVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.entity.vo.WorkPlanImportPreviewVO;
import com.tianye.hrsystem.entity.vo.WorkPlanImportPreviewRowVO;
import com.tianye.hrsystem.entity.vo.WorkPlanSubmitProgressVO;
import com.tianye.hrsystem.entity.vo.WorkPlanSubmitResultVO;
import com.tianye.hrsystem.entity.vo.WorkPlanSubmitRowErrorVO;
import com.tianye.hrsystem.imple.workplan.WorkPlanCustomShiftResolver;
import com.tianye.hrsystem.imple.workplan.WorkPlanDingTalkErrorTranslator;
import com.tianye.hrsystem.imple.workplan.ResolvedWorkPlanAssignment;
import com.tianye.hrsystem.mapper.WorkPlanMapper;
import com.tianye.hrsystem.model.HrmAttendanceShift;
import com.tianye.hrsystem.model.HrmAttendanceGroup;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.HrmWorkPlanCustomShift;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.UserObject;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.hrmWorkPlanCustomShiftRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHrmAttendanceShiftService;
import com.tianye.hrsystem.service.IWorkPlanService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class WorkPlanServiceImpl implements IWorkPlanService {

    private static final Logger logger = LoggerFactory.getLogger(WorkPlanServiceImpl.class);

    private static final String SHIFT_TYPE_STANDARD = "standard";
    private static final String SHIFT_TYPE_CUSTOM = "custom";
    private static final String SHIFT_TYPE_REST = "rest";
    private static final String SHIFT_TYPE_EMPTY = "empty";
    private static final String REST_SHIFT_TYPE_ADJUST = "adjust";
    private static final String REST_SHIFT_TYPE_REST = "rest";
    private static final String CUSTOM_SHIFT_PERIOD_DAY = "day";
    private static final String CUSTOM_SHIFT_PERIOD_NIGHT = "night";
    private static final String DAY_SHIFT_SOURCE_LOCAL = "local";
    private static final String DAY_SHIFT_SOURCE_ATTENDANCE = "attendance";
    private static final String DAY_SHIFT_SOURCE_EMPTY = "empty";
    private static final String SUBMIT_STATUS_IDLE = "IDLE";
    private static final String SUBMIT_STATUS_PENDING = "PENDING";
    private static final String SUBMIT_STATUS_RUNNING = "RUNNING";
    private static final String SUBMIT_STATUS_SUCCESS = "SUCCESS";
    private static final String SUBMIT_STATUS_FAILED = "FAILED";
    private static final String LOCAL_CUSTOM_SHIFT_NAME_PREFIX = "自定义班次 ";
    private static final String IMPORT_TEMPLATE_FILE_NAME = "排班导入模板.xlsx";
    private static final String IMPORT_TEMPLATE_SHEET_NAME = "排班明细";
    private static final String IMPORT_HEADER_WORK_DATE = "排班日期";
    private static final String IMPORT_HEADER_EMPLOYEE = "员工";
    private static final String IMPORT_HEADER_PHONE = "电话";
    private static final String IMPORT_HEADER_MOBILE = "手机号";
    private static final String IMPORT_HEADER_SHIFT_TYPE = "排班类型";
    private static final String IMPORT_HEADER_SCHEDULE_TIME = "排班时间";
    private static final String IMPORT_HEADER_SHIFT_PERIOD = "白班/夜班";
    private static final String IMPORT_HEADER_CONTINUOUS_SHIFT = "是否连班";
    private static final String IMPORT_HEADER_REMARK = "备注";
    private static final String WORKPLAN_HORIZONTAL_HEADER_DATE = "日期";
    private static final String WORKPLAN_HORIZONTAL_HEADER_EMPLOYEE = "姓名";
    private static final String WORKPLAN_HORIZONTAL_HEADER_PHONE = "电话";
    private static final String WORKPLAN_HORIZONTAL_HEADER_PRODUCT = "产品";
    private static final String WORKPLAN_HORIZONTAL_HEADER_POSITION = "岗位";
    private static final String WORKPLAN_HORIZONTAL_HEADER_WORKSHOP = "车间";
    private static final String WORKPLAN_HORIZONTAL_HEADER_SHIFT_PERIOD = "白/夜班";
    private static final String WORKPLAN_HORIZONTAL_HEADER_START = "上班时间";
    private static final String WORKPLAN_HORIZONTAL_HEADER_END = "下班时间";
    private static final String WORKPLAN_HORIZONTAL_HEADER_REST = "休假/调休";
    private static final int EMPLOYEE_CONTINUOUS_SHIFT_YES = 1;
    private static final int SUBMIT_CACHE_TTL_MINUTES = 10;
    private static final int DISPLAY_CACHE_TTL_MINUTES = 30;
    private static final String DISPLAY_USER_CACHE_KEY_SUFFIX = "_getAllUsers_display_v3";
    private static final int MAX_RETRY_COUNT = 10;
    private static final long SUBMIT_PROGRESS_TTL_MS = TimeUnit.HOURS.toMillis(6);
    // 排班自定义班次补充元数据 TTL：库内 custom_shift_id 仍是第一来源，此处只是兜底缓存，
    // 30 天过期可避免排班记录删除后 key 永久残留
    private static final long PLAN_META_CACHE_TTL_DAYS = 30;
    // 有界托管线程池：避免 Executors.newCachedThreadPool() 无上限创建线程导致内存/句柄耗尽。
    // 队列满时由调用方线程同步执行(CallerRunsPolicy)，既限流又不会丢任务。
    private final ExecutorService workPlanExecutor = new ThreadPoolExecutor(
            4, 16, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(512),
            new ThreadFactory() {
                private final AtomicInteger seq = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "work-plan-exec-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy());
    private static final ConcurrentHashMap<String, SubmitTaskState> SUBMIT_TASK_MAP = new ConcurrentHashMap<>();

    @PreDestroy
    public void shutdownWorkPlanExecutor() {
        workPlanExecutor.shutdown();
    }

    private final WorkPlanCustomShiftResolver customShiftResolver = new WorkPlanCustomShiftResolver();
    private final WorkPlanDingTalkErrorTranslator dingTalkErrorTranslator = new WorkPlanDingTalkErrorTranslator();

    /** 排班推送钉钉考勤组开关（2026-09 决议：弃用推送，本地 tbplanlist 为唯一事实源；默认关闭，置 true 可回滚旧行为） */
    @org.springframework.beans.factory.annotation.Value("${hrm.dingtalk.schedule-push.enabled:false}")
    private boolean schedulePushEnabled;

    @Autowired
    tbPlanListRepository planRep;
    @Autowired
    IAccessToken tokener;
    @Autowired
    WorkPlanMapper workPlanMapper;
    @Autowired
    hrmAttendanceShiftRepository shiftRep;
    @Autowired
    hrmAttendanceGroupRepository groupRep;
    @Autowired
    hrmWorkPlanCustomShiftRepository customShiftRep;
    @Autowired
    StringRedisTemplate redisRep;
    @Autowired
    tbattendanceuserRepository userRep;
    @Autowired
    hrmEmployeeRepository employeeRep;
    @Autowired
    IHrmAttendanceShiftService attendanceShiftService;

    @Override
    public WorkPlanSubmitResultVO AddAll(List<tbplanlist> planList) throws Exception {
        if (planList == null || planList.isEmpty()) {
            throw new Exception("排班数据不能为空");
        }
        clearExpiredSubmitProgress();
        String taskId = UUID.randomUUID().toString().replace("-", "");
        LoginUserInfo contextSnapshot = copyContext(CompanyContext.get());
        List<tbplanlist> submitPlans = clonePlans(planList);
        SubmitTaskState state = new SubmitTaskState();
        state.taskId = taskId;
        state.status = SUBMIT_STATUS_PENDING;
        state.stage = "PREPARE";
        state.message = "排班提交任务已创建";
        state.retryCount = 0;
        state.maxRetryCount = MAX_RETRY_COUNT;
        state.totalCount = Math.max(countPlanUnits(submitPlans), submitPlans.size());
        state.successCount = 0;
        state.failCount = 0;
        state.updateTime = System.currentTimeMillis();
        SUBMIT_TASK_MAP.put(taskId, state);

        CompletableFuture.runAsync(() -> executeSubmitTask(taskId, contextSnapshot, submitPlans), workPlanExecutor);

        WorkPlanSubmitResultVO resultVO = new WorkPlanSubmitResultVO();
        resultVO.setTaskId(taskId);
        resultVO.setStatus(SUBMIT_STATUS_PENDING);
        resultVO.setMessage("排班提交任务已创建");
        return resultVO;
    }

    @Override
    public WorkPlanSubmitProgressVO querySubmitProgress(String taskId) {
        clearExpiredSubmitProgress();
        SubmitTaskState state = SUBMIT_TASK_MAP.get(taskId);
        if (state == null) {
            WorkPlanSubmitProgressVO vo = new WorkPlanSubmitProgressVO();
            vo.setTaskId(taskId);
            vo.setStatus(SUBMIT_STATUS_IDLE);
            vo.setStage("PREPARE");
            vo.setMessage("未找到提交任务");
            vo.setRetryCount(0);
            vo.setMaxRetryCount(MAX_RETRY_COUNT);
            vo.setTotalCount(0);
            vo.setSuccessCount(0);
            vo.setFailCount(0);
            vo.setDone(false);
            vo.setSuccess(false);
            vo.setErrors(new ArrayList<>());
            return vo;
        }
        return toProgressVO(state);
    }

    @Override
    public WorkPlanEmployeeDayShiftVO queryEmployeeDayShift(Long employeeId, Date workDate) throws Exception {
        if (employeeId == null) {
            throw new Exception("employeeId不能为空");
        }
        if (workDate == null) {
            throw new Exception("排班日期不能为空");
        }
        tbattendanceuser attendanceUser = resolveAttendanceUserByEmployeeId(employeeId);
        WorkPlanEmployeeDayShiftVO result = buildBaseEmployeeDayShift(employeeId, attendanceUser, workDate);

        List<tbplanlist> localPlans = findEmployeeDayPlans(attendanceUser.getUserId(), workDate);
        tbplanlist localPlan = pickPreferredEmployeePlan(localPlans);
        if (localPlan != null) {
            fillCustomShiftMeta(Collections.singletonList(localPlan));
            applyLocalPlanToDayShift(result, localPlan);
            return result;
        }

        HrmAttendanceShiftVO attendanceShift = queryAttendanceShift(employeeId, workDate);
        applyAttendanceShiftToDayShift(result, attendanceShift);
        return result;
    }

    @Override
    public WorkPlanEmployeeDayAssignmentsVO queryEmployeeDayAssignments(Long employeeId, Date workDate) throws Exception {
        if (employeeId == null) {
            throw new Exception("employeeId不能为空");
        }
        if (workDate == null) {
            throw new Exception("排班日期不能为空");
        }
        tbattendanceuser attendanceUser = resolveAttendanceUserByEmployeeId(employeeId);
        List<tbplanlist> localPlans = findEmployeeDayPlans(attendanceUser.getUserId(), workDate);
        if (!localPlans.isEmpty()) {
            fillCustomShiftMeta(localPlans);
        }
        return buildEmployeeDayAssignments(employeeId, attendanceUser, workDate, localPlans);
    }

    @Override
    public WorkPlanImportPreviewVO previewImportExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("请选择排班导入Excel文件");
        }
        List<WorkPlanImportPreviewRowVO> rows = readImportRows(file);
        markDuplicateImportRows(rows);
        WorkPlanImportPreviewVO preview = new WorkPlanImportPreviewVO();
        preview.setRows(rows);
        preview.setTotalCount(rows.size());
        int validCount = 0;
        int errorCount = 0;
        for (WorkPlanImportPreviewRowVO row : rows) {
            if (Boolean.TRUE.equals(row.getValid())) {
                validCount++;
            } else {
                errorCount++;
            }
        }
        preview.setValidCount(validCount);
        preview.setErrorCount(errorCount);
        return preview;
    }

    @Override
    public File createImportTemplateExcel() throws Exception {
        File exportDir = new File("export");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new Exception("创建排班模板导出目录失败");
        }
        File template = new File(exportDir, IMPORT_TEMPLATE_FILE_NAME);
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream outputStream = new FileOutputStream(template)) {
            Sheet sheet = workbook.createSheet(IMPORT_TEMPLATE_SHEET_NAME);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            String[] headers = new String[]{
                    IMPORT_HEADER_WORK_DATE,
                    IMPORT_HEADER_EMPLOYEE,
                    IMPORT_HEADER_PHONE,
                    IMPORT_HEADER_SHIFT_TYPE,
                    IMPORT_HEADER_SCHEDULE_TIME,
                    IMPORT_HEADER_SHIFT_PERIOD,
                    IMPORT_HEADER_CONTINUOUS_SHIFT,
                    IMPORT_HEADER_REMARK
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, i == 3 ? 18 * 256 : 16 * 256);
            }
            workbook.write(outputStream);
        }
        return template;
    }

    private List<WorkPlanImportPreviewRowVO> readImportRows(MultipartFile file) throws Exception {
        List<WorkPlanImportPreviewRowVO> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(IMPORT_TEMPLATE_SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            if (sheet == null || sheet.getLastRowNum() < 1) {
                return rows;
            }
            if (isHorizontalWorkplanSheet(sheet, formatter)) {
                rows.addAll(readHorizontalWorkplanRows(sheet, formatter));
            } else {
                Map<String, Integer> headerIndex = readImportHeader(sheet.getRow(0), formatter);
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row sheetRow = sheet.getRow(rowIndex);
                    if (sheetRow == null || isBlankImportRow(sheetRow, formatter)) {
                        continue;
                    }
                    rows.add(readImportRow(sheetRow, rowIndex + 1, headerIndex, formatter));
                }
            }
        }
        buildPlansFromImportRows(rows);
        return rows;
    }

    private boolean isHorizontalWorkplanSheet(Sheet sheet, DataFormatter formatter) {
        Row dateRow = sheet.getRow(0);
        Row headerRow = sheet.getRow(1);
        if (dateRow == null || headerRow == null) {
            return false;
        }
        String dateTitle = formatter.formatCellValue(dateRow.getCell(0)).trim();
        String employeeTitle = formatter.formatCellValue(headerRow.getCell(0)).trim();
        return WORKPLAN_HORIZONTAL_HEADER_DATE.equals(dateTitle)
                && WORKPLAN_HORIZONTAL_HEADER_EMPLOYEE.equals(employeeTitle)
                && !readHorizontalDayColumns(sheet, formatter).isEmpty();
    }

    private List<HorizontalWorkplanDayColumns> readHorizontalDayColumns(Sheet sheet, DataFormatter formatter) {
        List<HorizontalWorkplanDayColumns> columns = new ArrayList<>();
        Row dateRow = sheet.getRow(0);
        Row headerRow = sheet.getRow(1);
        if (dateRow == null || headerRow == null) {
            return columns;
        }
        int lastCell = Math.max(headerRow.getLastCellNum(), 0);
        int firstDateColumn = 1;
        if (hasHorizontalProductPositionColumns(headerRow, formatter)) {
            firstDateColumn = hasHorizontalWorkshopColumn(headerRow, formatter) ? 5 : 4;
        } else if (hasHorizontalPhoneColumn(headerRow, formatter)) {
            firstDateColumn = 2;
        }
        for (int columnIndex = firstDateColumn; columnIndex + 4 < lastCell; columnIndex++) {
            if (!isHorizontalShiftPeriodHeader(formatter.formatCellValue(headerRow.getCell(columnIndex)).trim())) {
                continue;
            }
            if (!WORKPLAN_HORIZONTAL_HEADER_START.equals(formatter.formatCellValue(headerRow.getCell(columnIndex + 1)).trim())
                    || !WORKPLAN_HORIZONTAL_HEADER_END.equals(formatter.formatCellValue(headerRow.getCell(columnIndex + 2)).trim())
                    || !IMPORT_HEADER_CONTINUOUS_SHIFT.equals(formatter.formatCellValue(headerRow.getCell(columnIndex + 3)).trim())
                    || !WORKPLAN_HORIZONTAL_HEADER_REST.equals(formatter.formatCellValue(headerRow.getCell(columnIndex + 4)).trim())) {
                continue;
            }
            String workDate = readHorizontalWorkDate(sheet, dateRow, columnIndex, formatter);
            if (StringUtils.isBlank(workDate)) {
                continue;
            }
            HorizontalWorkplanDayColumns dayColumns = new HorizontalWorkplanDayColumns();
            dayColumns.workDate = workDate;
            dayColumns.shiftPeriodColumn = columnIndex;
            dayColumns.startColumn = columnIndex + 1;
            dayColumns.endColumn = columnIndex + 2;
            dayColumns.continuousShiftColumn = columnIndex + 3;
            dayColumns.restColumn = columnIndex + 4;
            if (hasHorizontalProductPositionColumns(headerRow, formatter)) {
                dayColumns.productColumn = 2;
                dayColumns.positionColumn = 3;
                if (hasHorizontalWorkshopColumn(headerRow, formatter)) {
                    dayColumns.workshopColumn = 4;
                }
            }
            columns.add(dayColumns);
            columnIndex += 4;
        }
        return columns;
    }

    private boolean isHorizontalShiftPeriodHeader(String text) {
        return WORKPLAN_HORIZONTAL_HEADER_SHIFT_PERIOD.equals(text) || IMPORT_HEADER_SHIFT_PERIOD.equals(text);
    }

    private boolean hasHorizontalPhoneColumn(Row headerRow, DataFormatter formatter) {
        return headerRow != null
                && WORKPLAN_HORIZONTAL_HEADER_PHONE.equals(formatter.formatCellValue(headerRow.getCell(1)).trim());
    }

    private boolean hasHorizontalProductPositionColumns(Row headerRow, DataFormatter formatter) {
        return headerRow != null
                && WORKPLAN_HORIZONTAL_HEADER_PRODUCT.equals(formatter.formatCellValue(headerRow.getCell(2)).trim())
                && WORKPLAN_HORIZONTAL_HEADER_POSITION.equals(formatter.formatCellValue(headerRow.getCell(3)).trim());
    }

    private boolean hasHorizontalWorkshopColumn(Row headerRow, DataFormatter formatter) {
        return headerRow != null
                && WORKPLAN_HORIZONTAL_HEADER_WORKSHOP.equals(formatter.formatCellValue(headerRow.getCell(4)).trim());
    }

    private List<WorkPlanImportPreviewRowVO> readHorizontalWorkplanRows(Sheet sheet, DataFormatter formatter) {
        List<WorkPlanImportPreviewRowVO> rows = new ArrayList<>();
        List<HorizontalWorkplanDayColumns> dayColumns = readHorizontalDayColumns(sheet, formatter);
        boolean hasPhoneColumn = hasHorizontalPhoneColumn(sheet.getRow(1), formatter);
        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row sheetRow = sheet.getRow(rowIndex);
            if (sheetRow == null) {
                continue;
            }
            String employeeName = readHorizontalCellText(sheetRow.getCell(0), formatter);
            if (StringUtils.isBlank(employeeName)) {
                continue;
            }
            String mobile = hasPhoneColumn ? readHorizontalCellText(sheetRow.getCell(1), formatter) : "";
            for (HorizontalWorkplanDayColumns dayColumn : dayColumns) {
                WorkPlanImportPreviewRowVO row = buildHorizontalWorkplanRow(sheetRow, rowIndex + 1, employeeName,
                        mobile, dayColumn, formatter);
                if (row != null) {
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private WorkPlanImportPreviewRowVO buildHorizontalWorkplanRow(Row sheetRow, int rowNumber,
                                                                 String employeeName,
                                                                 String mobile,
                                                                 HorizontalWorkplanDayColumns dayColumn,
                                                                 DataFormatter formatter) {
        String shiftPeriod = readHorizontalCellText(sheetRow.getCell(dayColumn.shiftPeriodColumn), formatter);
        String start = readHorizontalCellText(sheetRow.getCell(dayColumn.startColumn), formatter);
        String end = readHorizontalCellText(sheetRow.getCell(dayColumn.endColumn), formatter);
        String continuousShift = readHorizontalCellText(sheetRow.getCell(dayColumn.continuousShiftColumn), formatter);
        String rest = readHorizontalCellText(sheetRow.getCell(dayColumn.restColumn), formatter);
        boolean restDay = isHorizontalTruthy(rest);
        if (!restDay && StringUtils.isBlank(shiftPeriod) && StringUtils.isBlank(start) && StringUtils.isBlank(end)) {
            return null;
        }
        WorkPlanImportPreviewRowVO row = new WorkPlanImportPreviewRowVO();
        row.setRowIndex(rowNumber);
        row.setWorkDate(dayColumn.workDate);
        row.setEmployeeName(employeeName.trim());
        row.setMobile(StringUtils.trimToEmpty(mobile));
        row.setRemark("");
        if (dayColumn.productColumn > 0) {
            row.setProductName(readHorizontalCellText(sheetRow.getCell(dayColumn.productColumn), formatter));
        }
        if (dayColumn.positionColumn > 0) {
            row.setPositionName(readHorizontalCellText(sheetRow.getCell(dayColumn.positionColumn), formatter));
        }
        if (dayColumn.workshopColumn > 0) {
            row.setWorkshopName(readHorizontalCellText(sheetRow.getCell(dayColumn.workshopColumn), formatter));
        }
        if (restDay) {
            row.setShiftType(buildHorizontalRestShiftType(rest));
            row.setScheduleText("");
            row.setCustomShiftPeriod("");
            row.setCustomContinuousShift("");
        } else {
            row.setShiftType("自定义排班");
            row.setScheduleText(StringUtils.trimToEmpty(start) + "-" + StringUtils.trimToEmpty(end));
            row.setCustomShiftPeriod(buildHorizontalShiftPeriod(shiftPeriod));
            row.setCustomContinuousShift(continuousShift);
        }
        return row;
    }

    private String readHorizontalWorkDate(Sheet sheet, Row dateRow, int columnIndex, DataFormatter formatter) {
        Cell dateCell = dateRow.getCell(columnIndex);
        if (dateCell == null) {
            CellRangeAddress mergedRegion = findMergedRegion(sheet, dateRow.getRowNum(), columnIndex);
            if (mergedRegion != null) {
                dateCell = dateRow.getCell(mergedRegion.getFirstColumn());
            }
        }
        if (dateCell == null) {
            return "";
        }
        if (dateCell.getCellType() == CellType.NUMERIC) {
            return formatWorkDate(DateUtil.getJavaDate(dateCell.getNumericCellValue()));
        }
        return parseHorizontalDateText(formatter.formatCellValue(dateCell));
    }

    private CellRangeAddress findMergedRegion(Sheet sheet, int rowIndex, int columnIndex) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.isInRange(rowIndex, columnIndex)) {
                return region;
            }
        }
        return null;
    }

    private String parseHorizontalDateText(String text) {
        String value = StringUtils.trimToEmpty(text);
        if (StringUtils.isBlank(value)) {
            return "";
        }
        Matcher matcher = Pattern.compile("(\\d{4})[-/年](\\d{1,2})[-/月](\\d{1,2})").matcher(value);
        if (matcher.find()) {
            return String.format("%s-%02d-%02d", matcher.group(1),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
        if (value.matches("^\\d+(\\.0+)?$")) {
            return formatWorkDate(DateUtil.getJavaDate(Double.parseDouble(value)));
        }
        return value;
    }

    private String readHorizontalCellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(cell.getDateCellValue());
            return String.format("%d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        }
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isHorizontalTruthy(String text) {
        String value = StringUtils.trimToEmpty(text);
        return "是".equals(value) || "休".equals(value) || "休息".equals(value) || "休假".equals(value)
                || "调休".equals(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private String buildHorizontalRestShiftType(String text) {
        String value = StringUtils.trimToEmpty(text);
        if ("休息".equals(value) || "休".equals(value) || "休假".equals(value)) {
            return "休息";
        }
        return "调休".equals(value) ? "调休" : "休息";
    }

    private String buildHorizontalShiftPeriod(String text) {
        return CUSTOM_SHIFT_PERIOD_NIGHT.equals(normalizeCustomShiftPeriod(text)) ? "夜班" : "白班";
    }

    private Map<String, Integer> readImportHeader(Row headerRow, DataFormatter formatter) throws Exception {
        if (headerRow == null) {
            throw new Exception("排班导入Excel缺少表头");
        }
        Map<String, Integer> headerIndex = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell).trim();
            if (StringUtils.isNotBlank(header)) {
                headerIndex.put(header, cell.getColumnIndex());
            }
        }
        String[] requiredHeaders = new String[]{IMPORT_HEADER_WORK_DATE, IMPORT_HEADER_EMPLOYEE, IMPORT_HEADER_SCHEDULE_TIME};
        for (String header : requiredHeaders) {
            if (!headerIndex.containsKey(header)) {
                throw new Exception("排班导入Excel缺少表头: " + header);
            }
        }
        return headerIndex;
    }

    private WorkPlanImportPreviewRowVO readImportRow(Row sheetRow, int rowNumber,
                                                     Map<String, Integer> headerIndex,
                                                     DataFormatter formatter) {
        WorkPlanImportPreviewRowVO row = new WorkPlanImportPreviewRowVO();
        row.setRowIndex(rowNumber);
        row.setWorkDate(readImportCell(sheetRow, headerIndex, formatter, IMPORT_HEADER_WORK_DATE));
        row.setEmployeeName(readImportCell(sheetRow, headerIndex, formatter, IMPORT_HEADER_EMPLOYEE));
        row.setMobile(readImportCell(sheetRow, headerIndex, formatter, IMPORT_HEADER_PHONE, IMPORT_HEADER_MOBILE));
        row.setShiftType(readImportCell(sheetRow, headerIndex, formatter, IMPORT_HEADER_SHIFT_TYPE));
        row.setScheduleText(readImportCell(sheetRow, headerIndex, formatter, IMPORT_HEADER_SCHEDULE_TIME));
        row.setCustomShiftPeriod(readImportCell(sheetRow, headerIndex, formatter, IMPORT_HEADER_SHIFT_PERIOD));
        row.setCustomContinuousShift(readImportCell(sheetRow, headerIndex, formatter, IMPORT_HEADER_CONTINUOUS_SHIFT));
        row.setRemark(readImportCell(sheetRow, headerIndex, formatter, IMPORT_HEADER_REMARK));
        return row;
    }

    private void buildPlansFromImportRows(List<WorkPlanImportPreviewRowVO> rows) {
        for (WorkPlanImportPreviewRowVO row : rows) {
            List<String> errors = new ArrayList<>();
            try {
                tbplanlist plan = buildPlanFromImportRow(row, errors);
                row.setPlan(plan);
            } catch (Exception ex) {
                errors.add(ex.getMessage());
            }
            row.setErrors(errors);
            row.setValid(errors.isEmpty());
        }
    }

    private WorkPlanImportPreviewRowVO parseImportRow(Row sheetRow, int rowNumber,
                                                      Map<String, Integer> headerIndex,
                                                      DataFormatter formatter) {
        WorkPlanImportPreviewRowVO row = readImportRow(sheetRow, rowNumber, headerIndex, formatter);
        List<String> errors = new ArrayList<>();
        try {
            tbplanlist plan = buildPlanFromImportRow(row, errors);
            row.setPlan(plan);
        } catch (Exception ex) {
            errors.add(ex.getMessage());
        }
        row.setErrors(errors);
        row.setValid(errors.isEmpty());
        return row;
    }

    private String readImportCell(Row sheetRow, Map<String, Integer> headerIndex, DataFormatter formatter,
                                  String... headers) {
        for (String header : headers) {
            Integer columnIndex = headerIndex.get(header);
            if (columnIndex != null) {
                return formatter.formatCellValue(sheetRow.getCell(columnIndex)).trim();
            }
        }
        return "";
    }

    private boolean isBlankImportRow(Row sheetRow, DataFormatter formatter) {
        for (Cell cell : sheetRow) {
            if (StringUtils.isNotBlank(formatter.formatCellValue(cell))) {
                return false;
            }
        }
        return true;
    }

    private tbplanlist buildPlanFromImportRow(WorkPlanImportPreviewRowVO row, List<String> errors) throws Exception {
        Date workDate = parseImportWorkDate(row.getWorkDate());
        HrmEmployee employee = resolveImportEmployee(row.getEmployeeName(), row.getMobile(), errors);
        String shiftType = normalizeImportShiftType(row.getShiftType());
        ImportScheduleTime scheduleTime = SHIFT_TYPE_REST.equals(shiftType)
                ? null
                : parseImportScheduleTime(row.getScheduleText());
        String customShiftPeriod = SHIFT_TYPE_REST.equals(shiftType)
                ? null
                : normalizeCustomShiftPeriod(row.getCustomShiftPeriod());
        // 排班归属身份统一锚定员工档案（employeeId → hrm_employee.dingtalk_user_id），
        // 不再采信考勤用户表的 UserID，杜绝"考勤用户表过期/员工ID兜底"产生的孤儿排班（界面上显示为数字员工行）
        String canonicalUserId = null;
        if (employee != null && employee.getEmployeeId() != null) {
            canonicalUserId = resolveCanonicalPlanUserId(employee, errors);
        }
        if (!errors.isEmpty()) {
            return null;
        }
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(workDate);
        plan.setUserId(canonicalUserId);
        plan.setGroupId(null);
        plan.setProductName(StringUtils.trimToEmpty(row.getProductName()));
        plan.setLinkName(StringUtils.trimToEmpty(row.getPositionName()));
        plan.setWorkshopName(StringUtils.trimToEmpty(row.getWorkshopName()));
        plan.setShiftType(shiftType);
        if (SHIFT_TYPE_REST.equals(shiftType)) {
            plan.setRestShiftType(normalizeRestShiftType(row.getShiftType()));
        }
        plan.setCustomStart(scheduleTime == null ? null : scheduleTime.startText);
        plan.setCustomEnd(scheduleTime == null ? null : scheduleTime.endText);
        plan.setCustomShiftPeriod(customShiftPeriod);
        boolean explicitContinuousShift = StringUtils.isNotBlank(row.getCustomContinuousShift());
        Boolean importedContinuousShift = explicitContinuousShift
                ? parseImportBoolean(row.getCustomContinuousShift())
                : toEmployeeContinuousShift(employee).orElse(false);
        plan.setCustomContinuousShift(SHIFT_TYPE_REST.equals(shiftType)
                ? false : normalizeCustomContinuousShift(importedContinuousShift, customShiftPeriod));
        plan.setCustomContinuousShiftExplicit(!SHIFT_TYPE_REST.equals(shiftType) && explicitContinuousShift);
        plan.setCustomCrossDay(scheduleTime != null && scheduleTime.crossDay);
        return plan;
    }

    private Boolean parseImportBoolean(String text) {
        String value = StringUtils.trimToEmpty(text);
        return "是".equals(value)
                || "true".equalsIgnoreCase(value)
                || "1".equals(value)
                || "连".equals(value)
                || "连班".equals(value);
    }

    private String normalizeImportShiftType(String text) {
        String normalized = normalizeShiftTypeValue(text);
        if (SHIFT_TYPE_REST.equals(normalized)) {
            return SHIFT_TYPE_REST;
        }
        return SHIFT_TYPE_CUSTOM;
    }

    private Date parseImportWorkDate(String text) throws Exception {
        if (StringUtils.isBlank(text)) {
            throw new Exception("排班日期不能为空");
        }
        String value = text.trim();
        String[] patterns = new String[]{"yyyy-MM-dd", "yyyy/M/d", "yyyy/MM/dd"};
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date date = format.parse(value, position);
            if (date != null && position.getIndex() == value.length()) {
                return date;
            }
        }
        throw new Exception("排班日期格式错误，格式应为yyyy-MM-dd");
    }

    private HrmEmployee resolveImportEmployee(String employeeName, String mobile, List<String> errors) {
        if (StringUtils.isBlank(employeeName)) {
            errors.add("员工不能为空");
            return null;
        }
        List<HrmEmployee> employees = employeeRep.findAllByEmployeeName(employeeName.trim());
        if (employees == null || employees.isEmpty()) {
            errors.add("未找到员工: " + employeeName.trim());
            return null;
        }
        if (employees.size() == 1) {
            HrmEmployee employee = employees.get(0);
            if (StringUtils.isNotBlank(mobile) && !normalizeMobile(mobile).equals(normalizeMobile(employee.getMobile()))) {
                errors.add("员工姓名与电话不匹配");
                return null;
            }
            return employee;
        }
        if (StringUtils.isNotBlank(mobile)) {
            String normalizedMobile = normalizeMobile(mobile);
            for (HrmEmployee employee : employees) {
                if (normalizedMobile.equals(normalizeMobile(employee.getMobile()))) {
                    return employee;
                }
            }
            errors.add("员工姓名与电话不匹配");
            return null;
        }
        return employees.get(0);
    }

    private String normalizeMobile(String mobile) {
        return StringUtils.trimToEmpty(mobile).replaceAll("\\s+", "");
    }

    private ImportScheduleTime parseImportScheduleTime(String text) throws Exception {
        if (StringUtils.isBlank(text)) {
            throw new Exception("排班时间不能为空");
        }
        String value = text.trim().replace("～", "-").replace("—", "-").replace("－", "-");
        String startText;
        String endText = null;
        int dashIndex = value.indexOf('-');
        if (dashIndex >= 0) {
            startText = value.substring(0, dashIndex).trim();
            String rawEnd = value.substring(dashIndex + 1).trim();
            if (StringUtils.isNotBlank(rawEnd) && !"结束".equals(rawEnd)) {
                endText = normalizeImportTime(rawEnd, "结束时间");
            }
        } else {
            startText = value;
        }
        startText = normalizeImportTime(startText, "开始时间");
        WorkPlanCustomShiftResolver.NormalizedCustomShift normalized = customShiftResolver.normalize(startText, endText);
        ImportScheduleTime scheduleTime = new ImportScheduleTime();
        scheduleTime.startText = normalized.getStartText();
        scheduleTime.endText = normalized.getEndText();
        scheduleTime.crossDay = normalized.getCrossDay();
        return scheduleTime;
    }

    private String normalizeImportTime(String text, String fieldName) throws Exception {
        String value = StringUtils.trimToEmpty(text);
        if (value.matches("^\\d{1,2}:\\d{1,2}$")) {
            String[] parts = value.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                return String.format("%02d:%02d", hour, minute);
            }
        }
        throw new Exception(fieldName + "格式错误，格式必须为HH:mm");
    }

    private void markDuplicateImportRows(List<WorkPlanImportPreviewRowVO> rows) {
        Map<String, List<WorkPlanImportPreviewRowVO>> grouped = new HashMap<>();
        for (WorkPlanImportPreviewRowVO row : rows) {
            if (!Boolean.TRUE.equals(row.getValid()) || row.getPlan() == null) {
                continue;
            }
            String key = formatWorkDate(row.getPlan().getWorkDate()) + "|" + row.getPlan().getUserId();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        for (List<WorkPlanImportPreviewRowVO> duplicateRows : grouped.values()) {
            if (duplicateRows.size() <= 1) {
                continue;
            }
            for (WorkPlanImportPreviewRowVO row : duplicateRows) {
                row.getErrors().add("同一员工同一排班日期存在重复行");
                row.setValid(false);
                row.setPlan(null);
            }
        }
    }

    private static class ImportScheduleTime {
        private String startText;
        private String endText;
        private Boolean crossDay;
    }

    private static class HorizontalWorkplanDayColumns {
        private String workDate;
        private int shiftPeriodColumn;
        private int startColumn;
        private int endColumn;
        private int continuousShiftColumn;
        private int restColumn;
        private int productColumn;
        private int positionColumn;
        private int workshopColumn;
    }

    @Override
    @Transactional
    public WorkPlanEmployeeDayShiftVO saveEmployeeDayCustomShift(Long employeeId, Date workDate,
                                                                 String customStart, String customEnd,
                                                                 String customShiftPeriod) throws Exception {
        return saveEmployeeDayShift(employeeId, workDate, SHIFT_TYPE_CUSTOM, customStart, customEnd, customShiftPeriod, false);
    }

    @Override
    @Transactional
    public WorkPlanEmployeeDayShiftVO saveEmployeeDayShift(Long employeeId, Date workDate,
                                                           String shiftType, String customStart, String customEnd,
                                                           String customShiftPeriod) throws Exception {
        return saveEmployeeDayShift(employeeId, workDate, shiftType, customStart, customEnd, customShiftPeriod, false);
    }

    @Override
    @Transactional
    public WorkPlanEmployeeDayShiftVO saveEmployeeDayShift(Long employeeId, Date workDate,
                                                           String shiftType, String customStart, String customEnd,
                                                           String customShiftPeriod, Boolean customContinuousShift) throws Exception {
        return saveEmployeeDayShift(employeeId, workDate, shiftType, customStart, customEnd, customShiftPeriod,
                customContinuousShift, null);
    }

    @Override
    @Transactional
    public WorkPlanEmployeeDayShiftVO saveEmployeeDayShift(Long employeeId, Date workDate,
                                                           String shiftType, String customStart, String customEnd,
                                                           String customShiftPeriod, Boolean customContinuousShift,
                                                           String restShiftType) throws Exception {
        return saveEmployeeDayShift(employeeId, workDate, shiftType, customStart, customEnd, customShiftPeriod,
                customContinuousShift, false, restShiftType);
    }

    @Override
    @Transactional
    public WorkPlanEmployeeDayShiftVO saveEmployeeDayShift(Long employeeId, Date workDate,
                                                           String shiftType, String customStart, String customEnd,
                                                           String customShiftPeriod, Boolean customContinuousShift,
                                                           Boolean customContinuousShiftExplicit,
                                                           String restShiftType) throws Exception {
        if (employeeId == null) {
            throw new Exception("employeeId不能为空");
        }
        if (workDate == null) {
            throw new Exception("排班日期不能为空");
        }
        String normalizedShiftType = normalizeShiftTypeValue(shiftType);
        boolean restShift = SHIFT_TYPE_REST.equals(normalizedShiftType);
        String normalizedRestShiftType = normalizeRestShiftType(restShiftType);
        WorkPlanCustomShiftResolver.NormalizedCustomShift normalizedShift = restShift
                ? null
                : customShiftResolver.normalize(customStart, customEnd);
        String normalizedPeriod = normalizeCustomShiftPeriod(customShiftPeriod);
        boolean explicitContinuousShift = Boolean.TRUE.equals(customContinuousShiftExplicit);
        Boolean employeeContinuousShift = explicitContinuousShift
                ? customContinuousShift
                : resolveEmployeeContinuousShift(employeeId).orElse(customContinuousShift);
        Boolean normalizedContinuousShift = normalizeCustomContinuousShift(employeeContinuousShift, normalizedPeriod);
        tbattendanceuser attendanceUser = resolveAttendanceUserByEmployeeId(employeeId);
        List<tbplanlist> localPlans = findEmployeeDayPlans(attendanceUser.getUserId(), workDate);
        tbplanlist templatePlan = pickPreferredEmployeePlan(localPlans);
        tbplanlist reusablePlan = null;
        List<tbplanlist> rowsToUpdate = new ArrayList<>();
        List<tbplanlist> rowsToDelete = new ArrayList<>();

        for (tbplanlist item : localPlans) {
            List<String> userIds = parseUserIds(item.getUserId());
            if (userIds.isEmpty() || !userIds.contains(attendanceUser.getUserId())) {
                continue;
            }
            if (userIds.size() == 1 && attendanceUser.getUserId().equals(userIds.get(0))) {
                if (reusablePlan == null) {
                    reusablePlan = item;
                } else {
                    rowsToDelete.add(item);
                }
                continue;
            }
            List<String> remainUserIds = userIds.stream()
                    .filter(id -> !attendanceUser.getUserId().equals(id))
                    .collect(Collectors.toList());
            if (remainUserIds.isEmpty()) {
                rowsToDelete.add(item);
            } else {
                item.setUserId(String.join(",", remainUserIds));
                rowsToUpdate.add(item);
            }
        }
        if (!rowsToUpdate.isEmpty()) {
            planRep.saveAll(rowsToUpdate);
        }
        if (!rowsToDelete.isEmpty()) {
            planRep.deleteAll(rowsToDelete);
        }

        tbplanlist sourcePlan = new tbplanlist();
        if (templatePlan != null) {
            sourcePlan.setProductName(templatePlan.getProductName());
            sourcePlan.setLinkName(templatePlan.getLinkName());
            sourcePlan.setGroupId(templatePlan.getGroupId());
            sourcePlan.setCreateTime(templatePlan.getCreateTime());
        }
        if (reusablePlan != null) {
            sourcePlan.setId(reusablePlan.getId());
        }
        sourcePlan.setProductName(StringUtils.defaultString(sourcePlan.getProductName()));
        sourcePlan.setLinkName(StringUtils.defaultString(sourcePlan.getLinkName()));
        if (StringUtils.isBlank(sourcePlan.getGroupId()) && attendanceUser.getGroupId() != null) {
            sourcePlan.setGroupId(String.valueOf(attendanceUser.getGroupId()));
        }
        sourcePlan.setGroupId(StringUtils.defaultString(sourcePlan.getGroupId()));
        sourcePlan.setWorkDate(workDate);
        sourcePlan.setUserId(attendanceUser.getUserId());
        sourcePlan.setShiftType(normalizedShiftType);
        tbplanlist resultPlan = sourcePlan;
        if (restShift) {
            sourcePlan.setClassId(null);
            sourcePlan.setCustomShiftId(null);
            sourcePlan.setCustomShiftPeriod(null);
            sourcePlan.setCustomContinuousShift(false);
            sourcePlan.setRestShiftType(normalizedRestShiftType);
            sourcePlan.setCustomStart(null);
            sourcePlan.setCustomEnd(null);
            sourcePlan.setCustomCrossDay(false);
            List<ResolvedWorkPlanAssignment> assignments = buildLocalRestAssignments(Collections.singletonList(sourcePlan));
            persistResolvedAssignments(assignments);
            if (!assignments.isEmpty()) {
                resultPlan = assignments.get(0).getSourcePlan();
            }
        } else {
            sourcePlan.setCustomShiftPeriod(normalizedPeriod);
            sourcePlan.setCustomContinuousShift(normalizedContinuousShift);
            sourcePlan.setCustomContinuousShiftExplicit(explicitContinuousShift);
            sourcePlan.setCustomStart(normalizedShift.getStartText());
            sourcePlan.setCustomEnd(normalizedShift.getEndText());
            sourcePlan.setCustomCrossDay(normalizedShift.getCrossDay());
            List<ResolvedWorkPlanAssignment> assignments = buildLocalCustomAssignments(Collections.singletonList(sourcePlan));
            persistResolvedAssignments(assignments);
            if (!assignments.isEmpty()) {
                resultPlan = assignments.get(0).getSourcePlan();
            }
        }

        WorkPlanEmployeeDayShiftVO result = buildBaseEmployeeDayShift(employeeId, attendanceUser, workDate);
        applyLocalPlanToDayShift(result, resultPlan);
        return result;
    }

    @Override
    @Transactional
    public WorkPlanEmployeeDayAssignmentsVO saveEmployeeDayAssignments(SaveWorkPlanEmployeeDayAssignmentsBO request) throws Exception {
        if (request == null) {
            throw new Exception("排班数据不能为空");
        }
        if (request.getEmployeeId() == null) {
            throw new Exception("employeeId不能为空");
        }
        Date workDate = parseWorkDateText(request.getWorkDate());
        tbattendanceuser attendanceUser = resolveAttendanceUserByEmployeeId(request.getEmployeeId());
        List<tbplanlist> localPlans = findEmployeeDayPlans(attendanceUser.getUserId(), workDate);
        removeEmployeeFromLocalPlans(attendanceUser.getUserId(), localPlans);

        List<tbplanlist> sourcePlans = buildEmployeeDayAssignmentPlans(request, attendanceUser, workDate);
        List<tbplanlist> savedRows = sourcePlans.isEmpty()
                ? Collections.emptyList()
                : persistReplacementAssignments(buildLocalAssignments(sourcePlans));
        return buildEmployeeDayAssignments(request.getEmployeeId(), attendanceUser, workDate, savedRows);
    }

    @Override
    @Transactional
    public void removeEmployeeDayShift(Long employeeId, Date workDate) throws Exception {
        if (employeeId == null) {
            throw new Exception("employeeId不能为空");
        }
        if (workDate == null) {
            throw new Exception("排班日期不能为空");
        }
        tbattendanceuser attendanceUser = resolveAttendanceUserByEmployeeId(employeeId);
        String targetUserId = attendanceUser.getUserId();
        List<tbplanlist> localPlans = findEmployeeDayPlans(targetUserId, workDate);
        removeEmployeeFromLocalPlans(targetUserId, localPlans);
    }

    @Override
    @Transactional
    public int batchSetRestDay(Date workDate) throws Exception {
        if (workDate == null) {
            throw new Exception("排班日期不能为空");
        }
        List<HrmEmployee> allActive = employeeRep.findAllByIsDelAndEntryStatusIn(0, Collections.singletonList(1));
        List<HrmEmployee> targets = allActive.stream()
                .filter(e -> Integer.valueOf(2).equals(e.getAffiliationSystem())
                        && Integer.valueOf(2).equals(e.getRestType()))
                .collect(Collectors.toList());
        if (targets.isEmpty()) {
            return 0;
        }
        Set<String> targetUserIds = new HashSet<>();
        Map<String, HrmEmployee> employeeByUserId = new HashMap<>();
        for (HrmEmployee emp : targets) {
            String userId = emp.getDingtalkUserId();
            if (StringUtils.isBlank(userId)) {
                continue;
            }
            targetUserIds.add(userId);
            employeeByUserId.put(userId, emp);
        }
        if (targetUserIds.isEmpty()) {
            return 0;
        }
        List<tbplanlist> dayPlans = planRep.findAllByWorkDateBetweenOrderByIdDesc(startOfDay(workDate), endOfDay(workDate));
        List<tbplanlist> toDelete = new ArrayList<>();
        List<tbplanlist> toUpdate = new ArrayList<>();
        for (tbplanlist plan : dayPlans) {
            List<String> planUserIds = parseUserIds(plan.getUserId());
            List<String> remaining = planUserIds.stream()
                    .filter(uid -> !targetUserIds.contains(uid))
                    .collect(Collectors.toList());
            boolean hasTarget = planUserIds.stream().anyMatch(targetUserIds::contains);
            if (!hasTarget) {
                continue;
            }
            if (remaining.isEmpty()) {
                toDelete.add(plan);
            } else {
                plan.setUserId(String.join(",", remaining));
                toUpdate.add(plan);
            }
        }
        if (!toUpdate.isEmpty()) {
            planRep.saveAll(toUpdate);
        }
        if (!toDelete.isEmpty()) {
            planRep.deleteAll(toDelete);
        }
        List<tbplanlist> restPlans = new ArrayList<>();
        for (String userId : targetUserIds) {
            HrmEmployee emp = employeeByUserId.get(userId);
            tbattendanceuser identity = new tbattendanceuser();
            identity.setEmpId(emp.getEmployeeId());
            identity.setUserId(userId);
            identity.setUserName(emp.getEmployeeName());
            identity.setDepId(emp.getDeptId());
            tbplanlist restPlan = buildBaseAssignmentPlan(identity, workDate);
            restPlan.setProductName("");
            restPlan.setLinkName("");
            restPlan.setShiftType(SHIFT_TYPE_REST);
            restPlan.setRestShiftType(REST_SHIFT_TYPE_REST);
            restPlan.setCustomStart(null);
            restPlan.setCustomEnd(null);
            restPlan.setCustomCrossDay(false);
            restPlan.setCustomContinuousShift(false);
            restPlan.setClassId(null);
            restPlan.setCustomShiftId(null);
            restPlans.add(restPlan);
        }
        List<ResolvedWorkPlanAssignment> assignments = buildLocalRestAssignments(restPlans);
        persistResolvedAssignments(assignments);
        return targetUserIds.size();
    }

    private void removeEmployeeFromLocalPlans(String targetUserId, List<tbplanlist> localPlans) {
        List<tbplanlist> rowsToUpdate = new ArrayList<>();
        List<tbplanlist> rowsToDelete = new ArrayList<>();

        for (tbplanlist item : localPlans) {
            List<String> userIds = parseUserIds(item.getUserId());
            if (userIds.isEmpty() || !userIds.contains(targetUserId)) {
                continue;
            }
            List<String> remainUserIds = userIds.stream()
                    .filter(id -> !targetUserId.equals(id))
                    .collect(Collectors.toList());
            if (remainUserIds.isEmpty()) {
                rowsToDelete.add(item);
            } else {
                item.setUserId(String.join(",", remainUserIds));
                rowsToUpdate.add(item);
            }
        }
        if (!rowsToUpdate.isEmpty()) {
            planRep.saveAll(rowsToUpdate);
        }
        if (!rowsToDelete.isEmpty()) {
            planRep.deleteAll(rowsToDelete);
        }
    }

    private List<tbplanlist> buildEmployeeDayAssignmentPlans(SaveWorkPlanEmployeeDayAssignmentsBO request,
                                                             tbattendanceuser attendanceUser,
                                                             Date workDate) throws Exception {
        if (isRestDayAssignmentRequest(request)) {
            tbplanlist restPlan = buildBaseAssignmentPlan(attendanceUser, workDate);
            restPlan.setProductName("");
            restPlan.setLinkName("");
            restPlan.setWorkshopName(StringUtils.trimToEmpty(request.getWorkshopName()));
            restPlan.setShiftType(SHIFT_TYPE_REST);
            String restShiftType = StringUtils.isBlank(request.getRestShiftType())
                    ? request.getDayStatus() : request.getRestShiftType();
            restPlan.setRestShiftType(normalizeRestShiftType(restShiftType));
            return Collections.singletonList(restPlan);
        }
        List<SaveWorkPlanEmployeeDayAssignmentBO> assignments = request.getAssignments();
        if (assignments == null || assignments.isEmpty()) {
            throw new Exception("工作排班至少需要一条分配");
        }
        List<tbplanlist> plans = new ArrayList<>();
        for (SaveWorkPlanEmployeeDayAssignmentBO assignment : assignments) {
            if (assignment == null) {
                continue;
            }
            plans.add(buildPlanFromEmployeeDayAssignment(assignment, attendanceUser, workDate));
        }
        if (plans.isEmpty()) {
            throw new Exception("工作排班至少需要一条分配");
        }
        return plans;
    }

    private boolean isRestDayAssignmentRequest(SaveWorkPlanEmployeeDayAssignmentsBO request) {
        String dayStatus = StringUtils.trimToEmpty(request.getDayStatus());
        String restShiftType = StringUtils.trimToEmpty(request.getRestShiftType());
        return SHIFT_TYPE_REST.equalsIgnoreCase(dayStatus)
                || REST_SHIFT_TYPE_REST.equalsIgnoreCase(dayStatus)
                || REST_SHIFT_TYPE_ADJUST.equalsIgnoreCase(dayStatus)
                || "休息".equals(dayStatus)
                || "休假".equals(dayStatus)
                || "调休".equals(dayStatus)
                || StringUtils.isNotBlank(restShiftType);
    }

    private tbplanlist buildPlanFromEmployeeDayAssignment(SaveWorkPlanEmployeeDayAssignmentBO assignment,
                                                          tbattendanceuser attendanceUser,
                                                          Date workDate) throws Exception {
        tbplanlist plan = buildBaseAssignmentPlan(attendanceUser, workDate);
        plan.setProductName(StringUtils.trimToEmpty(assignment.getProductName()));
        plan.setLinkName(StringUtils.trimToEmpty(assignment.getPositionName()));
        plan.setWorkshopName(StringUtils.trimToEmpty(assignment.getWorkshopName()));
        String normalizedShiftType = normalizeShiftTypeValue(assignment.getShiftType());

        plan.setShiftType(normalizedShiftType);
        if (SHIFT_TYPE_REST.equals(normalizedShiftType)) {
            plan.setProductName("");
            plan.setLinkName("");
            plan.setRestShiftType(normalizeRestShiftType(assignment.getRestShiftType()));
            return plan;
        }
        if (SHIFT_TYPE_CUSTOM.equals(normalizedShiftType)) {
            plan.setCustomShiftId(assignment.getCustomShiftId());
            plan.setCustomStart(assignment.getCustomStart());
            plan.setCustomEnd(assignment.getCustomEnd());
            plan.setCustomShiftPeriod(normalizeCustomShiftPeriod(assignment.getCustomShiftPeriod()));
            plan.setCustomContinuousShift(normalizeCustomContinuousShift(
                    assignment.getCustomContinuousShift(), assignment.getCustomShiftPeriod()));
            plan.setCustomContinuousShiftExplicit(true);
            return plan;
        }
        plan.setClassId(assignment.getClassId());
        return plan;
    }

    private tbplanlist buildBaseAssignmentPlan(tbattendanceuser attendanceUser, Date workDate) {
        tbplanlist plan = new tbplanlist();
        plan.setWorkDate(workDate);
        plan.setUserId(attendanceUser.getUserId());
        if (attendanceUser.getGroupId() != null) {
            plan.setGroupId(String.valueOf(attendanceUser.getGroupId()));
        }
        plan.setCreateTime(new Date());
        return plan;
    }

    private List<ResolvedWorkPlanAssignment> buildLocalAssignments(List<tbplanlist> sourcePlans) throws Exception {
        List<ResolvedWorkPlanAssignment> assignments = new ArrayList<>();
        if (sourcePlans == null || sourcePlans.isEmpty()) {
            return assignments;
        }
        for (int i = 0; i < sourcePlans.size(); i++) {
            tbplanlist plan = sourcePlans.get(i);
            if (isCustomShift(plan)) {
                assignments.addAll(buildLocalCustomAssignments(Collections.singletonList(plan)));
                continue;
            }
            if (isRestShift(plan)) {
                assignments.addAll(buildLocalRestAssignments(Collections.singletonList(plan)));
                continue;
            }
            validatePlanForSubmit(plan);
            List<String> userIds = parseUserIds(plan.getUserId());
            for (String userId : userIds) {
                assignments.add(buildResolvedAssignment(plan, i, userId,
                        StringUtils.trimToEmpty(plan.getGroupId()), plan.getClassId()));
            }
        }
        return assignments;
    }

    private List<tbplanlist> persistReplacementAssignments(List<ResolvedWorkPlanAssignment> assignments) {
        List<PersistPlanRow> persistRows = buildPersistPlanRows(assignments);
        if (persistRows.isEmpty()) {
            return Collections.emptyList();
        }
        List<tbplanlist> saves = new ArrayList<>();
        for (PersistPlanRow persistRow : persistRows) {
            tbplanlist row = buildInsertPlan(persistRow.row);
            row.setId(null);
            if (row.getCreateTime() == null) {
                row.setCreateTime(new Date());
            }
            saves.add(row);
        }
        List<tbplanlist> savedPlans = planRep.saveAll(saves);
        for (int i = 0; i < savedPlans.size() && i < persistRows.size(); i++) {
            persistCustomPlanMeta(savedPlans.get(i), persistRows.get(i).row);
        }
        return savedPlans;
    }

    @Override
    public void fillCustomShiftMeta(List<tbplanlist> planList) throws Exception {
        if (planList == null || planList.isEmpty()) {
            return;
        }
        String companyId = resolveCompanyId();
        for (tbplanlist plan : planList) {
            if (plan == null) {
                continue;
            }
            HrmWorkPlanCustomShift customShift = findCustomShift(plan);
            if (customShift != null) {
                applyCustomShiftMeta(plan, customShift);
                continue;
            }
            HrmAttendanceShift legacyCustomShift = findLegacyLocalCustomShift(plan.getClassId());
            if (legacyCustomShift != null) {
                applyLegacyCustomShiftMeta(plan, legacyCustomShift);
                continue;
            }
            if (plan.getId() != null) {
                String cacheValue = redisRep.opsForValue().get(getCustomPlanMetaKey(companyId, plan.getId()));
                if (StringUtils.isNotBlank(cacheValue) && cacheValue.trim().startsWith("{")) {
                    try {
                        PlanCustomMeta meta = JSON.parseObject(cacheValue, PlanCustomMeta.class);
                        if (meta != null) {
                            plan.setShiftType(meta.getShiftType());
                            plan.setCustomStart(meta.getCustomStart());
                            plan.setCustomEnd(meta.getCustomEnd());
                            plan.setCustomShiftPeriod(normalizeCustomShiftPeriod(meta.getCustomShiftPeriod()));
                            plan.setCustomContinuousShift(normalizeCustomContinuousShift(meta.getCustomContinuousShift(), meta.getCustomShiftPeriod()));
                            plan.setCustomCrossDay(meta.getCustomCrossDay());
                            continue;
                        }
                    } catch (Exception ex) {
                        logger.warn("读取排班自定义班次缓存失败, planId={}", plan.getId(), ex);
                    }
                }
            }
            plan.setShiftType(normalizeShiftType(plan));
        }
    }

    protected void submitPlans(List<tbplanlist> planList) throws Exception {
        List<ResolvedWorkPlanAssignment> assignments = new ArrayList<>();
        List<tbplanlist> standardPlans = new ArrayList<>();
        List<tbplanlist> customPlans = new ArrayList<>();
        List<tbplanlist> restPlans = new ArrayList<>();
        for (tbplanlist plan : planList) {
            if (isCustomShift(plan)) {
                customPlans.add(plan);
            } else if (isRestShift(plan)) {
                restPlans.add(plan);
            } else {
                standardPlans.add(plan);
            }
        }
        if (!standardPlans.isEmpty()) {
            if (schedulePushEnabled) {
                assignments.addAll(submitRemoteSchedules(standardPlans));
            } else {
                // 弃用钉钉推送：标准班也只构建本地指派，不再调用 changeGroup/scheduleShift
                logger.info("[排班推送] 已关闭钉钉推送（hrm.dingtalk.schedule-push.enabled=false），{} 条标准班仅落本地", standardPlans.size());
                assignments.addAll(buildLocalAssignments(standardPlans));
            }
        }
        if (!customPlans.isEmpty()) {
            assignments.addAll(buildLocalCustomAssignments(customPlans));
        }
        if (!restPlans.isEmpty()) {
            assignments.addAll(buildLocalRestAssignments(restPlans));
        }
        persistPlans(assignments);
    }

    @Override
    @Transactional
    public void RemoveAll(List<Integer> IDArray) throws Exception {
        for (Integer id : IDArray) {
            planRep.deleteById(id);
        }
    }

    @Override
    public PageObject<tbplanlist> getMaxDate(Integer pageSize, Integer pageNum, String sortField, String sortOrder) {
        Map<String, Object> param = new HashMap<>();
        if (pageSize == null) pageSize = 20;
        if (pageNum == null) pageNum = 0;
        if (StringUtils.isEmpty(sortField)) sortField = "createTime";
        if (StringUtils.isEmpty(sortOrder)) sortOrder = "asc";
        param.put("sortField", sortField);
        param.put("sortOrder", sortOrder);
        param.put("pageNum", pageNum);
        param.put("pageSize", pageSize);
        List<tbplanlist> datas = workPlanMapper.getMaxDate(param);
        try {
            fillCustomShiftMeta(datas);
        } catch (Exception ex) {
            logger.warn("回填自定义班次信息失败", ex);
        }
        PageObject<tbplanlist> object = new PageObject<>();
        if (datas.size() > 0) {
            object.setList(datas);
        }
        return object;
    }

    @Override
    public PageObject<tbplanlist> loadBySelectedDate(Date selectedDate, boolean loadLast, Integer pageSize,
                                                     Integer pageNum, String sortField, String sortOrder) {
        if (selectedDate == null) {
            return getMaxDate(pageSize, pageNum, sortField, sortOrder);
        }
        if (pageSize == null || pageSize <= 0) pageSize = 20;
        if (pageNum == null || pageNum < 0) pageNum = 0;
        if (StringUtils.isEmpty(sortField)) sortField = "createTime";
        if (StringUtils.isEmpty(sortOrder)) sortOrder = "asc";

        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        Date targetDay = selectedDate;
        if (loadLast) {
            tbplanlist nearest = planRep.findTopByWorkDateLessThanOrderByWorkDateDesc(startOfDay(selectedDate));
            if (nearest == null || nearest.getWorkDate() == null) {
                return PageObject.Of(new PageImpl<>(Collections.emptyList(), pageable, 0));
            }
            targetDay = nearest.getWorkDate();
        }

        Page<tbplanlist> page = planRep.findAllByWorkDateBetween(startOfDay(targetDay), endOfDay(targetDay), pageable);
        try {
            fillCustomShiftMeta(page.getContent());
        } catch (Exception ex) {
            logger.warn("回填自定义班次信息失败", ex);
        }
        return PageObject.Of(page);
    }

    @Override
    public List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getAllGroups() throws Exception {
        String companyId = resolveCompanyId();
        return getGroups(companyId, false);
    }

    @Override
    public List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getAllGroupsForDisplay() throws Exception {
        String companyId = resolveCompanyId();
        return getDisplayGroups(companyId);
    }

    @Override
    public List<UserObject> getUsers(String companyId) throws Exception {
        return getUsers(companyId, false);
    }

    @Override
    public List<UserObject> getUsersForDisplay(String companyId) throws Exception {
        return getUsers(companyId, true);
    }

    @Override
    public List<WorkPlanCustomShiftOptionVO> queryCustomShiftOptions() throws Exception {
        List<HrmWorkPlanCustomShift> shifts = customShiftRep.findAll(Sort.by(Sort.Direction.DESC, "updateTime", "id"));
        List<WorkPlanCustomShiftOptionVO> result = new ArrayList<>();
        for (HrmWorkPlanCustomShift shift : shifts) {
            if (shift == null || StringUtils.isBlank(shift.getStart1())) {
                continue;
            }
            WorkPlanCustomShiftOptionVO option = new WorkPlanCustomShiftOptionVO();
            option.setId(shift.getId());
            option.setShiftName(shift.getShiftName());
            option.setCustomStart(shift.getStart1());
            option.setCustomEnd(shift.getEnd1());
            option.setCustomShiftPeriod(normalizeCustomShiftPeriod(shift.getShiftPeriod()));
            option.setCustomContinuousShift(normalizeCustomContinuousShift(shift.getContinuousShift() != null && shift.getContinuousShift() == 1, shift.getShiftPeriod()));
            option.setCustomCrossDay(shift.getCrossDay() != null && shift.getCrossDay() == 1);
            String displayEnd = StringUtils.isBlank(option.getCustomEnd())
                    ? "结束"
                    : (Boolean.TRUE.equals(option.getCustomCrossDay()) ? "次日" + option.getCustomEnd() : option.getCustomEnd());
            option.setDisplayName("自定义班次(" + option.getCustomStart() + "~" + displayEnd + ")");
            result.add(option);
        }
        return result;
    }

    private WorkPlanEmployeeDayShiftVO buildBaseEmployeeDayShift(Long employeeId, tbattendanceuser attendanceUser,
                                                                 Date workDate) {
        WorkPlanEmployeeDayShiftVO result = new WorkPlanEmployeeDayShiftVO();
        result.setEmployeeId(employeeId);
        result.setUserId(attendanceUser.getUserId());
        result.setGroupId(attendanceUser.getGroupId() == null ? "" : String.valueOf(attendanceUser.getGroupId()));
        result.setWorkDate(formatWorkDate(workDate));
        result.setSource(DAY_SHIFT_SOURCE_EMPTY);
        result.setCurrentShiftType(SHIFT_TYPE_EMPTY);
        result.setCurrentShiftLabel("未设置排班");
        result.setCurrentCrossDay(false);
        result.setCustomCrossDay(false);
        result.setCustomContinuousShift(false);
        return result;
    }

    private WorkPlanEmployeeDayAssignmentsVO buildEmployeeDayAssignments(Long employeeId,
                                                                         tbattendanceuser attendanceUser,
                                                                         Date workDate,
                                                                         List<tbplanlist> localPlans) throws Exception {
        WorkPlanEmployeeDayAssignmentsVO result = new WorkPlanEmployeeDayAssignmentsVO();
        result.setEmployeeId(employeeId);
        result.setUserId(attendanceUser == null ? "" : attendanceUser.getUserId());
        result.setWorkDate(formatWorkDate(workDate));
        result.setDayStatus("empty");
        result.setRestShiftType("");
        result.setWorkshopName("");
        result.setAssignments(new ArrayList<>());
        if (localPlans == null || localPlans.isEmpty()) {
            return result;
        }
        List<tbplanlist> workPlans = localPlans.stream()
                .filter(item -> item != null && !isRestShift(item))
                .collect(Collectors.toList());
        if (workPlans.isEmpty()) {
            tbplanlist restPlan = localPlans.get(0);
            result.setDayStatus(SHIFT_TYPE_REST);
            result.setRestShiftType(normalizeRestShiftType(restPlan.getRestShiftType()));
            result.setWorkshopName(resolveWorkshopDisplayName(restPlan.getWorkshopName(), restPlan.getGroupId()));
            return result;
        }
        result.setDayStatus("work");
        result.setWorkshopName(resolveWorkshopDisplayName(workPlans.get(0).getWorkshopName(), workPlans.get(0).getGroupId()));
        for (tbplanlist plan : workPlans) {
            result.getAssignments().add(buildEmployeeDayAssignment(plan));
        }
        return result;
    }

    private WorkPlanEmployeeDayAssignmentVO buildEmployeeDayAssignment(tbplanlist plan) throws Exception {
        WorkPlanEmployeeDayAssignmentVO assignment = new WorkPlanEmployeeDayAssignmentVO();
        assignment.setPlanId(plan.getId());
        assignment.setProductName(plan.getProductName());
        assignment.setPositionName(plan.getLinkName());
        assignment.setGroupId(plan.getGroupId());
        assignment.setWorkshopName(resolveWorkshopDisplayName(plan.getWorkshopName(), plan.getGroupId()));
        assignment.setClassId(plan.getClassId());
        assignment.setCustomShiftId(plan.getCustomShiftId());
        assignment.setShiftType(normalizeShiftType(plan));
        assignment.setCurrentShiftLabel(buildShiftLabel(plan));
        if (isCustomShift(plan)) {
            assignment.setCustomStart(plan.getCustomStart());
            assignment.setCustomEnd(plan.getCustomEnd());
            assignment.setCustomShiftPeriod(normalizeCustomShiftPeriod(plan.getCustomShiftPeriod()));
            assignment.setCustomCrossDay(Boolean.TRUE.equals(plan.getCustomCrossDay()));
            assignment.setCustomContinuousShift(normalizeCustomContinuousShift(
                    plan.getCustomContinuousShift(), plan.getCustomShiftPeriod()));
            return assignment;
        }
        if (isRestShift(plan)) {
            assignment.setRestShiftType(normalizeRestShiftType(plan.getRestShiftType()));
            assignment.setCustomCrossDay(false);
            assignment.setCustomContinuousShift(false);
            return assignment;
        }
        HrmAttendanceShift standardShift = findStandardShift(plan.getClassId());
        if (standardShift != null) {
            assignment.setCurrentShiftLabel(buildStandardShiftLabel(standardShift.getShiftName(),
                    standardShift.getStart1(), standardShift.getEnd1(),
                    standardShift.getStart2(), standardShift.getEnd2(),
                    standardShift.getStart3(), standardShift.getEnd3()));
            WorkPlanCustomShiftResolver.NormalizedCustomShift normalizedShift =
                    normalizeSingleSectionShift(standardShift.getStart1(), standardShift.getEnd1(),
                            standardShift.getStart2(), standardShift.getEnd2(),
                            standardShift.getStart3(), standardShift.getEnd3());
            if (normalizedShift != null) {
                assignment.setCustomStart(normalizedShift.getStartText());
                assignment.setCustomEnd(normalizedShift.getEndText());
                assignment.setCustomCrossDay(normalizedShift.getCrossDay());
            }
        }
        assignment.setCustomContinuousShift(false);
        return assignment;
    }

    private Date parseWorkDateText(String workDate) throws Exception {
        if (StringUtils.isBlank(workDate)) {
            throw new Exception("排班日期不能为空");
        }
        String text = workDate.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd"
        };
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date parsed = format.parse(text, position);
            if (parsed != null && position.getIndex() == text.length()) {
                return parsed;
            }
        }
        throw new Exception("排班日期格式错误，格式应为yyyy-MM-dd");
    }

    /**
     * 排班身份唯一解析（仅 Excel 导入路径使用）：employeeId 锚定员工档案，
     * UserID 一律取 hrm_employee.dingtalk_user_id，与矩阵展示列表（loadUsersFromLocalSnapshot）同口径；
     * 不再采信考勤用户表的 UserID，杜绝"考勤用户表过期"导致导入排班挂到孤儿 ID（界面显示为数字员工行）。
     */
    private String resolveCanonicalPlanUserId(HrmEmployee employee, List<String> errors) {
        if (employee == null || employee.getEmployeeId() == null) {
            if (errors != null) {
                errors.add("员工不能为空");
            }
            return null;
        }
        String userId = StringUtils.trimToEmpty(employee.getDingtalkUserId());
        if (StringUtils.isBlank(userId)) {
            if (errors != null) {
                errors.add("员工[" + StringUtils.trimToEmpty(employee.getEmployeeName())
                        + "]缺少钉钉用户ID，请先同步钉钉通讯录后再导入排班");
            }
            return null;
        }
        return userId;
    }

    private tbattendanceuser resolveAttendanceUserByEmployeeId(Long employeeId) throws Exception {
        // 排班域不再读写 tbattendanceuser（2026-09 决议）：身份直接锚定员工档案，
        // userId 取 hrm_employee.dingtalk_user_id（缺失即明确报错，不再用 employeeId 兜底，不再自动建档）；
        // 返回对象仅作内存身份载体（userId/userName/depId），groupId 属钉钉考勤组数据，推送弃用后排班落库不再需要
        HrmEmployee employee = employeeRep.findById(employeeId)
                .orElseThrow(() -> new Exception("未找到员工[" + employeeId + "]的档案，无法解析排班身份"));
        String userId = StringUtils.trimToEmpty(employee.getDingtalkUserId());
        if (StringUtils.isBlank(userId)) {
            throw new Exception("员工[" + StringUtils.trimToEmpty(employee.getEmployeeName())
                    + "]缺少钉钉用户ID，请先在钉钉创建该员工并完成通讯录同步，或使用员工管理的「重新映射」");
        }
        tbattendanceuser identity = new tbattendanceuser();
        identity.setEmpId(employeeId);
        identity.setUserId(userId);
        identity.setUserName(employee.getEmployeeName());
        identity.setDepId(employee.getDeptId());
        return identity;
    }

    private List<tbplanlist> findEmployeeDayPlans(String userId, Date workDate) {
        if (StringUtils.isBlank(userId) || workDate == null) {
            return Collections.emptyList();
        }
        List<tbplanlist> rows = planRep.findAllByWorkDateBetweenOrderByIdDesc(startOfDay(workDate), endOfDay(workDate));
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .filter(item -> parseUserIds(item.getUserId()).contains(userId))
                .collect(Collectors.toList());
    }

    private tbplanlist pickPreferredEmployeePlan(List<tbplanlist> plans) {
        if (plans == null || plans.isEmpty()) {
            return null;
        }
        return plans.stream()
                .sorted(Comparator
                        .comparing((tbplanlist item) -> isCustomShift(item) ? 0 : 1)
                        .thenComparing(tbplanlist::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private void applyLocalPlanToDayShift(WorkPlanEmployeeDayShiftVO target, tbplanlist plan) throws Exception {
        if (target == null || plan == null) {
            return;
        }
        target.setSource(DAY_SHIFT_SOURCE_LOCAL);
        target.setPlanId(plan.getId());
        if (StringUtils.isNotBlank(plan.getGroupId())) {
            target.setGroupId(plan.getGroupId());
        }
        target.setProductName(plan.getProductName());
        target.setPositionName(plan.getLinkName());
        target.setWorkshopName(resolveWorkshopDisplayName(plan.getWorkshopName(), target.getGroupId()));
        target.setClassId(plan.getClassId());
        if (isCustomShift(plan)) {
            target.setCurrentShiftType(SHIFT_TYPE_CUSTOM);
            target.setCurrentShiftLabel(buildShiftLabel(plan));
            target.setCurrentStart(plan.getCustomStart());
            target.setCurrentEnd(plan.getCustomEnd());
            target.setCurrentCrossDay(Boolean.TRUE.equals(plan.getCustomCrossDay()));
            target.setCustomShiftId(plan.getCustomShiftId());
            target.setCustomStart(plan.getCustomStart());
            target.setCustomEnd(plan.getCustomEnd());
            target.setCustomShiftPeriod(normalizeCustomShiftPeriod(plan.getCustomShiftPeriod()));
            target.setCustomContinuousShift(normalizeCustomContinuousShift(plan.getCustomContinuousShift(), plan.getCustomShiftPeriod()));
            target.setCustomCrossDay(Boolean.TRUE.equals(plan.getCustomCrossDay()));
            return;
        }
        if (isRestShift(plan)) {
            target.setCurrentShiftType(SHIFT_TYPE_REST);
            target.setRestShiftType(normalizeRestShiftType(plan.getRestShiftType()));
            target.setCurrentShiftLabel(getRestShiftTypeLabel(plan.getRestShiftType()));
            target.setCurrentStart(null);
            target.setCurrentEnd(null);
            target.setCurrentCrossDay(false);
            target.setCustomShiftId(null);
            target.setCustomStart(null);
            target.setCustomEnd(null);
            target.setCustomShiftPeriod(null);
            target.setCustomContinuousShift(false);
            target.setCustomCrossDay(false);
            return;
        }
        HrmAttendanceShift standardShift = findStandardShift(plan.getClassId());
        if (standardShift == null) {
            target.setCurrentShiftType(SHIFT_TYPE_STANDARD);
            target.setCurrentShiftLabel(StringUtils.isBlank(plan.getClassId()) ? "标准班次" : plan.getClassId());
            return;
        }
        applyStandardShift(target, standardShift.getShiftType(), standardShift.getShiftName(), plan.getClassId(),
                standardShift.getStart1(), standardShift.getEnd1(),
                standardShift.getStart2(), standardShift.getEnd2(),
                standardShift.getStart3(), standardShift.getEnd3());
    }

    private String resolveWorkshopName(String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return "";
        }
        try {
            Long parsedGroupId = Long.parseLong(groupId);
            Optional<HrmAttendanceGroup> group = groupRep.findById(parsedGroupId);
            return group.map(HrmAttendanceGroup::getName).orElse("");
        } catch (Exception ex) {
            logger.warn("解析排班车间失败, groupId={}", groupId, ex);
            return "";
        }
    }

    private String resolveWorkshopDisplayName(String workshopName, String groupId) {
        if (StringUtils.isNotBlank(workshopName)) {
            return workshopName;
        }
        return resolveWorkshopName(groupId);
    }

    private HrmAttendanceShiftVO queryAttendanceShift(Long employeeId, Date workDate) {
        try {
            QueryAttendanceDailyDetailBO query = new QueryAttendanceDailyDetailBO();
            query.setEmployeeId(employeeId);
            query.setCurrentDate(toLocalDate(workDate));
            query.setMulti(0);
            return attendanceShiftService.getEmpHrmAttendanceShift(query);
        } catch (Exception ex) {
            logger.warn("查询员工单日排班失败, employeeId={}, workDate={}", employeeId, workDate, ex);
            return null;
        }
    }

    private void applyAttendanceShiftToDayShift(WorkPlanEmployeeDayShiftVO target, HrmAttendanceShiftVO shift) {
        if (target == null || shift == null) {
            return;
        }
        target.setSource(DAY_SHIFT_SOURCE_ATTENDANCE);
        applyStandardShift(target, shift.getShiftType(), shift.getShiftName(),
                shift.getShiftId() == null ? null : String.valueOf(shift.getShiftId()),
                shift.getStart1(), shift.getEnd1(), shift.getStart2(), shift.getEnd2(),
                shift.getStart3(), shift.getEnd3());
    }

    private void applyStandardShift(WorkPlanEmployeeDayShiftVO target, Integer shiftType, String shiftName,
                                    String classId, String start1, String end1,
                                    String start2, String end2, String start3, String end3) {
        target.setClassId(classId);
        if (shiftType != null && shiftType == 0) {
            target.setCurrentShiftType(SHIFT_TYPE_REST);
            target.setRestShiftType(REST_SHIFT_TYPE_ADJUST);
            target.setCurrentShiftLabel(getRestShiftTypeLabel(REST_SHIFT_TYPE_ADJUST));
            target.setCurrentCrossDay(false);
            return;
        }
        target.setCurrentShiftType(SHIFT_TYPE_STANDARD);
        target.setCurrentShiftLabel(buildStandardShiftLabel(shiftName, start1, end1, start2, end2, start3, end3));
        WorkPlanCustomShiftResolver.NormalizedCustomShift normalizedShift =
                normalizeSingleSectionShift(start1, end1, start2, end2, start3, end3);
        if (normalizedShift == null) {
            target.setCurrentStart(normalizeTimeText(start1));
            target.setCurrentEnd(normalizeTimeText(end1));
            target.setCurrentCrossDay(false);
            return;
        }
        target.setCurrentStart(normalizedShift.getStartText());
        target.setCurrentEnd(normalizedShift.getEndText());
        target.setCurrentCrossDay(normalizedShift.getCrossDay());
        target.setCustomStart(normalizedShift.getStartText());
        target.setCustomEnd(normalizedShift.getEndText());
        target.setCustomCrossDay(normalizedShift.getCrossDay());
    }

    private HrmAttendanceShift findStandardShift(String classId) {
        if (StringUtils.isBlank(classId)) {
            return null;
        }
        try {
            return shiftRep.findById(Long.valueOf(classId)).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private WorkPlanCustomShiftResolver.NormalizedCustomShift normalizeSingleSectionShift(String start1, String end1,
                                                                                          String start2, String end2,
                                                                                          String start3, String end3) {
        if (StringUtils.isBlank(start1) || StringUtils.isBlank(end1)) {
            return null;
        }
        if (StringUtils.isNotBlank(start2) || StringUtils.isNotBlank(end2)
                || StringUtils.isNotBlank(start3) || StringUtils.isNotBlank(end3)) {
            return null;
        }
        try {
            return customShiftResolver.normalize(normalizeTimeText(start1), normalizeTimeText(end1));
        } catch (Exception ex) {
            logger.warn("解析单段标准班次失败, start={}, end={}", start1, end1, ex);
            return null;
        }
    }

    private String buildStandardShiftLabel(String shiftName, String start1, String end1,
                                           String start2, String end2, String start3, String end3) {
        List<String> slots = new ArrayList<>();
        appendShiftSlot(slots, start1, end1);
        appendShiftSlot(slots, start2, end2);
        appendShiftSlot(slots, start3, end3);
        String slotText = String.join(" ", slots);
        if (StringUtils.isNotBlank(shiftName) && StringUtils.isNotBlank(slotText)) {
            return shiftName + "(" + slotText + ")";
        }
        if (StringUtils.isNotBlank(shiftName)) {
            return shiftName;
        }
        return StringUtils.isBlank(slotText) ? "标准班次" : slotText;
    }

    private void appendShiftSlot(List<String> slots, String start, String end) {
        String startText = normalizeTimeText(start);
        String endText = normalizeTimeText(end);
        if (StringUtils.isBlank(startText) || StringUtils.isBlank(endText)) {
            return;
        }
        slots.add(startText + "~" + endText);
    }

    private String formatWorkDate(Date workDate) {
        if (workDate == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(workDate);
    }

    private java.time.LocalDate toLocalDate(Date workDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(workDate);
        return java.time.LocalDate.of(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    private String normalizeTimeText(String value) {
        String text = StringUtils.trimToEmpty(value);
        if (text.length() >= 5 && text.charAt(2) == ':') {
            return text.substring(0, 5);
        }
        return text;
    }

    private List<UserObject> getUsers(String companyId, boolean displayCache) throws Exception {
        String key = displayCache ? getDisplayUserKey(companyId) : getUserKey(companyId);
        if (Boolean.TRUE.equals(redisRep.hasKey(key))) {
            String cached = redisRep.opsForValue().get(key);
            if (StringUtils.isNotBlank(cached)) {
                return JSON.parseArray(cached, UserObject.class);
            }
        }
        return refreshUsersCache(companyId, displayCache);
    }

    private List<UserObject> refreshUsersCache(String companyId, boolean displayCache) throws Exception {
        String key = displayCache ? getDisplayUserKey(companyId) : getUserKey(companyId);
        List<UserObject> result = displayCache ? loadUsersFromLocalSnapshot() : loadUsersFromDingTalk(companyId, false);
        redisRep.opsForValue().set(key, JSON.toJSONString(result),
                displayCache ? DISPLAY_CACHE_TTL_MINUTES : SUBMIT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return result;
    }

    private List<UserObject> loadUsersFromLocalSnapshot() {
        List<HrmEmployee> employees = employeeRep.findAllByIsDelAndEntryStatusIn(
                0, Arrays.asList(1, 3, 4));
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, UserObject> deduplicated = new LinkedHashMap<>();
        for (HrmEmployee employee : employees) {
            if (employee == null || StringUtils.isBlank(employee.getEmployeeName())) {
                continue;
            }
            // 双键口径：优先 dingtalk_user_id，历史员工ID键兜底（读侧兼容，写侧已统一钉钉键）
            String userId = StringUtils.trimToEmpty(employee.getDingtalkUserId());
            if (StringUtils.isBlank(userId) && employee.getEmployeeId() != null) {
                userId = String.valueOf(employee.getEmployeeId());
            }
            if (StringUtils.isBlank(userId)) {
                continue;
            }
            if (deduplicated.containsKey(userId)) {
                continue;
            }
            UserObject item = new UserObject();
            item.setId(userId);
            item.setName(employee.getEmployeeName().trim());
            item.setEmployeeId(employee.getEmployeeId());
            item.setGroupId("");
            deduplicated.put(userId, item);
        }
        return new ArrayList<>(deduplicated.values());
    }

    private List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getDisplayGroups(String companyId) {
        String key = getDisplayGroupKey(companyId);
        if (Boolean.TRUE.equals(redisRep.hasKey(key))) {
            String cached = redisRep.opsForValue().get(key);
            if (StringUtils.isNotBlank(cached)) {
                return JSON.parseArray(cached, OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo.class);
            }
        }
        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> result = loadGroupsFromLocalSnapshot();
        redisRep.opsForValue().set(key, JSON.toJSONString(result), DISPLAY_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return result;
    }

    private List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> loadGroupsFromLocalSnapshot() {
        List<HrmAttendanceGroup> groups = groupRep.findAll();
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        List<HrmAttendanceShift> shifts = shiftRep.findAll();
        Map<Long, List<HrmAttendanceShift>> shiftsByGroupId = new HashMap<>();
        if (shifts != null) {
            for (HrmAttendanceShift shift : shifts) {
                if (shift == null || shift.getGroupId() == null || shift.getShiftId() == null
                        || StringUtils.isBlank(shift.getShiftName())) {
                    continue;
                }
                shiftsByGroupId.computeIfAbsent(shift.getGroupId(), key -> new ArrayList<>()).add(shift);
            }
        }
        Map<Long, Long> memberCounts = countLocalAttendanceUsersByGroup();
        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> result = new ArrayList<>();
        for (HrmAttendanceGroup group : groups) {
            if (group == null || group.getAttendanceGroupId() == null || StringUtils.isBlank(group.getName())
                    || (group.getOldSetting() != null && group.getOldSetting() == 1)) {
                continue;
            }
            Set<String> shiftIds = parseShiftSetting(group.getShiftSetting());
            if (shiftIds.isEmpty()) {
                continue;
            }
            List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> selectedClasses =
                    buildLocalDisplayClasses(group, shiftIds, shiftsByGroupId);
            if (selectedClasses.isEmpty()) {
                continue;
            }
            OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo groupVO =
                    new OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo();
            groupVO.setGroupId(group.getAttendanceGroupId());
            groupVO.setGroupName(group.getName());
            groupVO.setType("TURN");
            groupVO.setMemberCount(countLocalGroupMembers(memberCounts, group));
            groupVO.setSelectedClass(selectedClasses);
            result.add(groupVO);
        }
        return result;
    }

    private List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> buildLocalDisplayClasses(
            HrmAttendanceGroup group, Set<String> shiftIds, Map<Long, List<HrmAttendanceShift>> shiftsByGroupId) {
        List<HrmAttendanceShift> groupShifts = new ArrayList<>();
        groupShifts.addAll(shiftsByGroupId.getOrDefault(group.getAttendanceGroupId(), Collections.emptyList()));
        if (group.getOldGroupId() != null && !group.getOldGroupId().equals(group.getAttendanceGroupId())) {
            groupShifts.addAll(shiftsByGroupId.getOrDefault(group.getOldGroupId(), Collections.emptyList()));
        }
        Map<String, HrmAttendanceShift> localShiftsById = new LinkedHashMap<>();
        for (HrmAttendanceShift shift : groupShifts) {
            localShiftsById.put(String.valueOf(shift.getShiftId()), shift);
        }
        List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> selectedClasses = new ArrayList<>();
        for (String shiftId : shiftIds) {
            HrmAttendanceShift shift = localShiftsById.get(shiftId);
            if (shift == null) {
                continue;
            }
            OapiAttendanceGetsimplegroupsResponse.AtClassVo classVO =
                    new OapiAttendanceGetsimplegroupsResponse.AtClassVo();
            classVO.setClassId(shift.getShiftId());
            classVO.setClassName(shift.getShiftName());
            classVO.setSections(buildLocalDisplaySections(shift));
            selectedClasses.add(classVO);
        }
        return selectedClasses;
    }

    private Long countLocalGroupMembers(Map<Long, Long> memberCounts, HrmAttendanceGroup group) {
        if (group == null || group.getAttendanceGroupId() == null) {
            return 0L;
        }
        Long count = memberCounts.getOrDefault(group.getAttendanceGroupId(), 0L);
        if (group.getOldGroupId() != null && !group.getOldGroupId().equals(group.getAttendanceGroupId())) {
            count += memberCounts.getOrDefault(group.getOldGroupId(), 0L);
        }
        return count;
    }

    private List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> buildLocalDisplaySections(HrmAttendanceShift shift) {
        List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> sections = new ArrayList<>();
        appendLocalDisplaySection(sections, shift.getStart1(), shift.getEnd1());
        appendLocalDisplaySection(sections, shift.getStart2(), shift.getEnd2());
        appendLocalDisplaySection(sections, shift.getStart3(), shift.getEnd3());
        return sections;
    }

    private void appendLocalDisplaySection(List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> sections,
                                           String start, String end) {
        Date startTime = parseLocalShiftTime(start);
        Date endTime = parseLocalShiftTime(end);
        if (startTime == null || endTime == null) {
            return;
        }
        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO onDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        onDuty.setCheckType("OnDuty");
        onDuty.setCheckTime(startTime);
        OapiAttendanceGetsimplegroupsResponse.SetionTimeVO offDuty =
                new OapiAttendanceGetsimplegroupsResponse.SetionTimeVO();
        offDuty.setCheckType("OffDuty");
        offDuty.setCheckTime(endTime);
        OapiAttendanceGetsimplegroupsResponse.AtSectionVo section =
                new OapiAttendanceGetsimplegroupsResponse.AtSectionVo();
        section.setTimes(java.util.Arrays.asList(onDuty, offDuty));
        sections.add(section);
    }

    private Date parseLocalShiftTime(String value) {
        String text = normalizeTimeText(value);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return new SimpleDateFormat("HH:mm").parse(text);
        } catch (Exception ex) {
            logger.warn("解析本地班次时间失败, value={}", value, ex);
            return null;
        }
    }

    private Set<String> parseShiftSetting(String shiftSetting) {
        if (StringUtils.isBlank(shiftSetting)) {
            return Collections.emptySet();
        }
        Set<String> shiftIds = new LinkedHashSet<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < shiftSetting.length(); i++) {
            char value = shiftSetting.charAt(i);
            if (Character.isDigit(value)) {
                current.append(value);
                continue;
            }
            if (current.length() > 0) {
                shiftIds.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            shiftIds.add(current.toString());
        }
        return shiftIds;
    }

    private Map<Long, Long> countLocalAttendanceUsersByGroup() {
        List<tbattendanceuser> users = userRep.findAll();
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (tbattendanceuser user : users) {
            if (user == null || user.getGroupId() == null || StringUtils.isBlank(user.getUserId())) {
                continue;
            }
            counts.put(user.getGroupId(), counts.getOrDefault(user.getGroupId(), 0L) + 1L);
        }
        return counts;
    }

    private void executeSubmitTask(String taskId, LoginUserInfo context, List<tbplanlist> planList) {
        if (context != null) {
            CompanyContext.set(context);
        }
        try {
            SubmitTaskState state = SUBMIT_TASK_MAP.get(taskId);
            if (state == null) {
                return;
            }
            for (int retry = 1; retry <= MAX_RETRY_COUNT; retry++) {
                state.status = SUBMIT_STATUS_RUNNING;
                state.stage = "SUBMIT";
                state.retryCount = retry;
                state.message = "正在第" + retry + "次提交，共" + MAX_RETRY_COUNT + "次";
                state.updateTime = System.currentTimeMillis();
                try {
                    submitPlans(planList);
                    state.status = SUBMIT_STATUS_SUCCESS;
                    state.stage = "FINISH";
                    state.message = "排班提交成功";
                    state.successCount = state.totalCount;
                    state.failCount = 0;
                    state.errors = new ArrayList<>();
                    state.updateTime = System.currentTimeMillis();
                    return;
                } catch (Exception ex) {
                    String chineseReason = dingTalkErrorTranslator.translate(ex);
                    List<WorkPlanSubmitRowErrorVO> errors = buildRowErrors(planList, chineseReason);
                    if (ex instanceof WorkPlanSubmitException) {
                        List<WorkPlanSubmitRowErrorVO> detailErrors = ((WorkPlanSubmitException) ex).getErrors();
                        if (detailErrors != null && !detailErrors.isEmpty()) {
                            errors = detailErrors;
                        }
                    }
                    state.errors = errors;
                    state.failCount = errors.isEmpty() ? state.totalCount : errors.size();
                    state.successCount = 0;
                    state.updateTime = System.currentTimeMillis();
                    if (retry >= MAX_RETRY_COUNT) {
                        state.status = SUBMIT_STATUS_FAILED;
                        state.stage = "ERROR";
                        state.message = "排班提交失败：" + chineseReason;
                        return;
                    }
                    state.message = "第" + retry + "次提交失败，正在准备重试";
                    try {
                        // 指数退避：200ms 起步、封顶 30s；固定 200ms 会在钉钉限流期内 10 连击放大调用量
                        Thread.sleep(submitRetryBackoffMillis(retry));
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } finally {
            CompanyContext.clear();
        }
    }

    private List<ResolvedWorkPlanAssignment> submitRemoteSchedules(List<tbplanlist> planList) throws Exception {
        LoginUserInfo info = CompanyContext.get();
        if (info == null || StringUtils.isBlank(info.getCompanyId())) {
            throw new Exception("未获取到当前公司信息");
        }
        String companyId = info.getCompanyId();
        String adminUserId = tokener.GetAdminUser(companyId);
        String token = tokener.Refresh(companyId);
        List<UserObject> users = getUsers(companyId);
        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> groups = getAllGroups();
        boolean hasGroupChange = false;
        List<WorkPlanSubmitRowErrorVO> errors = new ArrayList<>();
        List<ResolvedWorkPlanAssignment> assignments = new ArrayList<>();

        for (int i = 0; i < planList.size(); i++) {
            tbplanlist plan = planList.get(i);
            int rowIndex = i + 1;
            validatePlanForSubmit(plan);
            List<String> userIds = parseUserIds(plan.getUserId());
            if (userIds.isEmpty()) {
                continue;
            }
            for (String userId : userIds) {
                try {
                    StandardScheduleResult result =
                            scheduleStandardPlan(plan, i, userId, users, adminUserId, token);
                    assignments.add(result.assignment);
                    if (result.hasGroupChange) {
                        hasGroupChange = true;
                    }
                } catch (Exception ex) {
                    errors.add(buildRowError(plan, rowIndex, userId, users, groups, dingTalkErrorTranslator.translate(ex)));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new WorkPlanSubmitException("存在排班失败明细", errors);
        }
        if (hasGroupChange) {
            refreshUsersCache(companyId, false);
        }
        return assignments;
    }

    List<ResolvedWorkPlanAssignment> buildLocalCustomAssignments(List<tbplanlist> planList) throws Exception {
        List<ResolvedWorkPlanAssignment> assignments = new ArrayList<>();
        if (planList == null || planList.isEmpty()) {
            return assignments;
        }
        Map<String, Boolean> employeeContinuousShiftByUserId =
                resolveEmployeeContinuousShiftByUserId(collectPlanUserIds(planList));
        Map<String, tbplanlist> resolvedCustomPlanByKey = new HashMap<>();
        for (int i = 0; i < planList.size(); i++) {
            tbplanlist plan = planList.get(i);
            validatePlanForSubmit(plan);
            List<String> userIds = parseUserIds(plan.getUserId());
            if (userIds.isEmpty()) {
                continue;
            }
            String resolvedGroupId = StringUtils.trimToEmpty(plan.getGroupId());
            for (String userId : userIds) {
                tbplanlist effectivePlan = clonePlan(plan);
                Boolean employeeContinuousShift = employeeContinuousShiftByUserId.get(userId);
                if (employeeContinuousShift != null
                        && !Boolean.TRUE.equals(effectivePlan.getCustomContinuousShiftExplicit())) {
                    effectivePlan.setCustomContinuousShift(employeeContinuousShift);
                }
                String customShiftKey = buildLocalCustomShiftCacheKey(effectivePlan);
                tbplanlist resolvedTemplate = resolvedCustomPlanByKey.get(customShiftKey);
                Long shiftId;
                if (resolvedTemplate == null) {
                    shiftId = resolveLocalCustomShiftId(effectivePlan);
                    resolvedCustomPlanByKey.put(customShiftKey, clonePlan(effectivePlan));
                } else {
                    applyResolvedCustomShift(effectivePlan, resolvedTemplate);
                    shiftId = effectivePlan.getCustomShiftId();
                }
                assignments.add(buildResolvedAssignment(effectivePlan, i, userId, resolvedGroupId, String.valueOf(shiftId)));
            }
        }
        return assignments;
    }

    private List<ResolvedWorkPlanAssignment> buildLocalRestAssignments(List<tbplanlist> planList) throws Exception {
        List<ResolvedWorkPlanAssignment> assignments = new ArrayList<>();
        if (planList == null || planList.isEmpty()) {
            return assignments;
        }
        for (int i = 0; i < planList.size(); i++) {
            tbplanlist plan = planList.get(i);
            validatePlanForSubmit(plan);
            List<String> userIds = parseUserIds(plan.getUserId());
            if (userIds.isEmpty()) {
                continue;
            }
            String resolvedGroupId = StringUtils.trimToEmpty(plan.getGroupId());
            for (String userId : userIds) {
                assignments.add(buildResolvedAssignment(plan, i, userId, resolvedGroupId, null));
            }
        }
        return assignments;
    }

    private StandardScheduleResult scheduleStandardPlan(tbplanlist plan, int sourceIndex, String userId,
                                                        List<UserObject> users, String adminUserId, String token)
            throws Exception {
        String groupId = plan.getGroupId();
        String shiftId = plan.getClassId();
        if (StringUtils.isBlank(groupId)) {
            throw new Exception("排班缺少考勤组信息");
        }
        if (StringUtils.isBlank(shiftId)) {
            throw new Exception("排班缺少班次信息");
        }
        boolean hasChange = false;
        UserObject currentUser = findUser(users, userId);
        if (currentUser != null && StringUtils.isNotBlank(currentUser.getGroupId())
                && !currentUser.getGroupId().equals(groupId)) {
            changeGroup(userId, token, adminUserId, currentUser.getGroupId(), groupId);
            hasChange = true;
        }
        scheduleShift(adminUserId, token, groupId, Long.valueOf(shiftId), plan.getWorkDate(), userId);
        StandardScheduleResult result = new StandardScheduleResult();
        result.hasGroupChange = hasChange;
        result.assignment = buildResolvedAssignment(plan, sourceIndex, userId, groupId, shiftId);
        return result;
    }

    private Long resolveLocalCustomShiftId(tbplanlist plan) throws Exception {
        WorkPlanCustomShiftResolver.NormalizedCustomShift customShift =
                customShiftResolver.normalize(plan.getCustomStart(), plan.getCustomEnd());
        String customShiftPeriod = normalizeCustomShiftPeriod(plan.getCustomShiftPeriod());
        plan.setCustomStart(customShift.getStartText());
        plan.setCustomEnd(customShift.getEndText());
        plan.setCustomShiftPeriod(customShiftPeriod);
        plan.setCustomCrossDay(customShift.getCrossDay());
        int crossDay = toCrossDayFlag(customShift.getCrossDay());
        int continuousShift = toContinuousShiftFlag(plan.getCustomContinuousShift(), customShiftPeriod);
        String queryEnd1 = StringUtils.defaultString(customShift.getEndText());
        Optional<HrmWorkPlanCustomShift> matchedShift =
                customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodAndContinuousShiftOrderByIdAsc(
                        customShift.getStartText(), queryEnd1, crossDay, customShiftPeriod, continuousShift);
        if (matchedShift == null) {
            matchedShift = Optional.empty();
        }
        if (!matchedShift.isPresent() && continuousShift == 0) {
            Optional<HrmWorkPlanCustomShift> legacyMatchedShift =
                    customShiftRep.findFirstByStart1AndEnd1AndCrossDayAndShiftPeriodOrderByIdAsc(
                            customShift.getStartText(), queryEnd1, crossDay, customShiftPeriod);
            matchedShift = legacyMatchedShift == null ? Optional.empty() : legacyMatchedShift;
        }
        if (matchedShift.isPresent() && matchedShift.get().getId() != null) {
            plan.setCustomShiftId(matchedShift.get().getId());
            plan.setClassId(null);
            plan.setCustomContinuousShift(normalizeCustomContinuousShift(plan.getCustomContinuousShift(), customShiftPeriod));
            plan.setShiftType(SHIFT_TYPE_CUSTOM);
            return matchedShift.get().getId();
        }

        HrmWorkPlanCustomShift localCustomShift = new HrmWorkPlanCustomShift();
        localCustomShift.setShiftName(LOCAL_CUSTOM_SHIFT_NAME_PREFIX + customShift.buildDisplayText());
        localCustomShift.setShiftHours(calculateShiftMinutes(customShift));
        localCustomShift.setStart1(customShift.getStartText());
        localCustomShift.setEnd1(queryEnd1);
        localCustomShift.setCrossDay(crossDay);
        localCustomShift.setShiftPeriod(customShiftPeriod);
        localCustomShift.setContinuousShift(continuousShift);
        localCustomShift.setCreateTime(new Date());
        localCustomShift.setUpdateTime(new Date());
        HrmWorkPlanCustomShift savedShift = customShiftRep.save(localCustomShift);
        if (savedShift == null || savedShift.getId() == null) {
            throw new Exception("保存本地自定义班次失败");
        }
        plan.setCustomShiftId(savedShift.getId());
        plan.setClassId(null);
        plan.setCustomContinuousShift(normalizeCustomContinuousShift(plan.getCustomContinuousShift(), customShiftPeriod));
        plan.setShiftType(SHIFT_TYPE_CUSTOM);
        return savedShift.getId();
    }

    private int calculateShiftMinutes(WorkPlanCustomShiftResolver.NormalizedCustomShift customShift) {
        if (customShift == null || StringUtils.isBlank(customShift.getEndText())) {
            return 0;
        }
        String[] startParts = customShift.getStartText().split(":");
        String[] endParts = customShift.getEndText().split(":");
        int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
        int endMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
        if (customShift.getCrossDay()) {
            endMinutes += 24 * 60;
        }
        return Math.max(endMinutes - startMinutes, 0);
    }

    private boolean isSingleSectionShift(HrmAttendanceShift shift) {
        return shift != null
                && StringUtils.isNotBlank(shift.getStart1())
                && StringUtils.isNotBlank(shift.getEnd1())
                && StringUtils.isBlank(shift.getStart2())
                && StringUtils.isBlank(shift.getEnd2())
                && StringUtils.isBlank(shift.getStart3())
                && StringUtils.isBlank(shift.getEnd3());
    }

    private boolean isShiftCrossDay(HrmAttendanceShift shift) {
        if (shift == null || StringUtils.isBlank(shift.getStart1()) || StringUtils.isBlank(shift.getEnd1())) {
            return false;
        }
        try {
            return customShiftResolver.normalize(shift.getStart1(), shift.getEnd1()).getCrossDay();
        } catch (Exception ex) {
            logger.warn("解析本地班次跨天状态失败, shiftId={}", shift.getShiftId(), ex);
            return false;
        }
    }

    private HrmWorkPlanCustomShift findCustomShift(tbplanlist plan) {
        if (plan == null || plan.getCustomShiftId() == null) {
            return null;
        }
        return customShiftRep.findById(plan.getCustomShiftId()).orElse(null);
    }

    private HrmAttendanceShift findLegacyLocalCustomShift(String classId) {
        if (StringUtils.isBlank(classId)) {
            return null;
        }
        try {
            Long shiftId = Long.valueOf(classId);
            Optional<HrmAttendanceShift> shift = shiftRep.findById(shiftId);
            if (!shift.isPresent() || !isLocalCustomShift(shift.get())) {
                return null;
            }
            return shift.get();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isLocalCustomShift(HrmAttendanceShift shift) {
        return shift != null
                && shift.getShiftId() != null
                && shift.getShiftId() < 0
                && StringUtils.startsWith(StringUtils.defaultString(shift.getShiftName()), LOCAL_CUSTOM_SHIFT_NAME_PREFIX)
                && isSingleSectionShift(shift);
    }

    private void applyCustomShiftMeta(tbplanlist plan, HrmWorkPlanCustomShift shift) {
        if (plan == null || shift == null) {
            return;
        }
        plan.setShiftType(SHIFT_TYPE_CUSTOM);
        plan.setCustomShiftId(shift.getId());
        plan.setClassId(null);
        plan.setCustomStart(StringUtils.trimToEmpty(shift.getStart1()));
        plan.setCustomEnd(StringUtils.trimToEmpty(shift.getEnd1()));
        plan.setCustomShiftPeriod(normalizeCustomShiftPeriod(shift.getShiftPeriod()));
        plan.setCustomContinuousShift(normalizeCustomContinuousShift(shift.getContinuousShift() != null && shift.getContinuousShift() == 1, shift.getShiftPeriod()));
        plan.setCustomCrossDay(shift.getCrossDay() != null && shift.getCrossDay() == 1);
    }

    private void applyLegacyCustomShiftMeta(tbplanlist plan, HrmAttendanceShift shift) {
        if (plan == null || shift == null) {
            return;
        }
        plan.setShiftType(SHIFT_TYPE_CUSTOM);
        plan.setCustomStart(StringUtils.trimToEmpty(shift.getStart1()));
        plan.setCustomEnd(StringUtils.trimToEmpty(shift.getEnd1()));
        try {
            plan.setCustomCrossDay(customShiftResolver.normalize(shift.getStart1(), shift.getEnd1()).getCrossDay());
        } catch (Exception ex) {
            plan.setCustomCrossDay(false);
        }
    }

    private void scheduleShift(String adminUserId, String token, String groupId, Long shiftId, Date workDate,
                               String userId) throws Exception {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/schedule/async");
        OapiAttendanceGroupScheduleAsyncRequest request = new OapiAttendanceGroupScheduleAsyncRequest();
        request.setOpUserId(adminUserId);
        request.setGroupId(Long.valueOf(groupId));
        OapiAttendanceGroupScheduleAsyncRequest.TopScheduleParam scheduleParam =
                new OapiAttendanceGroupScheduleAsyncRequest.TopScheduleParam();
        scheduleParam.setShiftId(shiftId);
        scheduleParam.setIsRest(shiftId == 1L);
        scheduleParam.setWorkDate(workDate.getTime());
        scheduleParam.setUserid(userId);
        request.setSchedules(Collections.singletonList(scheduleParam));
        OapiAttendanceGroupScheduleAsyncResponse response = client.execute(request, token);
        if (!response.isSuccess()) {
            String message = StringUtils.isBlank(response.getMessage()) ? response.getErrmsg() : response.getMessage();
            throw new Exception(message);
        }
    }

    private void persistPlans(List<ResolvedWorkPlanAssignment> assignments) throws Exception {
        List<PersistPlanRow> persistRows = buildPersistPlanRows(assignments);
        if (persistRows.isEmpty()) {
            return;
        }
        List<tbplanlist> saves = new ArrayList<>();
        List<tbplanlist> cleanupSaves = new ArrayList<>();
        List<tbplanlist> cleanupDeletes = new ArrayList<>();
        Map<Long, List<tbplanlist>> dayPlanCache = new HashMap<>();
        Set<Integer> reusedSourceIndexes = new LinkedHashSet<>();
        for (PersistPlanRow persistRow : persistRows) {
            tbplanlist plan = persistRow.row;
            if (plan == null) {
                continue;
            }
            tbplanlist sourcePlan = persistRow.sourcePlan;
            Integer sourceIndex = persistRow.sourceIndex;
            boolean reuseId = sourcePlan != null && sourcePlan.getId() != null
                    && !reusedSourceIndexes.contains(sourceIndex);
            if (reuseId) {
                plan.setId(sourcePlan.getId());
                reusedSourceIndexes.add(sourceIndex);
            }
            if (plan.getId() == null) {
                tbplanlist reusablePlan = findReusableEmployeeDayPlan(plan, dayPlanCache, cleanupSaves, cleanupDeletes);
                if (reusablePlan != null) {
                    applyPersistPlanFields(reusablePlan, plan);
                    saves.add(reusablePlan);
                } else {
                    plan.setCreateTime(new Date());
                    saves.add(buildInsertPlan(plan));
                }
                continue;
            }
            Optional<tbplanlist> existing = planRep.findById(plan.getId());
            if (existing.isPresent() && isSameWorkDate(existing.get().getWorkDate(), plan.getWorkDate())) {
                tbplanlist target = existing.get();
                applyPersistPlanFields(target, plan);
                saves.add(target);
            } else {
                tbplanlist reusablePlan = findReusableEmployeeDayPlan(plan, dayPlanCache, cleanupSaves, cleanupDeletes);
                if (reusablePlan != null) {
                    applyPersistPlanFields(reusablePlan, plan);
                    saves.add(reusablePlan);
                } else {
                    saves.add(buildInsertPlan(plan));
                }
            }
        }
        if (!cleanupSaves.isEmpty()) {
            planRep.saveAll(cleanupSaves);
        }
        if (!cleanupDeletes.isEmpty()) {
            planRep.deleteAll(cleanupDeletes);
        }
        List<tbplanlist> savedPlans = planRep.saveAll(saves);
        for (int i = 0; i < savedPlans.size() && i < persistRows.size(); i++) {
            persistCustomPlanMeta(savedPlans.get(i), persistRows.get(i).row);
        }
    }

    private tbplanlist findReusableEmployeeDayPlan(tbplanlist plan, Map<Long, List<tbplanlist>> dayPlanCache,
                                                   List<tbplanlist> cleanupSaves,
                                                   List<tbplanlist> cleanupDeletes) {
        if (plan == null || plan.getWorkDate() == null) {
            return null;
        }
        List<String> targetUserIds = parseUserIds(plan.getUserId());
        if (targetUserIds.isEmpty()) {
            return null;
        }
        Set<String> targetUserIdSet = new LinkedHashSet<>(targetUserIds);
        List<tbplanlist> existingRows = getCachedDayPlans(plan.getWorkDate(), dayPlanCache);
        tbplanlist reusablePlan = null;
        for (tbplanlist existing : existingRows) {
            if (existing == null) {
                continue;
            }
            List<String> existingUserIds = parseUserIds(existing.getUserId());
            if (existingUserIds.isEmpty()) {
                continue;
            }
            boolean hasTargetUser = existingUserIds.stream().anyMatch(targetUserIdSet::contains);
            if (!hasTargetUser) {
                continue;
            }
            if (hasSameUserSet(existingUserIds, targetUserIds)) {
                cleanupSaves.remove(existing);
                if (reusablePlan == null) {
                    reusablePlan = existing;
                } else {
                    cleanupSaves.remove(existing);
                    addUniquePlan(cleanupDeletes, existing);
                }
                continue;
            }
            List<String> remainUserIds = existingUserIds.stream()
                    .filter(userId -> !targetUserIdSet.contains(userId))
                    .collect(Collectors.toList());
            if (remainUserIds.isEmpty()) {
                cleanupSaves.remove(existing);
                addUniquePlan(cleanupDeletes, existing);
            } else if (!cleanupDeletes.contains(existing)) {
                existing.setUserId(String.join(",", remainUserIds));
                addUniquePlan(cleanupSaves, existing);
            }
        }
        return reusablePlan;
    }

    private List<tbplanlist> getCachedDayPlans(Date workDate, Map<Long, List<tbplanlist>> dayPlanCache) {
        long dayKey = startOfDay(workDate).getTime();
        List<tbplanlist> rows = dayPlanCache.get(dayKey);
        if (rows == null) {
            rows = planRep.findAllByWorkDateBetweenOrderByIdDesc(startOfDay(workDate), endOfDay(workDate));
            rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            dayPlanCache.put(dayKey, rows);
        }
        return rows;
    }

    private boolean hasSameUserSet(List<String> leftUserIds, List<String> rightUserIds) {
        return new LinkedHashSet<>(leftUserIds).equals(new LinkedHashSet<>(rightUserIds));
    }

    private void addUniquePlan(List<tbplanlist> plans, tbplanlist plan) {
        if (plan != null && !plans.contains(plan)) {
            plans.add(plan);
        }
    }

    private void applyPersistPlanFields(tbplanlist target, tbplanlist plan) {
        target.setWorkDate(plan.getWorkDate());
        target.setProductName(plan.getProductName());
        target.setLinkName(plan.getLinkName());
        target.setWorkshopName(plan.getWorkshopName());
        target.setUserId(plan.getUserId());
        target.setGroupId(StringUtils.isBlank(plan.getGroupId()) ? target.getGroupId() : plan.getGroupId());
        target.setClassId(plan.getClassId());
        target.setCustomShiftId(plan.getCustomShiftId());
        target.setCustomShiftPeriod(plan.getCustomShiftPeriod());
        target.setCustomContinuousShift(plan.getCustomContinuousShift());
        target.setRestShiftType(plan.getRestShiftType());
        target.setShiftType(plan.getShiftType());
        target.setCustomStart(plan.getCustomStart());
        target.setCustomEnd(plan.getCustomEnd());
        target.setCustomCrossDay(plan.getCustomCrossDay());
    }

    void persistResolvedAssignments(List<ResolvedWorkPlanAssignment> assignments) throws Exception {
        persistPlans(assignments);
    }

    List<tbplanlist> buildPersistRows(List<ResolvedWorkPlanAssignment> assignments) {
        return buildPersistPlanRows(assignments).stream().map(f -> f.row).collect(Collectors.toList());
    }

    private List<PersistPlanRow> buildPersistPlanRows(List<ResolvedWorkPlanAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, PersistPlanRow> grouped = new java.util.LinkedHashMap<>();
        for (ResolvedWorkPlanAssignment assignment : assignments) {
            if (assignment == null || assignment.getSourcePlan() == null || StringUtils.isBlank(assignment.getUserId())) {
                continue;
            }
            tbplanlist sourcePlan = assignment.getSourcePlan();
            String key = buildPersistGroupKey(assignment);
            PersistPlanRow persistRow = grouped.get(key);
            if (persistRow == null) {
                persistRow = new PersistPlanRow();
                persistRow.sourceIndex = assignment.getSourceIndex();
                persistRow.sourcePlan = sourcePlan;
                persistRow.row = new tbplanlist();
                persistRow.row.setProductName(sourcePlan.getProductName());
                persistRow.row.setLinkName(sourcePlan.getLinkName());
                persistRow.row.setWorkshopName(sourcePlan.getWorkshopName());
                persistRow.row.setWorkDate(sourcePlan.getWorkDate());
                persistRow.row.setGroupId(assignment.getResolvedGroupId());
                persistRow.row.setCreateTime(sourcePlan.getCreateTime());
                persistRow.row.setShiftType(normalizeShiftType(sourcePlan));
                if (isCustomShift(sourcePlan)) {
                    persistRow.row.setClassId(null);
                    persistRow.row.setCustomShiftId(parseLongValue(assignment.getResolvedShiftId()));
                    persistRow.row.setCustomStart(sourcePlan.getCustomStart());
                    persistRow.row.setCustomEnd(sourcePlan.getCustomEnd());
                    persistRow.row.setCustomShiftPeriod(normalizeCustomShiftPeriod(sourcePlan.getCustomShiftPeriod()));
                    persistRow.row.setCustomContinuousShift(normalizeCustomContinuousShift(sourcePlan.getCustomContinuousShift(), sourcePlan.getCustomShiftPeriod()));
                    persistRow.row.setCustomContinuousShiftExplicit(sourcePlan.getCustomContinuousShiftExplicit());
                    persistRow.row.setCustomCrossDay(sourcePlan.getCustomCrossDay());
                } else if (isRestShift(sourcePlan)) {
                    persistRow.row.setClassId(null);
                    persistRow.row.setCustomShiftId(null);
                    persistRow.row.setCustomStart(null);
                    persistRow.row.setCustomEnd(null);
                    persistRow.row.setCustomShiftPeriod(null);
                    persistRow.row.setCustomContinuousShift(false);
                    persistRow.row.setRestShiftType(normalizeRestShiftType(sourcePlan.getRestShiftType()));
                    persistRow.row.setCustomCrossDay(false);
                } else {
                    persistRow.row.setClassId(assignment.getResolvedShiftId());
                    persistRow.row.setCustomShiftId(null);
                    persistRow.row.setCustomStart(null);
                    persistRow.row.setCustomEnd(null);
                    persistRow.row.setCustomShiftPeriod(null);
                    persistRow.row.setCustomContinuousShift(false);
                    persistRow.row.setCustomCrossDay(false);
                }
                grouped.put(key, persistRow);
            }
            List<String> userIds = new ArrayList<>(parseUserIds(persistRow.row.getUserId()));
            if (!userIds.contains(assignment.getUserId())) {
                userIds.add(assignment.getUserId());
            }
            persistRow.row.setUserId(String.join(",", userIds));
        }
        return new ArrayList<>(grouped.values());
    }

    private String buildPersistGroupKey(ResolvedWorkPlanAssignment assignment) {
        tbplanlist sourcePlan = assignment.getSourcePlan();
        return assignment.getSourceIndex() + "|" + assignment.getResolvedGroupId() + "|" + assignment.getResolvedShiftId()
                + "|" + normalizeShiftType(sourcePlan)
                + "|" + normalizeRestShiftType(sourcePlan.getRestShiftType())
                + "|" + StringUtils.defaultString(sourcePlan.getCustomStart())
                + "|" + StringUtils.defaultString(sourcePlan.getCustomEnd())
                + "|" + normalizeCustomShiftPeriod(sourcePlan.getCustomShiftPeriod())
                + "|" + String.valueOf(normalizeCustomContinuousShift(sourcePlan.getCustomContinuousShift(), sourcePlan.getCustomShiftPeriod()))
                + "|" + String.valueOf(sourcePlan.getCustomCrossDay())
                + "|" + (sourcePlan.getWorkDate() == null ? 0L : sourcePlan.getWorkDate().getTime())
                + "|" + StringUtils.defaultString(sourcePlan.getProductName())
                + "|" + StringUtils.defaultString(sourcePlan.getLinkName())
                + "|" + StringUtils.defaultString(sourcePlan.getWorkshopName());
    }

    private void persistCustomPlanMeta(tbplanlist savedPlan, tbplanlist sourcePlan) {
        if (savedPlan == null || savedPlan.getId() == null || sourcePlan == null) {
            return;
        }
        String key = getCustomPlanMetaKey(resolveCompanyId(), savedPlan.getId());
        if (!isCustomShift(sourcePlan) || sourcePlan.getCustomShiftId() != null) {
            redisRep.delete(key);
            return;
        }
        PlanCustomMeta meta = new PlanCustomMeta();
        meta.setShiftType(SHIFT_TYPE_CUSTOM);
        meta.setCustomStart(sourcePlan.getCustomStart());
        meta.setCustomEnd(sourcePlan.getCustomEnd());
        meta.setCustomShiftPeriod(normalizeCustomShiftPeriod(sourcePlan.getCustomShiftPeriod()));
        meta.setCustomContinuousShift(normalizeCustomContinuousShift(sourcePlan.getCustomContinuousShift(), sourcePlan.getCustomShiftPeriod()));
        meta.setCustomCrossDay(sourcePlan.getCustomCrossDay());
        redisRep.opsForValue().set(key, JSON.toJSONString(meta), PLAN_META_CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    private void validatePlanForSubmit(tbplanlist plan) throws Exception {
        if (plan == null) {
            throw new Exception("排班数据不能为空");
        }
        if (StringUtils.isBlank(plan.getUserId())) {
            throw new Exception("人员不能为空");
        }
        if (plan.getWorkDate() == null) {
            throw new Exception("排班日期不能为空");
        }
        plan.setShiftType(normalizeShiftType(plan));
        if (isCustomShift(plan)) {
            WorkPlanCustomShiftResolver.NormalizedCustomShift customShift =
                    customShiftResolver.normalize(plan.getCustomStart(), plan.getCustomEnd());
            plan.setCustomStart(customShift.getStartText());
            plan.setCustomEnd(customShift.getEndText());
            plan.setCustomShiftPeriod(normalizeCustomShiftPeriod(plan.getCustomShiftPeriod()));
            plan.setCustomContinuousShift(normalizeCustomContinuousShift(plan.getCustomContinuousShift(), plan.getCustomShiftPeriod()));
            plan.setCustomCrossDay(customShift.getCrossDay());
            return;
        }
        if (isRestShift(plan)) {
            plan.setClassId(null);
            plan.setCustomShiftId(null);
            plan.setCustomStart(null);
            plan.setCustomEnd(null);
            plan.setCustomShiftPeriod(null);
            plan.setCustomContinuousShift(false);
            plan.setRestShiftType(normalizeRestShiftType(plan.getRestShiftType()));
            plan.setCustomCrossDay(false);
            return;
        }
        if (StringUtils.isBlank(plan.getClassId())) {
            throw new Exception("班次不能为空");
        }
        if (StringUtils.isBlank(plan.getGroupId())) {
            try {
                Long shiftId = Long.valueOf(plan.getClassId());
                Optional<HrmAttendanceShift> findShift = shiftRep.findById(shiftId);
                if (findShift.isPresent() && findShift.get().getGroupId() != null) {
                    plan.setGroupId(String.valueOf(findShift.get().getGroupId()));
                }
            } catch (NumberFormatException ignore) {
            }
        }
        if (StringUtils.isBlank(plan.getGroupId())) {
            String groupId = resolveGroupIdByClassIdFromGroups(plan.getClassId());
            if (StringUtils.isNotBlank(groupId)) {
                plan.setGroupId(groupId);
            }
        }
        if (StringUtils.isBlank(plan.getGroupId())) {
            throw new Exception("排班缺少考勤组信息");
        }
    }

    private String normalizeShiftType(tbplanlist plan) {
        String value = plan == null ? null : plan.getShiftType();
        if (StringUtils.isBlank(value) && plan != null && plan.getCustomShiftId() != null) {
            return SHIFT_TYPE_CUSTOM;
        }
        if (StringUtils.isBlank(value)
                && (StringUtils.isNotBlank(plan.getCustomStart()) || StringUtils.isNotBlank(plan.getCustomEnd()))) {
            return SHIFT_TYPE_CUSTOM;
        }
        if (StringUtils.isBlank(value)) {
            return SHIFT_TYPE_STANDARD;
        }
        return normalizeShiftTypeValue(value);
    }

    private String normalizeShiftTypeValue(String value) {
        if (StringUtils.isBlank(value)) {
            return SHIFT_TYPE_STANDARD;
        }
        String text = value.trim();
        String normalized = text.toLowerCase();
        if ("2".equals(normalized) || SHIFT_TYPE_CUSTOM.equals(normalized) || "自定义班次".equals(text)) {
            return SHIFT_TYPE_CUSTOM;
        }
        if ("3".equals(normalized) || SHIFT_TYPE_REST.equals(normalized)
                || "调休".equals(text) || "休息".equals(text) || "休假".equals(text)) {
            return SHIFT_TYPE_REST;
        }
        return SHIFT_TYPE_STANDARD;
    }

    private boolean isCustomShift(tbplanlist plan) {
        return SHIFT_TYPE_CUSTOM.equalsIgnoreCase(normalizeShiftType(plan));
    }

    private boolean isRestShift(tbplanlist plan) {
        return SHIFT_TYPE_REST.equalsIgnoreCase(normalizeShiftType(plan));
    }

    private String normalizeRestShiftType(String value) {
        String text = StringUtils.trimToEmpty(value);
        String normalized = text.toLowerCase();
        if (REST_SHIFT_TYPE_REST.equals(normalized)
                || "休息".equals(text) || "休假".equals(text) || "4".equals(normalized)) {
            return REST_SHIFT_TYPE_REST;
        }
        return REST_SHIFT_TYPE_ADJUST;
    }

    private String getRestShiftTypeLabel(String value) {
        return REST_SHIFT_TYPE_REST.equals(normalizeRestShiftType(value)) ? "休息" : "调休";
    }

    private String normalizeCustomShiftPeriod(String value) {
        String text = StringUtils.trimToEmpty(value).toLowerCase();
        if (CUSTOM_SHIFT_PERIOD_NIGHT.equals(text) || "夜".equals(text) || "夜班".equals(text) || "2".equals(text)) {
            return CUSTOM_SHIFT_PERIOD_NIGHT;
        }
        return CUSTOM_SHIFT_PERIOD_DAY;
    }

    private Boolean normalizeCustomContinuousShift(Boolean continuousShift, String customShiftPeriod) {
        return Boolean.TRUE.equals(continuousShift)
                && CUSTOM_SHIFT_PERIOD_DAY.equals(normalizeCustomShiftPeriod(customShiftPeriod));
    }

    private int toContinuousShiftFlag(Boolean continuousShift, String customShiftPeriod) {
        return Boolean.TRUE.equals(normalizeCustomContinuousShift(continuousShift, customShiftPeriod)) ? 1 : 0;
    }

    private String buildLocalCustomShiftCacheKey(tbplanlist plan) throws Exception {
        WorkPlanCustomShiftResolver.NormalizedCustomShift customShift =
                customShiftResolver.normalize(plan.getCustomStart(), plan.getCustomEnd());
        String customShiftPeriod = normalizeCustomShiftPeriod(plan.getCustomShiftPeriod());
        Boolean continuousShift = normalizeCustomContinuousShift(plan.getCustomContinuousShift(), customShiftPeriod);
        return customShift.getStartText()
                + "|" + StringUtils.defaultString(customShift.getEndText())
                + "|" + String.valueOf(customShift.getCrossDay())
                + "|" + customShiftPeriod
                + "|" + String.valueOf(continuousShift);
    }

    private void applyResolvedCustomShift(tbplanlist target, tbplanlist resolved) {
        target.setClassId(null);
        target.setCustomShiftId(resolved.getCustomShiftId());
        target.setCustomStart(resolved.getCustomStart());
        target.setCustomEnd(resolved.getCustomEnd());
        target.setCustomShiftPeriod(resolved.getCustomShiftPeriod());
        target.setCustomContinuousShift(resolved.getCustomContinuousShift());
        target.setCustomCrossDay(resolved.getCustomCrossDay());
        target.setShiftType(SHIFT_TYPE_CUSTOM);
    }

    private Optional<Boolean> resolveEmployeeContinuousShift(Long employeeId) {
        if (employeeId == null) {
            return Optional.empty();
        }
        Optional<HrmEmployee> employee = employeeRep.findById(employeeId);
        return employee.isPresent() ? toEmployeeContinuousShift(employee.get()) : Optional.empty();
    }

    private Map<String, Boolean> resolveEmployeeContinuousShiftByUserId(List<String> userIds) {
        Map<String, Boolean> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        // 双键口径：userId 可能是 dingtalk_user_id（新写入）或 employeeId 字符串（历史行），都映射回员工
        List<HrmEmployee> employees = employeeRep.findAll();
        if (employees == null || employees.isEmpty()) {
            return result;
        }
        Set<String> wanted = new HashSet<>(userIds);
        for (HrmEmployee employee : employees) {
            if (employee == null || employee.getEmployeeId() == null) {
                continue;
            }
            Optional<Boolean> continuousShift = toEmployeeContinuousShift(employee);
            if (!continuousShift.isPresent()) {
                continue;
            }
            String dingTalkKey = StringUtils.trimToEmpty(employee.getDingtalkUserId());
            if (!dingTalkKey.isEmpty() && wanted.contains(dingTalkKey)) {
                result.put(dingTalkKey, continuousShift.get());
            }
            String employeeKey = String.valueOf(employee.getEmployeeId());
            if (wanted.contains(employeeKey)) {
                result.put(employeeKey, continuousShift.get());
            }
        }
        return result;
    }

    private Optional<Boolean> toEmployeeContinuousShift(HrmEmployee employee) {
        if (employee == null || employee.getIsContinuousShift() == null) {
            return Optional.empty();
        }
        return Optional.of(EMPLOYEE_CONTINUOUS_SHIFT_YES == employee.getIsContinuousShift());
    }

    private List<String> collectPlanUserIds(List<tbplanlist> planList) {
        Set<String> userIds = new LinkedHashSet<>();
        if (planList == null) {
            return new ArrayList<>(userIds);
        }
        for (tbplanlist plan : planList) {
            if (plan != null) {
                userIds.addAll(parseUserIds(plan.getUserId()));
            }
        }
        return new ArrayList<>(userIds);
    }

    private List<String> parseUserIds(String userIdText) {
        if (StringUtils.isBlank(userIdText)) {
            return Collections.emptyList();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String item : userIdText.split(",")) {
            if (StringUtils.isNotBlank(item)) {
                ids.add(item.trim());
            }
        }
        return new ArrayList<>(ids);
    }

    private int countPlanUnits(List<tbplanlist> planList) {
        int total = 0;
        for (tbplanlist plan : planList) {
            int count = parseUserIds(plan.getUserId()).size();
            total += count > 0 ? count : 1;
        }
        return total;
    }

    private UserObject findUser(List<UserObject> users, String userId) {
        if (users == null || StringUtils.isBlank(userId)) {
            return null;
        }
        for (UserObject user : users) {
            if (user != null && userId.equals(user.getId())) {
                return user;
            }
        }
        return null;
    }

    private OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo findGroup(
            List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> groups, String groupId) {
        if (groups == null || StringUtils.isBlank(groupId)) {
            return null;
        }
        for (OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group : groups) {
            if (group != null && group.getGroupId() != null && groupId.equals(String.valueOf(group.getGroupId()))) {
                return group;
            }
        }
        return null;
    }

    private String resolveGroupIdByClassIdFromGroups(String classId) {
        if (StringUtils.isBlank(classId)) {
            return null;
        }
        try {
            Long targetClassId = Long.valueOf(classId);
            List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> groups = getAllGroups();
            for (OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group : groups) {
                if (group == null || group.getGroupId() == null || group.getSelectedClass() == null) {
                    continue;
                }
                for (OapiAttendanceGetsimplegroupsResponse.AtClassVo shift : group.getSelectedClass()) {
                    if (shift != null && targetClassId.equals(shift.getClassId())) {
                        return String.valueOf(group.getGroupId());
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("根据班次反查考勤组失败, classId={}", classId, ex);
        }
        return null;
    }

    private boolean changeGroup(String userId, String token, String adminUserId, String oldGroup, String newGroup)
            throws Exception {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/users/remove");
        OapiAttendanceGroupUsersRemoveRequest removeRequest = new OapiAttendanceGroupUsersRemoveRequest();
        removeRequest.setOpUserid(adminUserId);
        removeRequest.setGroupKey(oldGroup);
        removeRequest.setUserIdList(userId);
        OapiAttendanceGroupUsersRemoveResponse removeResponse = client.execute(removeRequest, token);
        if (!removeResponse.getSuccess()) {
            throw new Exception(removeResponse.getMessage());
        }
        DingTalkClient addClient = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/users/add");
        OapiAttendanceGroupUsersAddRequest addRequest = new OapiAttendanceGroupUsersAddRequest();
        addRequest.setOpUserid(adminUserId);
        addRequest.setGroupKey(newGroup);
        addRequest.setUserIdList(userId);
        OapiAttendanceGroupUsersAddResponse addResponse = addClient.execute(addRequest, token);
        if (!addResponse.getSuccess()) {
            throw new Exception(addResponse.getMessage());
        }
        String companyId = resolveCompanyId();
        clearUserCache(companyId);
        clearGroupCache(companyId, oldGroup);
        clearGroupCache(companyId, newGroup);
        return true;
    }

    private List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getGroups(String companyId) throws Exception {
        return getGroups(companyId, false);
    }

    private List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getGroups(String companyId,
                                                                                   boolean displayCache)
            throws Exception {
        String key = displayCache ? getDisplayGroupKey(companyId) : getGroupKey(companyId);
        if (Boolean.TRUE.equals(redisRep.hasKey(key))) {
            String cached = redisRep.opsForValue().get(key);
            if (StringUtils.isNotBlank(cached)) {
                return JSON.parseArray(cached, OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo.class);
            }
        }
        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> result = loadGroupsFromDingTalk(companyId);
        redisRep.opsForValue().set(key, JSON.toJSONString(result),
                displayCache ? DISPLAY_CACHE_TTL_MINUTES : SUBMIT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return result;
    }

    List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> loadGroupsFromDingTalk(String companyId)
            throws Exception {
        ArrayList<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> result = new ArrayList<>();
        String token = tokener.Refresh(companyId);
        Long offset = 0L;
        while (true) {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getsimplegroups");
            OapiAttendanceGetsimplegroupsRequest request = new OapiAttendanceGetsimplegroupsRequest();
            request.setOffset(offset);
            request.setSize(10L);
            OapiAttendanceGetsimplegroupsResponse response = client.execute(request, token);
            if (!response.isSuccess()) {
                break;
            }
            OapiAttendanceGetsimplegroupsResponse.AtGroupListForTopVo body = response.getResult();
            if (body == null || body.getGroups() == null) {
                break;
            }
            result.addAll(body.getGroups());
            if (Boolean.TRUE.equals(body.getHasMore())) {
                offset += 10L;
            } else {
                break;
            }
        }
        return result;
    }

    List<UserObject> loadUsersFromDingTalk(String companyId, boolean displayCache) throws Exception {
        String token = tokener.Refresh(companyId);
        String adminUserId = tokener.GetAdminUser(companyId);
        List<tbattendanceuser> users = userRep.findAll();
        List<UserObject> result = new ArrayList<>();
        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> groups = getGroups(companyId, displayCache);
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/memberusers/list");
        for (OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group : groups) {
            if (group == null || group.getGroupId() == null) {
                continue;
            }
            Long cursor = 0L;
            while (true) {
                OapiAttendanceGroupMemberusersListRequest request = new OapiAttendanceGroupMemberusersListRequest();
                request.setCursor(cursor);
                request.setOpUserId(adminUserId);
                request.setGroupId(group.getGroupId());
                OapiAttendanceGroupMemberusersListResponse response = client.execute(request, token);
                if (!response.isSuccess()) {
                    break;
                }
                OapiAttendanceGroupMemberusersListResponse.PageResult pageResult = response.getResult();
                if (pageResult == null || pageResult.getResult() == null) {
                    break;
                }
                for (String userId : pageResult.getResult()) {
                    Optional<tbattendanceuser> matched = users.stream()
                            .filter(item -> item != null && userId.equals(item.getUserId()))
                            .findFirst();
                    if (matched.isPresent()) {
                        UserObject item = new UserObject();
                        item.setId(userId);
                        item.setName(matched.get().getUserName());
                        item.setEmployeeId(matched.get().getEmpId());
                        item.setGroupId(String.valueOf(group.getGroupId()));
                        result.add(item);
                    }
                }
                if (Boolean.TRUE.equals(pageResult.getHasMore())) {
                    cursor = pageResult.getCursor();
                } else {
                    break;
                }
            }
        }
        return result;
    }

    private void clearGroupCache(String companyId, String groupId) {
        redisRep.delete(getGroupKey(companyId));
        redisRep.delete(getDisplayGroupKey(companyId));
        if (StringUtils.isNotBlank(groupId)) {
            redisRep.delete("ClassList_" + groupId);
        }
    }

    private void clearUserCache(String companyId) {
        redisRep.delete(getUserKey(companyId));
        redisRep.delete(getDisplayUserKey(companyId));
        redisRep.delete(getPreviousDisplayUserKey(companyId));
        redisRep.delete(getLegacyDisplayUserKey(companyId));
    }

    private String getGroupKey(String companyId) {
        return StringUtils.leftPad(companyId, 4, '0') + "_getAllGroups";
    }

    private String getDisplayGroupKey(String companyId) {
        return StringUtils.leftPad(companyId, 4, '0') + "_getAllGroups_display";
    }

    private String getUserKey(String companyId) {
        return StringUtils.leftPad(companyId, 4, '0') + "_getAllUsers";
    }

    private String getDisplayUserKey(String companyId) {
        return StringUtils.leftPad(companyId, 4, '0') + DISPLAY_USER_CACHE_KEY_SUFFIX;
    }

    private String getPreviousDisplayUserKey(String companyId) {
        return StringUtils.leftPad(companyId, 4, '0') + "_getAllUsers_display_v2";
    }

    private String getLegacyDisplayUserKey(String companyId) {
        return StringUtils.leftPad(companyId, 4, '0') + "_getAllUsers_display";
    }

    private String getCustomPlanMetaKey(String companyId, Integer planId) {
        return "workplan:custom-plan-meta:" + companyId + ":" + planId;
    }

    private String resolveCompanyId() {
        LoginUserInfo info = CompanyContext.get();
        if (info == null || StringUtils.isBlank(info.getCompanyId())) {
            return "default";
        }
        return info.getCompanyId();
    }

    private WorkPlanSubmitProgressVO toProgressVO(SubmitTaskState state) {
        WorkPlanSubmitProgressVO vo = new WorkPlanSubmitProgressVO();
        vo.setTaskId(state.taskId);
        vo.setStatus(state.status);
        vo.setStage(state.stage);
        vo.setMessage(state.message);
        vo.setRetryCount(state.retryCount);
        vo.setMaxRetryCount(state.maxRetryCount);
        vo.setTotalCount(state.totalCount);
        vo.setSuccessCount(state.successCount);
        vo.setFailCount(state.failCount);
        boolean done = SUBMIT_STATUS_SUCCESS.equals(state.status) || SUBMIT_STATUS_FAILED.equals(state.status);
        vo.setDone(done);
        vo.setSuccess(SUBMIT_STATUS_SUCCESS.equals(state.status));
        vo.setErrors(state.errors == null ? new ArrayList<>() : new ArrayList<>(state.errors));
        return vo;
    }

    private void clearExpiredSubmitProgress() {
        long now = System.currentTimeMillis();
        SUBMIT_TASK_MAP.entrySet().removeIf(entry -> {
            SubmitTaskState state = entry.getValue();
            return state == null || now - state.updateTime > SUBMIT_PROGRESS_TTL_MS;
        });
    }

    private tbplanlist buildInsertPlan(tbplanlist source) {
        tbplanlist target = new tbplanlist();
        target.setProductName(source.getProductName());
        target.setLinkName(source.getLinkName());
        target.setWorkshopName(source.getWorkshopName());
        target.setWorkDate(source.getWorkDate());
        target.setGroupId(source.getGroupId());
        target.setClassId(source.getClassId());
        target.setCustomShiftId(source.getCustomShiftId());
        target.setCustomShiftPeriod(source.getCustomShiftPeriod());
        target.setCustomContinuousShift(source.getCustomContinuousShift());
        target.setCustomContinuousShiftExplicit(source.getCustomContinuousShiftExplicit());
        target.setRestShiftType(source.getRestShiftType());
        target.setUserId(source.getUserId());
        target.setCreateTime(source.getCreateTime() != null ? source.getCreateTime() : new Date());
        target.setShiftType(source.getShiftType());
        target.setCustomStart(source.getCustomStart());
        target.setCustomEnd(source.getCustomEnd());
        target.setCustomCrossDay(source.getCustomCrossDay());
        return target;
    }

    private ResolvedWorkPlanAssignment buildResolvedAssignment(tbplanlist sourcePlan, int sourceIndex, String userId,
                                                               String resolvedGroupId, String resolvedShiftId) {
        ResolvedWorkPlanAssignment assignment = new ResolvedWorkPlanAssignment();
        assignment.setSourceIndex(sourceIndex);
        assignment.setSourcePlan(sourcePlan);
        assignment.setUserId(userId);
        assignment.setResolvedGroupId(resolvedGroupId);
        assignment.setResolvedShiftId(resolvedShiftId);
        return assignment;
    }

    private boolean isSameWorkDate(Date left, Date right) {
        if (left == null || right == null) {
            return false;
        }
        Calendar leftCal = Calendar.getInstance();
        leftCal.setTime(left);
        Calendar rightCal = Calendar.getInstance();
        rightCal.setTime(right);
        return leftCal.get(Calendar.YEAR) == rightCal.get(Calendar.YEAR)
                && leftCal.get(Calendar.DAY_OF_YEAR) == rightCal.get(Calendar.DAY_OF_YEAR);
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date endOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private LoginUserInfo copyContext(LoginUserInfo source) {
        if (source == null) {
            return null;
        }
        LoginUserInfo target = new LoginUserInfo();
        target.setCompanyId(source.getCompanyId());
        target.setUserId(source.getUserId());
        target.setUserName(source.getUserName());
        target.setDepId(source.getDepId());
        target.setCompanyName(source.getCompanyName());
        return target;
    }

    private List<tbplanlist> clonePlans(List<tbplanlist> plans) {
        List<tbplanlist> copies = new ArrayList<>();
        for (tbplanlist plan : plans) {
            copies.add(clonePlan(plan));
        }
        return copies;
    }

    private tbplanlist clonePlan(tbplanlist plan) {
        tbplanlist copy = new tbplanlist();
        copy.setId(plan.getId());
        copy.setProductName(plan.getProductName());
        copy.setLinkName(plan.getLinkName());
        copy.setWorkshopName(plan.getWorkshopName());
        copy.setWorkDate(plan.getWorkDate());
        copy.setGroupId(plan.getGroupId());
        copy.setClassId(plan.getClassId());
        copy.setCustomShiftId(plan.getCustomShiftId());
        copy.setCustomShiftPeriod(plan.getCustomShiftPeriod());
        copy.setCustomContinuousShift(plan.getCustomContinuousShift());
        copy.setCustomContinuousShiftExplicit(plan.getCustomContinuousShiftExplicit());
        copy.setRestShiftType(plan.getRestShiftType());
        copy.setUserId(plan.getUserId());
        copy.setCreateTime(plan.getCreateTime());
        copy.setShiftType(plan.getShiftType());
        copy.setCustomStart(plan.getCustomStart());
        copy.setCustomEnd(plan.getCustomEnd());
        copy.setCustomCrossDay(plan.getCustomCrossDay());
        return copy;
    }

    private List<WorkPlanSubmitRowErrorVO> buildRowErrors(List<tbplanlist> planList, String reason) {
        List<WorkPlanSubmitRowErrorVO> errors = new ArrayList<>();
        for (int i = 0; i < planList.size(); i++) {
            errors.add(buildRowError(planList.get(i), i + 1, null, null, null, reason));
        }
        return errors;
    }

    private WorkPlanSubmitRowErrorVO buildRowError(tbplanlist plan, int rowIndex, String userId, List<UserObject> users,
                                                   List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> groups,
                                                   String reason) {
        WorkPlanSubmitRowErrorVO errorVO = new WorkPlanSubmitRowErrorVO();
        errorVO.setRowIndex(rowIndex);
        errorVO.setEmployeeId(StringUtils.isBlank(userId) ? (plan == null ? null : plan.getUserId()) : userId);
        if (users != null && StringUtils.isNotBlank(userId)) {
            UserObject user = findUser(users, userId);
            if (user != null) {
                errorVO.setEmployeeName(user.getName());
                if (StringUtils.isBlank(plan.getGroupId())) {
                    errorVO.setGroupId(user.getGroupId());
                }
            }
        }
        if (plan != null) {
            errorVO.setGroupId(StringUtils.isBlank(errorVO.getGroupId()) ? plan.getGroupId() : errorVO.getGroupId());
            if (groups != null && StringUtils.isNotBlank(errorVO.getGroupId())) {
                OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group = findGroup(groups, errorVO.getGroupId());
                if (group != null) {
                    errorVO.setGroupName(group.getGroupName());
                }
            }
            errorVO.setShiftLabel(buildShiftLabel(plan));
        }
        errorVO.setReason(reason);
        return errorVO;
    }

    private String buildShiftLabel(tbplanlist plan) {
        if (plan == null) {
            return "";
        }
        if (isCustomShift(plan)) {
            String end = Boolean.TRUE.equals(plan.getCustomCrossDay())
                    ? "次日" + plan.getCustomEnd() : plan.getCustomEnd();
            String label = "自定义班次(" + plan.getCustomStart() + "~" + end + ")";
            return Boolean.TRUE.equals(normalizeCustomContinuousShift(plan.getCustomContinuousShift(), plan.getCustomShiftPeriod()))
                    ? label + " 连班" : label;
        }
        if (isRestShift(plan)) {
            return getRestShiftTypeLabel(plan.getRestShiftType());
        }
        return StringUtils.isBlank(plan.getClassId()) ? "标准班次" : plan.getClassId();
    }

    private int toCrossDayFlag(Boolean crossDay) {
        return Boolean.TRUE.equals(crossDay) ? 1 : 0;
    }

    private Long parseLongValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return Long.valueOf(value);
    }

    /** 提交重试退避：200ms 起步指数增长、封顶 30s；单测可覆写为 0 加速 */
    protected long submitRetryBackoffMillis(int retry) {
        return Math.min(200L << Math.max(retry - 1, 0), 30_000L);
    }

    private static class SubmitTaskState {
        private String taskId;
        // 以下字段由提交线程写、HTTP 轮询线程读，需要 volatile 保证可见性
        private volatile String status;
        private volatile String stage;
        private volatile String message;
        private volatile Integer retryCount;
        private Integer maxRetryCount;
        private Integer totalCount;
        private volatile Integer successCount;
        private volatile Integer failCount;
        private volatile long updateTime;
        private List<WorkPlanSubmitRowErrorVO> errors = new ArrayList<>();
    }

    private static class PlanCustomMeta {
        private String shiftType;
        private String customStart;
        private String customEnd;
        private String customShiftPeriod;
        private Boolean customCrossDay;
        private Boolean customContinuousShift;

        public String getShiftType() {
            return shiftType;
        }

        public void setShiftType(String shiftType) {
            this.shiftType = shiftType;
        }

        public String getCustomStart() {
            return customStart;
        }

        public void setCustomStart(String customStart) {
            this.customStart = customStart;
        }

        public String getCustomEnd() {
            return customEnd;
        }

        public void setCustomEnd(String customEnd) {
            this.customEnd = customEnd;
        }

        public String getCustomShiftPeriod() {
            return customShiftPeriod;
        }

        public void setCustomShiftPeriod(String customShiftPeriod) {
            this.customShiftPeriod = customShiftPeriod;
        }

        public Boolean getCustomCrossDay() {
            return customCrossDay;
        }

        public void setCustomCrossDay(Boolean customCrossDay) {
            this.customCrossDay = customCrossDay;
        }

        public Boolean getCustomContinuousShift() {
            return customContinuousShift;
        }

        public void setCustomContinuousShift(Boolean customContinuousShift) {
            this.customContinuousShift = customContinuousShift;
        }
    }

    private static class WorkPlanSubmitException extends Exception {
        private final List<WorkPlanSubmitRowErrorVO> errors;

        private WorkPlanSubmitException(String message, List<WorkPlanSubmitRowErrorVO> errors) {
            super(message);
            this.errors = errors;
        }

        public List<WorkPlanSubmitRowErrorVO> getErrors() {
            return errors;
        }
    }

    private static class StandardScheduleResult {
        private ResolvedWorkPlanAssignment assignment;
        private boolean hasGroupChange;
    }

    private static class PersistPlanRow {
        private Integer sourceIndex;
        private tbplanlist sourcePlan;
        private tbplanlist row;
    }
}
