package com.tianye.hrsystem.imple;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AdministrativeAttendanceExportSupport {

    private static final String TEMPLATE_RESOURCE = "export/行政体系考勤汇总表.xlsx";
    private static final int LAST_EXPORT_COLUMN = 18;
    private static final int HEADER_MAIN_ROW = 2;
    private static final int HEADER_SUB_ROW = 3;
    private static final int DATA_START_ROW = 4;
    private static final String[] EXPORT_HEADERS = {
            "序号",
            "姓名",
            "部门",
            "月天数（天）",
            "应出勤（小时）",
            "实出勤（小时）",
            "应计出勤（小时）",
            "事假（小时）",
            "病假（小时）",
            "调休（小时）",
            "年假（小时）",
            "出差（天）",
            "旷工（天）",
            "迟到（次）",
            "早退（次）",
            "上班缺卡（次）",
            "下班缺卡（次）",
            "总缺卡（次）",
            "加班（小时）"
    };

    private AdministrativeAttendanceExportSupport() {
    }

    static byte[] buildWorkbook(YearMonth targetMonth,
                                String companyName,
                                LocalDate printDate,
                                List<AdministrativeAttendanceExportRow> rows) throws IOException {
        try (InputStream inputStream = AdministrativeAttendanceExportSupport.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("行政体系考勤导出模板不存在：" + TEMPLATE_RESOURCE);
            }
            try (Workbook workbook = WorkbookFactory.create(inputStream);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while (workbook.getNumberOfSheets() > 1) {
                    workbook.removeSheetAt(1);
                }
                Sheet sheet = workbook.getSheetAt(0);
                workbook.setSheetName(0, targetMonth.getMonthValue() + "月");
                CellStyle[] bodyStyles = captureBodyStyles(sheet);
                CellStyle headerStyle = captureStyle(sheet, HEADER_SUB_ROW, 0);
                CellStyle mainHeaderStyle = captureStyle(sheet, HEADER_MAIN_ROW, 0);
                CellStyle titleStyle = captureStyle(sheet, 0, 0);
                CellStyle infoStyle = captureStyle(sheet, 1, 0);

                clearMergedRegions(sheet);
                clearBodyRows(sheet);
                writeTopRows(sheet, targetMonth, companyName, printDate, titleStyle, infoStyle);
                writeHeaders(sheet, mainHeaderStyle, headerStyle);
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                writeDataRows(sheet, rows == null ? Collections.emptyList() : rows, bodyStyles, drawing);
                removeCellsAfterColumnS(sheet);

                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    private static void writeTopRows(Sheet sheet,
                                     YearMonth targetMonth,
                                     String companyName,
                                     LocalDate printDate,
                                     CellStyle titleStyle,
                                     CellStyle infoStyle) {
        addMergedRegion(sheet, 0, 0, 0, LAST_EXPORT_COLUMN);
        setText(sheet, 0, 0, targetMonth.getYear() + "年" + targetMonth.getMonthValue() + "月份行政后勤考勤统计表", titleStyle);
        setText(sheet, 1, 0, "编制单位:" + nullToEmpty(companyName), infoStyle);
        setText(sheet, 1, 12, "制表时间：" + printDate.getYear() + "年" + printDate.getMonthValue() + "月" + printDate.getDayOfMonth() + "日", infoStyle);
    }

    private static void writeHeaders(Sheet sheet, CellStyle mainHeaderStyle, CellStyle headerStyle) {
        addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_SUB_ROW, 0, 0);
        addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_SUB_ROW, 1, 1);
        addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_SUB_ROW, 2, 2);
        addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_SUB_ROW, 3, 3);
        addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_SUB_ROW, 4, 4);
        addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_SUB_ROW, 5, 5);
        addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_SUB_ROW, 6, 6);
        addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_MAIN_ROW, 7, 9);
        for (int columnIndex = 10; columnIndex <= LAST_EXPORT_COLUMN; columnIndex++) {
            addMergedRegion(sheet, HEADER_MAIN_ROW, HEADER_SUB_ROW, columnIndex, columnIndex);
        }

        for (int columnIndex = 0; columnIndex < EXPORT_HEADERS.length; columnIndex++) {
            if (columnIndex >= 7 && columnIndex <= 9) {
                setText(sheet, HEADER_SUB_ROW, columnIndex, shortHeader(EXPORT_HEADERS[columnIndex]), headerStyle);
            } else {
                setText(sheet, HEADER_MAIN_ROW, columnIndex, EXPORT_HEADERS[columnIndex], mainHeaderStyle);
                setText(sheet, HEADER_SUB_ROW, columnIndex, EXPORT_HEADERS[columnIndex], headerStyle);
            }
        }
        setText(sheet, HEADER_MAIN_ROW, 7, "请假（小时）", mainHeaderStyle);
    }

    private static String shortHeader(String header) {
        int index = header.indexOf('（');
        return index > 0 ? header.substring(0, index) : header;
    }

    private static void writeDataRows(Sheet sheet,
                                      List<AdministrativeAttendanceExportRow> rows,
                                      CellStyle[] bodyStyles,
                                      Drawing<?> drawing) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            AdministrativeAttendanceExportRow data = rows.get(rowIndex);
            int excelRowIndex = DATA_START_ROW + rowIndex;
            setNumber(sheet, excelRowIndex, 0, BigDecimal.valueOf(rowIndex + 1), bodyStyles[0]);
            setText(sheet, excelRowIndex, 1, data.employeeName, bodyStyles[1]);
            setText(sheet, excelRowIndex, 2, data.deptName, bodyStyles[2]);
            setNumber(sheet, excelRowIndex, 3, data.monthDays, bodyStyles[3]);
            setNumber(sheet, excelRowIndex, 4, data.expectedAttendanceHours, bodyStyles[4]);
            setNumber(sheet, excelRowIndex, 5, data.actualAttendanceHours, bodyStyles[5]);
            setNumber(sheet, excelRowIndex, 6, data.accruedAttendanceHours, bodyStyles[6]);
            setNumber(sheet, excelRowIndex, 7, data.personalLeaveHours, bodyStyles[7]);
            setNumber(sheet, excelRowIndex, 8, data.sickLeaveHours, bodyStyles[8]);
            setNumber(sheet, excelRowIndex, 9, data.compensatoryLeaveHours, bodyStyles[9]);
            setNumber(sheet, excelRowIndex, 10, data.annualLeaveHours, bodyStyles[10]);
            setNumber(sheet, excelRowIndex, 11, data.travelDays, bodyStyles[11]);
            setNumber(sheet, excelRowIndex, 12, data.absenteeismDays, bodyStyles[12]);
            setNumber(sheet, excelRowIndex, 13, data.lateCount, bodyStyles[13]);
            setNumber(sheet, excelRowIndex, 14, data.earlyCount, bodyStyles[14]);
            setNumber(sheet, excelRowIndex, 15, data.onDutyMissingCardCount, bodyStyles[15]);
            setNumber(sheet, excelRowIndex, 16, data.offDutyMissingCardCount, bodyStyles[16]);
            setNumber(sheet, excelRowIndex, 17, data.totalMissingCardCount, bodyStyles[17]);
            setNumber(sheet, excelRowIndex, 18, data.overtimeHours, bodyStyles[18]);
            for (Map.Entry<Integer, String> entry : data.columnComments.entrySet()) {
                setComment(sheet, drawing, excelRowIndex, entry.getKey(), entry.getValue());
            }
        }
    }

    private static void clearMergedRegions(Sheet sheet) {
        for (int index = sheet.getNumMergedRegions() - 1; index >= 0; index--) {
            sheet.removeMergedRegion(index);
        }
    }

    private static void clearBodyRows(Sheet sheet) {
        for (int rowIndex = sheet.getLastRowNum(); rowIndex >= DATA_START_ROW; rowIndex--) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    private static void removeCellsAfterColumnS(Sheet sheet) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || row.getLastCellNum() < 0) {
                continue;
            }
            for (int columnIndex = row.getLastCellNum() - 1; columnIndex > LAST_EXPORT_COLUMN; columnIndex--) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    row.removeCell(cell);
                }
            }
        }
    }

    private static CellStyle[] captureBodyStyles(Sheet sheet) {
        CellStyle[] styles = new CellStyle[LAST_EXPORT_COLUMN + 1];
        for (int columnIndex = 0; columnIndex <= LAST_EXPORT_COLUMN; columnIndex++) {
            styles[columnIndex] = captureStyle(sheet, DATA_START_ROW, columnIndex);
            if (styles[columnIndex] == null) {
                styles[columnIndex] = captureStyle(sheet, HEADER_SUB_ROW, columnIndex);
            }
        }
        return styles;
    }

    private static CellStyle captureStyle(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        return cell != null ? cell.getCellStyle() : null;
    }

    private static void addMergedRegion(Sheet sheet, int firstRow, int lastRow, int firstColumn, int lastColumn) {
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));
    }

    private static void setText(Sheet sheet, int rowIndex, int columnIndex, String value, CellStyle style) {
        Cell cell = cell(sheet, rowIndex, columnIndex);
        if (style != null) {
            cell.setCellStyle(style);
        }
        cell.setCellValue(nullToEmpty(value));
    }

    private static void setNumber(Sheet sheet, int rowIndex, int columnIndex, BigDecimal value, CellStyle style) {
        Cell cell = cell(sheet, rowIndex, columnIndex);
        if (style != null) {
            cell.setCellStyle(style);
        }
        cell.setCellValue(defaultDecimal(value).doubleValue());
    }

    private static Cell cell(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        return cell;
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static void setComment(Sheet sheet,
                                   Drawing<?> drawing,
                                   int rowIndex,
                                   int columnIndex,
                                   String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        Workbook workbook = sheet.getWorkbook();
        CreationHelper creationHelper = workbook.getCreationHelper();
        ClientAnchor anchor = creationHelper.createClientAnchor();
        anchor.setCol1(columnIndex);
        anchor.setCol2(columnIndex + 4);
        anchor.setRow1(rowIndex);
        anchor.setRow2(rowIndex + 6);
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(creationHelper.createRichTextString(text));
        comment.setAuthor("考勤系统");
        cell(sheet, rowIndex, columnIndex).setCellComment(comment);
    }

    /**
     * 请假类批注（事假/病假/调休/年假）
     */
    static String buildLeaveCommentText(String label, String occurredDates, BigDecimal totalHours) {
        if (occurredDates == null || occurredDates.trim().isEmpty()) {
            return label + "：本月没有" + label + "记录，合计 0 小时。"
                    + "计算过程：来自请假审批单，每笔审批的时长折算成小时后逐笔相加。";
        }
        return "发生时间：" + occurredDates + "。"
                + "计算过程：" + label + "来自请假审批单，每笔审批的时长先折算成小时"
                + "（不满1小时的按 分钟÷60 换算，按天审批的按每天8小时折算），再逐笔相加。"
                + "本月合计：" + amount(totalHours) + " 小时。";
    }

    /**
     * 出差批注（按天统计）
     */
    static String buildTravelCommentText(String occurredDates, BigDecimal totalDays) {
        if (occurredDates == null || occurredDates.trim().isEmpty()) {
            return "出差：本月没有出差记录，合计 0 天。计算过程：来自出差审批单，每笔审批时长÷8折算为天数后逐笔相加。";
        }
        return "发生时间：" + occurredDates + "。"
                + "计算过程：来自出差审批单，每笔审批的时长÷8（每天8小时）折算为天数，再逐笔相加。"
                + "本月合计：" + amount(totalDays) + " 天。";
    }

    /**
     * 旷工批注（按天统计）
     */
    static String buildAbsenteeismCommentText(String occurredDates, BigDecimal totalDays) {
        if (occurredDates == null || occurredDates.trim().isEmpty()) {
            return "旷工：本月没有旷工记录，合计 0 天。计算过程：考勤统计中当天被记为旷工的按天累加。";
        }
        return "发生时间：" + occurredDates + "。"
                + "计算过程：考勤统计中，当天未按规定出勤被记为旷工的按天累加"
                + "（迟到时间过长按旷工计算的也一并计入）。"
                + "本月合计：" + amount(totalDays) + " 天。";
    }

    /**
     * 按次统计批注（迟到/早退/上下班缺卡）
     */
    static String buildCountCommentText(String label, String occurredDates, BigDecimal totalCount, String ruleText) {
        if (occurredDates == null || occurredDates.trim().isEmpty()) {
            return label + "：本月没有" + label + "记录，合计 0 次。计算过程：" + ruleText;
        }
        return "发生时间：" + occurredDates + "。"
                + "计算过程：" + ruleText
                + "本月合计：" + amount(totalCount) + " 次。";
    }

    /**
     * 总缺卡批注：上班缺卡 + 下班缺卡
     */
    static String buildTotalMissingCardCommentText(BigDecimal onDutyCount, BigDecimal offDutyCount) {
        return "计算过程：上班缺卡次数 + 下班缺卡次数。"
                + "本月合计：" + amount(onDutyCount) + " + " + amount(offDutyCount)
                + " = " + amount(onDutyCount.add(defaultDecimal(offDutyCount))) + " 次。";
    }

    /**
     * 加班批注（按小时统计）
     */
    static String buildOvertimeCommentText(String occurredDates, BigDecimal totalHours) {
        if (occurredDates == null || occurredDates.trim().isEmpty()) {
            return "加班：本月没有加班记录，合计 0 小时。计算过程：来自加班审批单，每笔审批时长折算成小时后逐笔相加。";
        }
        return "发生时间：" + occurredDates + "。"
                + "计算过程：来自加班审批单，每笔审批的时长折算成小时（不满1小时的按 分钟÷60 换算）后逐笔相加。"
                + "本月合计：" + amount(totalHours) + " 小时。";
    }

    private static String amount(BigDecimal value) {
        return defaultDecimal(value).toPlainString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static final class AdministrativeAttendanceExportRow {
        final Long employeeId;
        final String employeeName;
        final String deptName;
        final BigDecimal monthDays;
        BigDecimal expectedAttendanceHours;
        BigDecimal actualAttendanceHours;
        BigDecimal accruedAttendanceHours;
        BigDecimal personalLeaveHours = BigDecimal.ZERO;
        BigDecimal sickLeaveHours = BigDecimal.ZERO;
        BigDecimal compensatoryLeaveHours = BigDecimal.ZERO;
        BigDecimal annualLeaveHours = BigDecimal.ZERO;
        BigDecimal travelDays = BigDecimal.ZERO;
        BigDecimal absenteeismDays = BigDecimal.ZERO;
        BigDecimal lateCount = BigDecimal.ZERO;
        BigDecimal earlyCount = BigDecimal.ZERO;
        BigDecimal onDutyMissingCardCount = BigDecimal.ZERO;
        BigDecimal offDutyMissingCardCount = BigDecimal.ZERO;
        BigDecimal totalMissingCardCount = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        /**
         * 批注文本（键 = 导出列索引），由服务层按员工明细生成
         */
        final Map<Integer, String> columnComments = new LinkedHashMap<>();

        AdministrativeAttendanceExportRow(Long employeeId,
                                          String employeeName,
                                          String deptName,
                                          BigDecimal monthDays,
                                          BigDecimal expectedAttendanceHours,
                                          BigDecimal actualAttendanceHours,
                                          BigDecimal accruedAttendanceHours) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.deptName = deptName;
            this.monthDays = monthDays;
            this.expectedAttendanceHours = expectedAttendanceHours;
            this.actualAttendanceHours = actualAttendanceHours;
            this.accruedAttendanceHours = accruedAttendanceHours;
        }
    }
}
