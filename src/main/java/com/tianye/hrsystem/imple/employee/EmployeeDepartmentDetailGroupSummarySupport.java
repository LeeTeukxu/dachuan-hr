package com.tianye.hrsystem.imple.employee;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

final class EmployeeDepartmentDetailGroupSummarySupport {

    private static final String SHEET_NAME = "集团总表";
    private static final String[] HEADERS = {
            "", "企业人数", "月总固定薪资成本", "月总绩效薪资成本", "月总社保成本", "月总公积金成本", "总成本"
    };

    private EmployeeDepartmentDetailGroupSummarySupport() {
    }

    static byte[] buildWorkbook(String title, List<CompanySummary> companies) throws IOException {
        List<CompanySummary> rows = companies == null ? Collections.emptyList() : companies;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            SummaryStyles styles = new SummaryStyles(workbook);
            setColumnWidths(sheet);
            writeTitle(sheet, title, styles.titleStyle);
            writeHeader(sheet, styles.headerStyle);

            CompanySummary total = CompanySummary.total(rows);
            writeSummaryRow(sheet, 2, "集团总数", total, styles.bodyStyle);
            for (int index = 0; index < rows.size(); index++) {
                writeSummaryRow(sheet, index + 3, rows.get(index), styles.bodyStyle);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static void writeTitle(Sheet sheet, String title, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(35);
        Cell cell = row.createCell(0);
        cell.setCellValue(title == null ? "" : title);
        cell.setCellStyle(style);
        for (int column = 1; column < HEADERS.length; column++) {
            row.createCell(column).setCellStyle(style);
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
    }

    private static void writeHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(1);
        row.setHeightInPoints(25);
        for (int column = 0; column < HEADERS.length; column++) {
            writeCell(row, column, HEADERS[column], style);
        }
    }

    private static void writeSummaryRow(Sheet sheet, int rowIndex, String label, CompanySummary summary, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(25);
        writeCell(row, 0, label, style);
        writeMetrics(row, summary, style);
    }

    private static void writeSummaryRow(Sheet sheet, int rowIndex, CompanySummary summary, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(25);
        writeCell(row, 0, summary.companyName, style);
        writeMetrics(row, summary, style);
    }

    private static void writeMetrics(Row row, CompanySummary summary, CellStyle style) {
        writeCell(row, 1, String.valueOf(summary.employeeCount), style);
        writeCell(row, 2, moneyText(summary.fixedSalaryCost), style);
        writeCell(row, 3, moneyText(summary.performanceSalaryCost), style);
        writeCell(row, 4, moneyText(summary.corporateInsuranceCost), style);
        writeCell(row, 5, moneyText(summary.corporateProvidentFundCost), style);
        writeCell(row, 6, moneyText(summary.totalCost()), style);
    }

    private static void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void setColumnWidths(Sheet sheet) {
        int[] widths = {17, 13, 18, 18, 16, 16, 14};
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index] * 256);
        }
    }

    private static String moneyText(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).stripTrailingZeros().toPlainString();
    }

    static final class CompanySummary {
        private final String companyName;
        private final int employeeCount;
        private final BigDecimal fixedSalaryCost;
        private final BigDecimal performanceSalaryCost;
        private final BigDecimal corporateInsuranceCost;
        private final BigDecimal corporateProvidentFundCost;

        CompanySummary(String companyName,
                       int employeeCount,
                       String fixedSalaryCost,
                       String performanceSalaryCost,
                       String corporateInsuranceCost,
                       String corporateProvidentFundCost) {
            this(companyName, employeeCount, parseMoney(fixedSalaryCost), parseMoney(performanceSalaryCost),
                    parseMoney(corporateInsuranceCost), parseMoney(corporateProvidentFundCost));
        }

        CompanySummary(String companyName,
                       int employeeCount,
                       BigDecimal fixedSalaryCost,
                       BigDecimal performanceSalaryCost,
                       BigDecimal corporateInsuranceCost,
                       BigDecimal corporateProvidentFundCost) {
            this.companyName = companyName == null ? "" : companyName.trim();
            this.employeeCount = employeeCount;
            this.fixedSalaryCost = safe(fixedSalaryCost);
            this.performanceSalaryCost = safe(performanceSalaryCost);
            this.corporateInsuranceCost = safe(corporateInsuranceCost);
            this.corporateProvidentFundCost = safe(corporateProvidentFundCost);
        }

        private static CompanySummary total(List<CompanySummary> companies) {
            int employeeCount = 0;
            BigDecimal fixedSalaryCost = BigDecimal.ZERO;
            BigDecimal performanceSalaryCost = BigDecimal.ZERO;
            BigDecimal corporateInsuranceCost = BigDecimal.ZERO;
            BigDecimal corporateProvidentFundCost = BigDecimal.ZERO;
            for (CompanySummary company : companies) {
                if (company == null) {
                    continue;
                }
                employeeCount += company.employeeCount;
                fixedSalaryCost = fixedSalaryCost.add(company.fixedSalaryCost);
                performanceSalaryCost = performanceSalaryCost.add(company.performanceSalaryCost);
                corporateInsuranceCost = corporateInsuranceCost.add(company.corporateInsuranceCost);
                corporateProvidentFundCost = corporateProvidentFundCost.add(company.corporateProvidentFundCost);
            }
            return new CompanySummary("集团总数", employeeCount, fixedSalaryCost, performanceSalaryCost,
                    corporateInsuranceCost, corporateProvidentFundCost);
        }

        private BigDecimal totalCost() {
            return fixedSalaryCost.add(performanceSalaryCost)
                    .add(corporateInsuranceCost).add(corporateProvidentFundCost);
        }

        private static BigDecimal parseMoney(String value) {
            if (value == null || value.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(value.trim());
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }

        private static BigDecimal safe(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private static final class SummaryStyles {
        private final CellStyle titleStyle;
        private final CellStyle headerStyle;
        private final CellStyle bodyStyle;

        private SummaryStyles(Workbook workbook) {
            this.titleStyle = createStyle(workbook, true, 14);
            this.headerStyle = createStyle(workbook, true, 11);
            this.bodyStyle = createStyle(workbook, false, 11);
        }

        private CellStyle createStyle(Workbook workbook, boolean header, int fontSize) {
            CellStyle style = workbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            if (header) {
                style.setFillForegroundColor((short) 22);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            Font font = workbook.createFont();
            font.setFontName("宋体");
            font.setFontHeightInPoints((short) fontSize);
            font.setBold(header);
            style.setFont(font);
            return style;
        }
    }
}
