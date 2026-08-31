package com.tianye.hrsystem.modules.salary.support;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryExport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 薪资导出批注：只解释关键计算字段，不参与金额计算。
 */
public class SalaryExportCommentWriteHandler implements CellWriteHandler {

    private static final int DATA_START_ROW_INDEX = 4;
    private static final int FULL_ATTENDANCE_COLUMN_INDEX = 17;
    private static final int ABSENCE_SALARY_COLUMN_INDEX = 19;
    private static final int TAX_COLUMN_INDEX = 21;
    private static final int UNION_FEES_COLUMN_INDEX = 25;
    private static final Set<Integer> COMMENT_COLUMN_INDEXES = new HashSet<>(Arrays.asList(
            FULL_ATTENDANCE_COLUMN_INDEX,
            ABSENCE_SALARY_COLUMN_INDEX,
            TAX_COLUMN_INDEX,
            UNION_FEES_COLUMN_INDEX
    ));

    private final List<HrmSalaryExport> salaryExportList;
    private final Map<Sheet, Drawing<?>> drawingBySheet = new IdentityHashMap<>();

    public SalaryExportCommentWriteHandler(List<HrmSalaryExport> salaryExportList) {
        this.salaryExportList = salaryExportList;
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        Cell cell = context.getCell();
        if (cell == null || !COMMENT_COLUMN_INDEXES.contains(cell.getColumnIndex())) {
            return;
        }

        int exportIndex = cell.getRowIndex() - DATA_START_ROW_INDEX;
        if (salaryExportList == null || exportIndex < 0 || exportIndex >= salaryExportList.size()) {
            return;
        }

        HrmSalaryExport salaryExport = salaryExportList.get(exportIndex);
        if (isSubtotalOrTotalRow(salaryExport)) {
            return;
        }

        applySalaryExportComment(cell, salaryExport, drawingFor(cell.getSheet()));
    }

    public static void applySalaryExportComments(Row row, HrmSalaryExport salaryExport) {
        if (row == null || isSubtotalOrTotalRow(salaryExport)) {
            return;
        }
        Drawing<?> drawing = row.getSheet().createDrawingPatriarch();
        for (Integer columnIndex : COMMENT_COLUMN_INDEXES) {
            Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            applySalaryExportComment(cell, salaryExport, drawing);
        }
    }

    static String buildSalaryExportCommentText(HrmSalaryExport salaryExport, int columnIndex) {
        if (salaryExport == null) {
            return "";
        }
        switch (columnIndex) {
            case FULL_ATTENDANCE_COLUMN_INDEX:
                return buildFullAttendanceComment(salaryExport);
            case ABSENCE_SALARY_COLUMN_INDEX:
                return buildAbsenceSalaryComment(salaryExport);
            case TAX_COLUMN_INDEX:
                return buildTaxComment(salaryExport);
            case UNION_FEES_COLUMN_INDEX:
                return buildUnionFeesComment(salaryExport);
            default:
                return "";
        }
    }

    private Drawing<?> drawingFor(Sheet sheet) {
        return drawingBySheet.computeIfAbsent(sheet, Sheet::createDrawingPatriarch);
    }

    private static void applySalaryExportComment(Cell cell, HrmSalaryExport salaryExport, Drawing<?> drawing) {
        String commentText = buildSalaryExportCommentText(salaryExport, cell.getColumnIndex());
        if (commentText == null || commentText.trim().isEmpty()) {
            return;
        }

        Workbook workbook = cell.getSheet().getWorkbook();
        CreationHelper creationHelper = workbook.getCreationHelper();
        ClientAnchor anchor = creationHelper.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 5);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 6);

        Comment comment = drawing.createCellComment(anchor);
        comment.setString(creationHelper.createRichTextString(commentText));
        comment.setAuthor("薪资系统");
        cell.setCellComment(comment);
    }

    private static String buildFullAttendanceComment(HrmSalaryExport salaryExport) {
        BigDecimal fullAttendance = safeDecimal(salaryExport.getFullattendancesalary());
        String base = "满勤/全勤奖 = 工资项 40102。金额来自计薪员工 fullMoney，员工级金额优先，基本工资设置兜底。";
        if (isBlankOrZero(fullAttendance)) {
            return base + "当前为空/为0，表示薪资核算未生成 40102；通常因为未转正或当月半路转正、未启用全勤奖、存在有效病假、应计出勤低于应出勤、缺少加班/夜班统计应出勤数据，或薪资项未生成。"
                    + "当前满勤天数：" + safeString(salaryExport.getNormaldays())
                    + "，超缺勤天数：" + amount(salaryExport.getAbsencehours()) + "。";
        }
        return base + "满足正式/启用全勤、无有效病假、应计出勤达到应出勤等条件后生成。当前全勤奖："
                + amount(fullAttendance) + "。";
    }

    private static String buildAbsenceSalaryComment(HrmSalaryExport salaryExport) {
        return "超缺勤工资 = 工资项 200101，由迟到、早退、旷工、事假、病假、缺卡等考勤扣款项汇总生成。"
                + "超缺勤天数 = (应出勤天数 * 8 - 应计出勤小时) / 8；应出勤和应计出勤读取加班/夜班统计落库结果。"
                + "当前超缺勤天数：" + amount(salaryExport.getAbsencehours())
                + "，当前超缺勤工资：" + amount(salaryExport.getAbsencesalary()) + "。";
    }

    private static String buildTaxComment(HrmSalaryExport salaryExport) {
        return "个人所得税 = 工资项 230101，按累计预扣法计算。"
                + "累计收入（含本月应税收入、福利计税、只计税奖金） - 累计减除费用 - 累计专项扣除（社保/公积金） - 累计专项附加扣除 = 累计应纳税所得额；"
                + "再按税率和速算扣除计算累计应纳税额，减去累计已缴税额得到本月个税。"
                + "当前个税：" + amount(salaryExport.getTax()) + "。";
    }

    private static String buildUnionFeesComment(HrmSalaryExport salaryExport) {
        BigDecimal unionFees = safeDecimal(salaryExport.getUnionfees());
        String base = "工会费 = 工资项 160102。正常规则为应发工资 * 0.5%，并受公司、员工状态、转正时间和应发工资规则限制。";
        if (isBlankOrZero(unionFees)) {
            return base + "当前为空/为0，通常因为成都/攀枝花公司免收、应发工资小于等于0、实习/离职员工、半路转正员工，或薪资项 160102 未生成。"
                    + "当前应发工资：" + amount(salaryExport.getTotalsalary()) + "。";
        }
        return base + "当前应发工资：" + amount(salaryExport.getTotalsalary())
                + "，当前工会费：" + amount(unionFees) + "。";
    }

    private static boolean isSubtotalOrTotalRow(HrmSalaryExport salaryExport) {
        if (salaryExport == null) {
            return true;
        }
        String empName = salaryExport.getEmpname();
        return "小计".equals(empName) || "合计".equals(empName);
    }

    private static boolean isBlankOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private static BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String amount(BigDecimal value) {
        return safeDecimal(value).toPlainString();
    }

    private static String safeString(String value) {
        return value == null || value.trim().isEmpty() ? "0" : value;
    }
}
