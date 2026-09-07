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
 * 批注正文优先使用导出服务按员工生成的具体说明（见 HrmSalaryExport 各 comment 字段），
 * 无具体说明时回退到通用解释文本。
 */
public class SalaryExportCommentWriteHandler implements CellWriteHandler {

    private static final int DATA_START_ROW_INDEX = 4;
    private static final int OTHER_SUBSIDY_COLUMN_INDEX = 16;
    private static final int FULL_ATTENDANCE_COLUMN_INDEX = 17;
    private static final int ABSENCE_SALARY_COLUMN_INDEX = 19;
    private static final int TAX_COLUMN_INDEX = 21;
    private static final int UNION_FEES_COLUMN_INDEX = 25;
    private static final Set<Integer> COMMENT_COLUMN_INDEXES = new HashSet<>(Arrays.asList(
            OTHER_SUBSIDY_COLUMN_INDEX,
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
        String prepared = preparedComment(salaryExport, columnIndex);
        if (prepared != null && !prepared.trim().isEmpty()) {
            return prepared;
        }
        switch (columnIndex) {
            case OTHER_SUBSIDY_COLUMN_INDEX:
                return buildOtherSubsidyComment(salaryExport);
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

    private static String preparedComment(HrmSalaryExport salaryExport, int columnIndex) {
        switch (columnIndex) {
            case OTHER_SUBSIDY_COLUMN_INDEX:
                return salaryExport.getOtherSubsidyComment();
            case FULL_ATTENDANCE_COLUMN_INDEX:
                return salaryExport.getFullAttendanceComment();
            case ABSENCE_SALARY_COLUMN_INDEX:
                return salaryExport.getAbsenceComment();
            case TAX_COLUMN_INDEX:
                return salaryExport.getTaxComment();
            case UNION_FEES_COLUMN_INDEX:
                return salaryExport.getUnionFeesComment();
            default:
                return null;
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

    private static String buildOtherSubsidyComment(HrmSalaryExport salaryExport) {
        BigDecimal otherSubsidy = safeDecimal(salaryExport.getOthersalary());
        String base = "其他补贴的数据来自【考勤管理】中的“每月考勤统计”（上传考勤）：先通过考勤报表文件导入，"
                + "也可以在考勤统计页面上直接修改单元格保存，薪资核算时按该列金额发放。";
        if (isBlankOrZero(otherSubsidy)) {
            return base + "本月该员工的其他补贴未填写或为0。";
        }
        return base + "本月金额：" + amount(otherSubsidy) + " 元。";
    }

    private static String buildFullAttendanceComment(HrmSalaryExport salaryExport) {
        BigDecimal fullAttendance = safeDecimal(salaryExport.getFullattendancesalary());
        String base = "全勤奖是员工当月满勤发放的奖励，金额在【薪资管理-基本工资设置】中按员工设定。";
        if (isBlankOrZero(fullAttendance)) {
            String reason = "本月没有全勤奖，常见原因：① 入职后尚未转正，或当月才转正；"
                    + "② 基本工资设置中未启用全勤奖；"
                    + "③ 本月有病假，按规则取消全勤；"
                    + "④ 本月出勤不满，存在迟到、早退、旷工、缺卡或请假。";
            if (salaryExport.getAbsencehours() != null && salaryExport.getAbsencehours().compareTo(BigDecimal.ZERO) != 0) {
                reason = "本月没有全勤奖：该员工存在超缺勤（超缺勤天数 " + amount(salaryExport.getAbsencehours())
                        + " 天），不满足满勤条件。另需注意：① 入职后尚未转正或当月才转正、② 基本工资设置中未启用全勤奖，也会导致没有全勤奖。";
            }
            return base + reason
                    + "当前满勤天数：" + safeString(salaryExport.getNormaldays()) + " 天。";
        }
        return base + "该员工本月满足满勤条件（已转正、无病假、出勤达标），获得全勤奖 "
                + amount(fullAttendance) + " 元。当前满勤天数：" + safeString(salaryExport.getNormaldays()) + " 天。";
    }

    private static String buildAbsenceSalaryComment(HrmSalaryExport salaryExport) {
        return "超缺勤工资是因迟到、早退、旷工、请事假、请病假、缺卡等从工资中扣除的部分，"
                + "由各项考勤扣款汇总得出。超缺勤天数 =（应出勤天数 × 8 − 应计出勤小时）÷ 8，"
                + "出勤数据来自考勤统计结果。病假扣款规则：每月前2天病假不扣钱，超过2天的部分按当地最低工资标准折算扣除。"
                + "当前超缺勤天数：" + amount(salaryExport.getAbsencehours())
                + " 天，超缺勤扣款合计：" + amount(salaryExport.getAbsencesalary()) + " 元。"
                + "各项扣款的具体金额见核算后的工资明细（迟到、早退、旷工、事假、病假、缺卡）。";
    }

    private static String buildTaxComment(HrmSalaryExport salaryExport) {
        return "个人所得税按“累计预扣”方法计算：把今年1月至本月的收入累加，"
                + "减去每月5000元的固定减除费用、个人承担的社保和公积金、专项附加扣除，得到累计应纳税所得额；"
                + "再按税率表（年应纳税所得额不超过3.6万的部分3%，之后依次10%、20%、25%、30%、35%、45%）算出累计应缴税额，"
                + "减去之前月份已缴的税，就是本月要扣的个税。"
                + "当前个税：" + amount(salaryExport.getTax()) + " 元。"
                + "该员工本月的具体计算过程（累计收入、各项扣除、适用税率等）见核算后保存的个税累计数据。";
    }

    private static String buildUnionFeesComment(HrmSalaryExport salaryExport) {
        BigDecimal unionFees = safeDecimal(salaryExport.getUnionfees());
        String base = "工会费按应发工资的0.5%收取，并受员工状态、转正时间等规则限制。";
        if (isBlankOrZero(unionFees)) {
            return base + "本月没有工会费，常见原因：① 实习或已离职员工不收取；"
                    + "② 入职当月尚未转正不收取；③ 本月应发工资为0；④ 所在公司免收工会费。"
                    + "当前应发工资：" + amount(salaryExport.getTotalsalary()) + " 元。";
        }
        return base + "本月计算：应发工资 " + amount(salaryExport.getTotalsalary())
                + " 元 × 0.5% = " + amount(unionFees) + " 元。";
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
