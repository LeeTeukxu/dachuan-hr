package com.tianye.hrsystem.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.*;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.*;
import com.tianye.hrsystem.entity.po.*;
import com.tianye.hrsystem.entity.vo.*;
import com.tianye.hrsystem.entity.vo.DeptEmployeeListVO;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.service.employee.IHrmEmployeeEmploymentRecordService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: EmployeeController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月12日 14:25
 **/
@RestController
@RequestMapping("/hrmEmployee")
@Api(tags = "员工管理")
public class HrmEmployeeController {
    @Autowired
    private IHrmEmployeeService employeeService;

    @Autowired
    private com.tianye.hrsystem.imple.employee.HrmEmployeeDingTalkSyncService employeeDingTalkSyncService;

    @Autowired
    private IHrmEmployeeEmploymentRecordService employmentRecordService;

    @Autowired
    private com.tianye.hrsystem.autoTask.RetirementReminderTask retirementReminderTask;

    @Autowired
    private com.tianye.hrsystem.autoTask.DingTalkUserMappingRetryTask dingTalkUserMappingRetryTask;

    Logger logger= LoggerFactory.getLogger(HrmEmployeeController.class);

//    @Autowired
//    private HrmUploadExcelService excelService;

    @PostMapping("/queryLoginEmployee")
    @ApiOperation("查询登录员工")
    public Result<EmployeeInfo> queryLoginEmployee() {
        return Result.ok(EmployeeHolder.getEmployeeInfo());
    }

    @PostMapping("/addEmployee")
    @ApiOperation("新建员工")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.SAVE)
    public Result addEmployee(@Valid @RequestBody AddEmployeeBO employeeVO) {
        List<OperationLog> operationLogList = employeeService.add(employeeVO);
        return OperationResult.ok(operationLogList);
    }

    @PostMapping("/confirmEntry")
    @ApiOperation("确认入职")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.SAVE)
    public Result confirmEntry(@RequestBody AddEmployeeFieldManageBO employeeBO) {
        OperationLog operationLog = employeeService.confirmEntry(employeeBO);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/retirementRemind")
    @ApiOperation("手动触发到龄退休提醒（遍历全部租户，本月到龄的员工发站内信）")
    public Result retirementRemind() {
        retirementReminderTask.remind();
        return Result.ok("已执行到龄退休提醒检查");
    }

    @PostMapping("/remapDingTalkUser")
    @ApiOperation("手动映射钉钉用户（员工管理「重新映射」按钮，支持批量）")
    public Result remapDingTalkUser(@org.springframework.web.bind.annotation.RequestBody java.util.List<Long> employeeIds) {
        return Result.ok(dingTalkUserMappingRetryTask.remapEmployees(employeeIds));
    }



    @PostMapping("/queryEmployeeStatusNum")
    @ApiOperation("查询每个员工状态的数量")
    public Result<Map<Integer, Long>> queryEmployeeStatusNum() {
        Map<Integer, Long> statusMap = employeeService.queryEmployeeStatusNum();
        return Result.ok(statusMap);
    }

    @PostMapping("/queryPageList")
    @ApiOperation("分页查询员工列表")
    public Result<BasePage<Map<String, Object>>> queryPageList(@RequestBody QueryEmployeePageListBO employeePageListBO) {
        BasePage<Map<String, Object>> map = employeeService.queryPageList(employeePageListBO);
        return Result.ok(map);
    }

    @PostMapping("/exportBasicInfoTemplate")
    @ApiOperation("导出员工基础信息模板")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.EXCEL_EXPORT)
    public void exportBasicInfoTemplate(@RequestBody QueryEmployeePageListBO employeePageListBO, HttpServletResponse response) throws IOException {
        employeeService.exportBasicInfoTemplate(employeePageListBO, response);
    }

    @PostMapping("/exportDepartmentDetail")
    @ApiOperation("下载部门明细")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.EXCEL_EXPORT)
    public void exportDepartmentDetail(HttpServletResponse response) throws IOException {
        employeeService.exportDepartmentDetail(response);
    }

    @PostMapping("/departmentDetailGroupSummary")
    @ApiOperation("数据看板-集团总表汇总（行政经理）")
    public Result<Map<String, Object>> departmentDetailGroupSummary() {
        return Result.ok(employeeService.departmentDetailGroupSummary());
    }

    @GetMapping("/downloadEmployeeRosterTemplate")
    @ApiOperation("下载员工花名册模版")
    public void downloadEmployeeRosterTemplate(HttpServletResponse response) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("export/employee_module.xlsx")) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "employee_module.xlsx not found");
                return;
            }
            byte[] workbookBytes = buildEmployeeRosterTemplate(inputStream);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("employee_module.xlsx", "UTF-8"));
            response.setHeader("Set-Cookie", "fileDownload=true; path=/");
            response.getOutputStream().write(workbookBytes);
            response.flushBuffer();
        }
    }

    private byte[] buildEmployeeRosterTemplate(InputStream inputStream) throws IOException {
        double originalMinInflateRatio = ZipSecureFile.getMinInflateRatio();
        ZipSecureFile.setMinInflateRatio(0.001);
        try (Workbook workbook = WorkbookFactory.create(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ensureEmployeeRosterTemplateColumns(workbook);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } finally {
            ZipSecureFile.setMinInflateRatio(originalMinInflateRatio);
        }
    }

    private void ensureEmployeeRosterTemplateColumns(Workbook workbook) {
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        CellStyle decimalStyle = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        decimalStyle.setDataFormat(dataFormat.getFormat("0.00"));
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            if (workbook.isSheetHidden(index) || workbook.isSheetVeryHidden(index)) {
                continue;
            }
            Sheet sheet = workbook.getSheetAt(index);
            Row headerRow = sheet.getRow(1);
            if (!isEmployeeRosterTemplateSheet(headerRow, formatter)) {
                continue;
            }
            Row groupRow = Optional.ofNullable(sheet.getRow(0)).orElseGet(() -> sheet.createRow(0));
            int salaryGradeColumn = findRosterTemplateHeaderColumn(headerRow, formatter, "薪资等级");
            int fixedPerformanceColumn = ensureRosterTemplateColumnAfter(sheet, groupRow, headerRow, formatter, "固定绩效", "薪酬福利", salaryGradeColumn);
            int dutySubsidyColumn = ensureRosterTemplateColumnAfter(sheet, groupRow, headerRow, formatter, "职务补助", "薪酬福利", fixedPerformanceColumn);
            int otherSubsidyColumn = ensureRosterTemplateColumnAfter(sheet, groupRow, headerRow, formatter, "其他补助", "薪酬福利", dutySubsidyColumn);
            sheet.setDefaultColumnStyle(fixedPerformanceColumn, decimalStyle);
            sheet.setDefaultColumnStyle(dutySubsidyColumn, decimalStyle);
            sheet.setDefaultColumnStyle(otherSubsidyColumn, decimalStyle);
            ensureRosterTemplateGroupMergedRegion(sheet, groupRow, formatter, "薪酬福利",
                    salaryGradeColumn >= 0 ? salaryGradeColumn : fixedPerformanceColumn,
                    otherSubsidyColumn);
        }
    }

    private boolean isEmployeeRosterTemplateSheet(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            return false;
        }
        Set<String> headers = new HashSet<>();
        for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            String header = normalizeRosterTemplateText(formatter.formatCellValue(headerRow.getCell(columnIndex)));
            if (StrUtil.isNotEmpty(header)) {
                headers.add(header);
            }
        }
        return headers.contains("姓名") && headers.contains("个人电话");
    }

    private int ensureRosterTemplateColumnAfter(Sheet sheet, Row groupRow, Row headerRow, DataFormatter formatter, String header, String group, int previousColumn) {
        int existingColumn = findRosterTemplateHeaderColumn(headerRow, formatter, header);
        if (existingColumn >= 0) {
            writeRosterTemplateColumn(sheet, groupRow, headerRow, existingColumn, header, group);
            return existingColumn;
        }
        int columnIndex = previousColumn >= 0 ? previousColumn + 1 : Math.max(headerRow.getLastCellNum(), 0);
        insertBlankRosterTemplateColumn(sheet, columnIndex);
        writeRosterTemplateColumn(sheet, groupRow, headerRow, columnIndex, header, group);
        return columnIndex;
    }

    private void writeRosterTemplateColumn(Sheet sheet, Row groupRow, Row headerRow, int columnIndex, String header, String group) {
        Cell groupCell = groupRow.getCell(columnIndex);
        if (groupCell == null) {
            groupCell = groupRow.createCell(columnIndex);
            copyCellStyle(groupRow.getCell(Math.max(columnIndex - 1, 0)), groupCell);
        }
        groupCell.setCellValue(group);
        Cell headerCell = headerRow.getCell(columnIndex);
        if (headerCell == null) {
            headerCell = headerRow.createCell(columnIndex);
            copyCellStyle(headerRow.getCell(Math.max(columnIndex - 1, 0)), headerCell);
        }
        headerCell.setCellValue(header);
        if (sheet.getColumnWidth(columnIndex) <= sheet.getDefaultColumnWidth() * 256) {
            sheet.setColumnWidth(columnIndex, 14 * 256);
        }
    }

    private void insertBlankRosterTemplateColumn(Sheet sheet, int columnIndex) {
        shiftRosterTemplateMergedRegions(sheet, columnIndex);
        int maxColumn = columnIndex;
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && row.getLastCellNum() > maxColumn) {
                maxColumn = Math.max(maxColumn, row.getLastCellNum() - 1);
            }
        }
        for (int column = maxColumn; column >= columnIndex; column--) {
            sheet.setColumnWidth(column + 1, sheet.getColumnWidth(column));
            CellStyle columnStyle = sheet.getColumnStyle(column);
            if (columnStyle != null) {
                sheet.setDefaultColumnStyle(column + 1, columnStyle);
            }
        }
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int column = Math.max(row.getLastCellNum() - 1, columnIndex); column >= columnIndex; column--) {
                Cell source = row.getCell(column);
                Cell target = row.getCell(column + 1);
                if (source == null) {
                    if (target != null) {
                        row.removeCell(target);
                    }
                    continue;
                }
                if (target == null) {
                    target = row.createCell(column + 1);
                }
                copyRosterTemplateCell(source, target);
            }
            Cell blankCell = row.getCell(columnIndex);
            if (blankCell == null) {
                blankCell = row.createCell(columnIndex);
            }
            blankCell.setBlank();
        }
    }

    private void shiftRosterTemplateMergedRegions(Sheet sheet, int columnIndex) {
        List<CellRangeAddress> adjustedRegions = new ArrayList<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            CellRangeAddress adjusted = region.copy();
            if (adjusted.getLastColumn() < columnIndex) {
                adjustedRegions.add(adjusted);
            } else if (adjusted.getFirstColumn() >= columnIndex) {
                adjusted.setFirstColumn(adjusted.getFirstColumn() + 1);
                adjusted.setLastColumn(adjusted.getLastColumn() + 1);
                adjustedRegions.add(adjusted);
            } else {
                adjusted.setLastColumn(adjusted.getLastColumn() + 1);
                adjustedRegions.add(adjusted);
            }
        }
        for (int index = sheet.getNumMergedRegions() - 1; index >= 0; index--) {
            sheet.removeMergedRegion(index);
        }
        for (CellRangeAddress region : adjustedRegions) {
            sheet.addMergedRegion(region);
        }
    }

    private void ensureRosterTemplateGroupMergedRegion(Sheet sheet,
                                                       Row groupRow,
                                                       DataFormatter formatter,
                                                       String group,
                                                       int firstChildColumn,
                                                       int lastChildColumn) {
        int firstColumn = Math.min(firstChildColumn, lastChildColumn);
        int lastColumn = Math.max(firstChildColumn, lastChildColumn);
        String normalizedGroup = normalizeRosterTemplateText(group);
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstRow() == 0 && region.getLastRow() == 0 && region.isInRange(0, firstChildColumn)) {
                firstColumn = Math.min(firstColumn, region.getFirstColumn());
                break;
            }
        }

        List<CellRangeAddress> remainingRegions = new ArrayList<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            boolean sameRow = region.getFirstRow() == 0 && region.getLastRow() == 0;
            boolean overlapsTarget = sameRow && region.getFirstColumn() <= lastColumn && region.getLastColumn() >= firstColumn;
            if (!overlapsTarget) {
                remainingRegions.add(region);
            }
        }
        for (int index = sheet.getNumMergedRegions() - 1; index >= 0; index--) {
            sheet.removeMergedRegion(index);
        }
        for (CellRangeAddress region : remainingRegions) {
            sheet.addMergedRegion(region);
        }
        for (int columnIndex = firstColumn; columnIndex <= lastColumn; columnIndex++) {
            Cell cell = groupRow.getCell(columnIndex);
            if (cell == null) {
                cell = groupRow.createCell(columnIndex);
            }
            if (normalizedGroup.equals(normalizeRosterTemplateText(formatter.formatCellValue(cell))) || columnIndex >= firstChildColumn) {
                cell.setCellValue(group);
            }
        }
        if (firstColumn < lastColumn) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, firstColumn, lastColumn));
        }
    }

    private void copyRosterTemplateCell(Cell source, Cell target) {
        target.setCellStyle(source.getCellStyle());
        target.setCellComment(source.getCellComment());
        target.setHyperlink(source.getHyperlink());
        if (source.getCellType() == CellType.FORMULA) {
            target.setCellFormula(source.getCellFormula());
        } else if (source.getCellType() == CellType.NUMERIC) {
            target.setCellValue(source.getNumericCellValue());
        } else if (source.getCellType() == CellType.BOOLEAN) {
            target.setCellValue(source.getBooleanCellValue());
        } else if (source.getCellType() == CellType.ERROR) {
            target.setCellErrorValue(source.getErrorCellValue());
        } else if (source.getCellType() == CellType.STRING) {
            target.setCellValue(source.getStringCellValue());
        } else {
            target.setBlank();
        }
    }

    private int findRosterTemplateHeaderColumn(Row headerRow, DataFormatter formatter, String expectedHeader) {
        String normalizedExpectedHeader = normalizeRosterTemplateText(expectedHeader);
        for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            String header = normalizeRosterTemplateText(formatter.formatCellValue(headerRow.getCell(columnIndex)));
            if (normalizedExpectedHeader.equals(header)) {
                return columnIndex;
            }
        }
        return -1;
    }

    private void copyCellStyle(Cell source, Cell target) {
        if (source != null && source.getCellStyle() != null) {
            target.setCellStyle(source.getCellStyle());
        }
    }

    private String normalizeRosterTemplateText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r", "").replace("\n", "").replace(" ", "").replace("　", "").trim();
    }

    @PostMapping("/queryAllEmployeeList")
    @ApiOperation("查询所用员工(表单选择使用)")
    public Result<List<SimpleHrmEmployeeVO>> queryAllEmployeeList(
            @RequestParam(name = "employeeName", required = false) String employeeName,
            @RequestParam(name = "month", required = false) String month) {
        List<SimpleHrmEmployeeVO> list = employeeService.queryAllEmployeeList(employeeName, month);
        return Result.ok(list);
    }


    @PostMapping("/queryInspectionAllEmployeeList")
    @ApiOperation("查询考核范围可查询的所有员工(表单选择使用)")
    public Result<List<SimpleHrmEmployeeVO>> queryInspectionAllEmployeeList(@RequestParam(name = "employeeName", required = false) String employeeName) {
        List<SimpleHrmEmployeeVO> list = employeeService.queryInspectionAllEmployeeList(employeeName);
        return Result.ok(list);
    }

    @PostMapping("/queryAttendanceAllEmployeeList")
    @ApiOperation("查询考勤范围可查询的所有员工(表单选择使用)")
    public Result<List<SimpleHrmEmployeeVO>> queryAttendanceAllEmployeeList(@RequestParam(name = "employeeName", required = false) String employeeName) {
        List<SimpleHrmEmployeeVO> list = employeeService.queryAttendanceAllEmployeeList(employeeName);
        return Result.ok(list);
    }

    @PostMapping("/queryDeptEmployeeList/{deptId}")
    @ApiOperation("查询部门员工列表")
    public Result<DeptEmployeeListVO> queryDeptEmployeeList(@PathVariable("deptId") Long deptId) {
        DeptEmployeeListVO deptEmployeeListVO = employeeService.queryDeptEmployeeList(deptId);
        return Result.ok(deptEmployeeListVO);
    }

    @PostMapping("/queryInspectionDeptEmployeeList/{deptId}")
    @ApiOperation("查询部门员工列表")
    public Result<DeptEmployeeListVO> queryInspectionDeptEmployeeList(@PathVariable("deptId") Long deptId) {
        DeptEmployeeListVO deptEmployeeListVO = employeeService.queryInspectionDeptEmployeeList(deptId);
        return Result.ok(deptEmployeeListVO);
    }

    @PostMapping("/queryAttendDeptEmployeeList/{deptId}")
    @ApiOperation("查询部门员工列表(考勤打卡调用)")
    public Result<DeptEmployeeListVO> queryAttendDeptEmployeeList(@PathVariable("deptId") Long deptId) {
        DeptEmployeeListVO deptEmployeeListVO = employeeService.queryAttendDeptEmployeeList(deptId);
        return Result.ok(deptEmployeeListVO);
    }

    @PostMapping("/queryDeptEmpListByUser")
    @ApiOperation("查询部门用户列表(hrm添加员工使用)")
    public Result<Set<SimpleHrmEmployeeVO>> queryDeptEmpListByUser(@RequestBody DeptUserListByUserBO deptUserListByUserBO) {
        Set<SimpleHrmEmployeeVO> userList = employeeService.queryDeptUserListByUser(deptUserListByUserBO);
        return Result.ok(userList);
    }

    @PostMapping("/queryInEmployeeList")
    @ApiOperation("查询在职员工(表单选择使用)")
    public Result<List<SimpleHrmEmployeeVO>> queryInEmployeeList() {
        List<SimpleHrmEmployeeVO> list = employeeService.queryInEmployeeList();
        return Result.ok(list);
    }


    @PostMapping("/personalInformation/{employeeId}")
    @ApiOperation("个人基本信息")
    public Result<PersonalInformationVO> personalInformation(@PathVariable("employeeId") Long employeeId) {
        PersonalInformationVO personalInformationVO = employeeService.personalInformation(employeeId);
        return Result.ok(personalInformationVO);
    }

    @PostMapping("/queryById/{employeeId}")
    @ApiOperation("查询员工详情")
    public Result<HrmEmployee> queryById(@PathVariable("employeeId") Long employeeId) {
        HrmEmployee hrmEmployee = employeeService.queryById(employeeId);
        return Result.ok(hrmEmployee);
    }

    @PostMapping("/updateInformation")
    @ApiOperation("修改员工基本信息")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result updateInformation(@RequestBody UpdateInformationBO updateInformationBO) {
        OperationLog operationLog = employeeService.updateInformation(updateInformationBO);
        return OperationResult.ok(operationLog);
    }


    @PostMapping("/updateCommunication")
    @ApiOperation("修改通讯信息")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result updateCommunication(@RequestBody UpdateInformationBO updateInformationBO) {
        OperationLog operationLog = employeeService.updateCommunication(updateInformationBO);
        return OperationResult.ok(operationLog);
    }


    @PostMapping("/addExperience")
    @ApiOperation("添加教育经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result addOrUpdateEduExperience(@Validated @RequestBody HrmEmployeeEducationExperience educationExperience) {
        OperationLog operationLog = employeeService.addOrUpdateEduExperience(educationExperience);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/setEduExperience")
    @ApiOperation("修改教育经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result setEduExperience(@Validated @RequestBody HrmEmployeeEducationExperience educationExperience) {
        OperationLog operationLog = employeeService.addOrUpdateEduExperience(educationExperience);
        return OperationResult.ok(operationLog);
    }


    @PostMapping("/deleteEduExperience/{educationId}")
    @ApiOperation("删除教育经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result deleteEduExperience(@PathVariable("educationId") Long educationId) {
        OperationLog operationLog = employeeService.deleteEduExperience(educationId);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/addWorkExperience")
    @ApiOperation("添加工作经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result addWorkExperience(@Validated @RequestBody HrmEmployeeWorkExperience workExperience) {
        OperationLog operationLog = employeeService.addOrUpdateWorkExperience(workExperience);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/setWorkExperience")
    @ApiOperation("修改工作经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result setWorkExperience(@Validated @RequestBody HrmEmployeeWorkExperience workExperience) {
        OperationLog operationLog = employeeService.addOrUpdateWorkExperience(workExperience);
        return OperationResult.ok(operationLog);
    }


    @PostMapping("/deleteWorkExperience/{workExpId}")
    @ApiOperation("删除工作经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result deleteWorkExperience(@PathVariable("workExpId") Long workExpId) {
        OperationLog operationLog = employeeService.deleteWorkExperience(workExpId);
        return OperationResult.ok(operationLog);
    }


    @PostMapping("/addCertificate")
    @ApiOperation("添加证书")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result addCertificate(@Validated @RequestBody HrmEmployeeCertificate certificate) {
        employeeService.addOrUpdateCertificate(certificate);
        return Result.ok();
    }

    @PostMapping("/setCertificate")
    @ApiOperation("修改证书")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result setCertificate(@Validated @RequestBody HrmEmployeeCertificate certificate) {
        employeeService.addOrUpdateCertificate(certificate);
        return Result.ok();
    }

    @PostMapping("/deleteCertificate/{certificateId}")
    @ApiOperation("删除证书")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result deleteCertificate(@PathVariable("certificateId") Long certificateId) {
        employeeService.deleteCertificate(certificateId);
        return Result.ok();
    }


    @PostMapping("/addTrainingExperience")
    @ApiOperation("添加培训经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result addTrainingExperience(@Validated @RequestBody HrmEmployeeTrainingExperience trainingExperience) {
        OperationLog operationLog = employeeService.addOrUpdateTrainingExperience(trainingExperience);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/setTrainingExperience")
    @ApiOperation("修改培训经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)

    public Result setTrainingExperience(@Validated @RequestBody HrmEmployeeTrainingExperience trainingExperience) {
        OperationLog operationLog = employeeService.addOrUpdateTrainingExperience(trainingExperience);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/deleteTrainingExperience/{trainingId}")
    @ApiOperation("删除培训经历")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)

    public Result deleteTrainingExperience(@PathVariable("trainingId") Long trainingId) {
        OperationLog operationLog = employeeService.deleteTrainingExperience(trainingId);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/queryContactsAddField")
    @ApiOperation("查询联系人添加字段")
    public Result<List<HrmEmployeeField>> queryContactsAddField() {
        List<HrmEmployeeField> hrmEmployeeFieldList = employeeService.queryContactsAddField();
        return Result.ok(hrmEmployeeFieldList);
    }


    @PostMapping("/addContacts")
    @ApiOperation("添加联系人")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result addContacts(@RequestBody UpdateInformationBO updateInformationBO) {
        OperationLog operationLog = employeeService.addOrUpdateContacts(updateInformationBO);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/setContacts")
    @ApiOperation("修改联系人")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)
    public Result setContacts(@RequestBody UpdateInformationBO updateInformationBO) {
        OperationLog operationLog = employeeService.addOrUpdateContacts(updateInformationBO);
        return OperationResult.ok(operationLog);
    }


    @PostMapping("/deleteContacts/{contractsId}")
    @ApiOperation("删除联系人")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.UPDATE)

    public Result deleteContacts(@PathVariable("contractsId") Long contractsId) {
        OperationLog operationLog = employeeService.deleteContacts(contractsId);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/deleteByIds")
    @ApiOperation("删除员工")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.DELETE)
    public Result deleteByIds(@RequestBody List<Long> employeeIds) {
        List<OperationLog> operationLogList = employeeService.deleteByIds(employeeIds);
        return OperationResult.ok(operationLogList);
    }


    @PostMapping("/become")
    @ApiOperation("转正")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.BECOME)
    public Result become(@RequestBody HrmEmployeeChangeRecord hrmEmployeeChangeRecord) {
        OperationLog operationLog = employeeService.change(hrmEmployeeChangeRecord);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/changePost")
    @ApiOperation("调整部门/岗位")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.CHANGE_POST)
    public Result changePost(@RequestBody HrmEmployeeChangeRecord hrmEmployeeChangeRecord) {
        OperationLog operationLog = employeeService.change(hrmEmployeeChangeRecord);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/promotion")
    @ApiOperation("晋升/降级")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.PROMOTION)
    public Result promotion(@RequestBody HrmEmployeeChangeRecord hrmEmployeeChangeRecord) {
        OperationLog operationLog = employeeService.change(hrmEmployeeChangeRecord);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/updateInsuranceScheme")
    @ApiOperation("修改社保方案")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.INSURANCE_SCHEME)
    public Result updateInsuranceScheme(@RequestBody UpdateInsuranceSchemeBO updateInsuranceSchemeBO) {
        List<OperationLog> operationLogs = employeeService.updateInsuranceScheme(updateInsuranceSchemeBO);
        return OperationResult.ok(operationLogs);
    }


    @PostMapping("/againOnboarding")
    @ApiOperation("再入职")
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.REINSTATEMENT)
    public Result againOnboarding(@RequestBody AddEmployeeFieldManageBO employeeBO) {
        OperationLog operationLog = employeeService.againOnboarding(employeeBO);
        return OperationResult.ok(operationLog);
    }

    @PostMapping("/queryEmploymentRecords/{employeeId}")
    @ApiOperation("查询员工入离职履历")
    public Result<List<HrmEmployeeEmploymentRecord>> queryEmploymentRecords(@PathVariable("employeeId") Long employeeId) {
        return Result.ok(employmentRecordService.queryByEmployeeId(employeeId));
    }
//    @RequestMapping("/import")
//    @ResponseBody
//    public Result selectCompanyXls(MultipartFile file) throws IOException {
//        LoginUserInfo Info = CompanyContext.get();
//        try {
//            //开始解析EXCEL
//            long a1 = System.currentTimeMillis();
//            String FileName=file.getOriginalFilename();
//            String filePath =CompanyPathUtils.getTempPath(FileName+".xls");
//            FileUtils.writeByteArrayToFile(new File(filePath),file.getBytes());
//            //开始解析EXCEL
//            AllEmployeeListener   empObject = new AllEmployeeListener();
//            EasyExcel.read(filePath, EmployeeImportVO.class,empObject).headRowNumber(2).build().readAll();
//            List<EmployeeImportVO>rows =empObject.getItems();
//            long a2 = System.currentTimeMillis();
//            logger.info("解析EXCEL时间:"+(Long.toString(a2-a1)));
//            long a3=System.currentTimeMillis();
//            employeeService.ImportDatas(rows);
//            logger.info("保存数据耗时:"+Long.toString(a3-a2));
//            return Result.ok();
//
//        } catch (Exception ax) {
//            return  Result.error(500,ax.getMessage());
//        }
//    }

    @RequestMapping("/import")
    @ResponseBody
    public Result importEmployee(MultipartFile file) {
        try {
            employeeService.importEmployee(file);
        }
        catch (Exception ax) {
            logger.error("员工花名册导入失败", ax);
            return Result.error(500, ax.getMessage());
        }
        return Result.ok();
    }

    @PostMapping("/listForBatchSetting")
    @ApiOperation("批量设置-按部门查询候选员工")
    public Result<List<SimpleHrmEmployeeVO>> listForBatchSetting(@RequestBody(required = false) List<Long> deptIds) {
        return Result.ok(employeeService.listForBatchSetting(deptIds));
    }

    @PostMapping("/batchSetting/save")
    @ApiOperation("批量设置-更新员工指定字段")
    public Result<Integer> batchSettingSave(@RequestBody EmployeeBatchSettingBO batchSettingBO) {
        try {
            return Result.ok(employeeService.batchUpdateEmployeeField(
                    batchSettingBO.getFieldName(), batchSettingBO.getFieldValue(), batchSettingBO.getEmployeeIds()));
        } catch (Exception ax) {
            logger.error("员工批量设置失败", ax);
            return Result.error(500, ax.getMessage());
        }
    }

    @PostMapping("/syncDingTalkRoster")
    @ApiOperation("同步钉钉员工(dryRun=true时仅预检不落库)")
    public Result<Map<String, Object>> syncDingTalkRoster(@RequestParam(name = "dryRun", required = false, defaultValue = "false") Boolean dryRun) {
        try {
            return Result.ok(employeeDingTalkSyncService.syncRoster(Boolean.TRUE.equals(dryRun)));
        } catch (Exception ax) {
            logger.error("同步钉钉员工失败", ax);
            return Result.error(500, ax.getMessage());
        }
    }
}
