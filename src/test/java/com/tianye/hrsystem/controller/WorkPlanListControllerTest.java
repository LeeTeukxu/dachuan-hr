package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.entity.vo.WorkPlanSubmitProgressVO;
import com.tianye.hrsystem.entity.vo.WorkPlanSubmitResultVO;
import com.tianye.hrsystem.entity.vo.WorkPlanImportPreviewVO;
import com.tianye.hrsystem.entity.vo.WorkPlanCustomShiftOptionVO;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentBO;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentsBO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayAssignmentsVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.service.IWorkPlanService;
import org.junit.Assert;
import org.junit.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class WorkPlanListControllerTest {

    @InjectMocks
    private WorkPlanListController controller;

    @Mock
    private IWorkPlanService planService;

    @Test
    public void add_shouldAcceptCustomShiftFieldsAndReturnTask() throws Exception {
        WorkPlanSubmitResultVO submitResult = new WorkPlanSubmitResultVO();
        submitResult.setTaskId("task-001");
        submitResult.setStatus("PENDING");
        submitResult.setMessage("排班提交任务已创建");
        when(planService.AddAll(anyList())).thenReturn(submitResult);

        String payload = "[{\"id\":null,\"productName\":\"测试车间\",\"linkName\":\"测试工段\",\"workshopName\":\"包装车间\",\"groupId\":\"1\",\"classId\":\"2\",\"userId\":\"3\",\"workDate\":\"2026-01-04\",\"shiftType\":\"custom\",\"customStart\":\"08:30\",\"customEnd\":\"17:45\",\"customContinuousShift\":true}]";

        successResult result = controller.Add(payload);

        Assert.assertTrue(result.getSuccess());
        Assert.assertTrue(result.getData() instanceof WorkPlanSubmitResultVO);
        Assert.assertEquals("task-001", ((WorkPlanSubmitResultVO) result.getData()).getTaskId());
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(planService).AddAll(captor.capture());
        tbplanlist plan = (tbplanlist) captor.getValue().get(0);
        Assert.assertEquals("2026-01-04", new SimpleDateFormat("yyyy-MM-dd").format(plan.getWorkDate()));
        Assert.assertEquals("custom", plan.getShiftType());
        Assert.assertEquals("08:30", plan.getCustomStart());
        Assert.assertEquals("17:45", plan.getCustomEnd());
        Assert.assertTrue(Boolean.TRUE.equals(plan.getCustomContinuousShift()));
        Assert.assertEquals("包装车间", plan.getWorkshopName());
    }

    @Test
    public void add_shouldKeepRestShiftTypeForAdjustAndRestOptions() throws Exception {
        WorkPlanSubmitResultVO submitResult = new WorkPlanSubmitResultVO();
        submitResult.setTaskId("task-rest-type");
        submitResult.setStatus("PENDING");
        when(planService.AddAll(anyList())).thenReturn(submitResult);

        String payload = "["
                + "{\"groupId\":\"1\",\"userId\":\"u1\",\"workDate\":\"2026-01-04\",\"shiftType\":\"rest\",\"restShiftType\":\"adjust\"},"
                + "{\"groupId\":\"1\",\"userId\":\"u2\",\"workDate\":\"2026-01-04\",\"shiftType\":\"rest\",\"restShiftType\":\"rest\"}"
                + "]";

        successResult result = controller.Add(payload);

        Assert.assertTrue(result.getSuccess());
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(planService).AddAll(captor.capture());
        List plans = captor.getValue();
        Assert.assertEquals("adjust", ((tbplanlist) plans.get(0)).getRestShiftType());
        Assert.assertEquals("rest", ((tbplanlist) plans.get(1)).getRestShiftType());
    }

    @Test
    public void add_shouldAcceptDateTimeWorkDate() throws Exception {
        WorkPlanSubmitResultVO submitResult = new WorkPlanSubmitResultVO();
        submitResult.setTaskId("task-002");
        submitResult.setStatus("PENDING");
        when(planService.AddAll(anyList())).thenReturn(submitResult);

        String payload = "[{\"id\":null,\"productName\":\"测试车间\",\"linkName\":\"测试工段\",\"groupId\":\"1\",\"classId\":\"2\",\"userId\":\"3\",\"workDate\":\"2026-01-04 00:00:00\"}]";

        successResult result = controller.Add(payload);

        Assert.assertTrue(result.getSuccess());
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(planService).AddAll(captor.capture());
        tbplanlist plan = (tbplanlist) captor.getValue().get(0);
        Assert.assertEquals("2026-01-04", new SimpleDateFormat("yyyy-MM-dd").format(plan.getWorkDate()));
    }

    @Test
    public void querySubmitProgress_shouldReturnProgressData() throws Exception {
        WorkPlanSubmitProgressVO progressVO = new WorkPlanSubmitProgressVO();
        progressVO.setTaskId("task-003");
        progressVO.setStatus("RUNNING");
        progressVO.setMessage("正在第1次提交，共10次");
        when(planService.querySubmitProgress(eq("task-003"))).thenReturn(progressVO);

        successResult result = controller.querySubmitProgress("task-003");

        Assert.assertTrue(result.getSuccess());
        Assert.assertTrue(result.getData() instanceof WorkPlanSubmitProgressVO);
        WorkPlanSubmitProgressVO data = (WorkPlanSubmitProgressVO) result.getData();
        Assert.assertEquals("task-003", data.getTaskId());
        Assert.assertEquals("RUNNING", data.getStatus());
    }

    @Test
    public void queryCustomShiftList_shouldReturnOptions() throws Exception {
        WorkPlanCustomShiftOptionVO option = new WorkPlanCustomShiftOptionVO();
        option.setId(11L);
        option.setShiftName("自定义班次 08:30-17:30");
        option.setCustomStart("08:30");
        option.setCustomEnd("17:30");
        option.setCustomCrossDay(false);
        option.setDisplayName("自定义班次(08:30~17:30)");
        when(planService.queryCustomShiftOptions()).thenReturn(Collections.singletonList(option));

        successResult result = controller.queryCustomShiftList();

        Assert.assertTrue(result.getSuccess());
        Assert.assertTrue(result.getData() instanceof List);
        List data = (List) result.getData();
        Assert.assertEquals(1, data.size());
        verify(planService).queryCustomShiftOptions();
    }

    @Test
    public void previewImportExcel_shouldDelegateToServiceAndReturnPreview() throws Exception {
        WorkPlanImportPreviewVO preview = new WorkPlanImportPreviewVO();
        preview.setTotalCount(2);
        preview.setValidCount(1);
        preview.setErrorCount(1);
        when(planService.previewImportExcel(org.mockito.ArgumentMatchers.any())).thenReturn(preview);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "排班导入模板.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});

        successResult result = controller.previewImportExcel(file);

        Assert.assertTrue(result.getSuccess());
        Assert.assertTrue(result.getData() instanceof WorkPlanImportPreviewVO);
        WorkPlanImportPreviewVO data = (WorkPlanImportPreviewVO) result.getData();
        Assert.assertEquals(Integer.valueOf(2), data.getTotalCount());
        Assert.assertEquals(Integer.valueOf(1), data.getValidCount());
        Assert.assertEquals(Integer.valueOf(1), data.getErrorCount());
        verify(planService).previewImportExcel(file);
    }

    @Test
    public void downloadImportTemplate_shouldFillDateRowWithFullDatesAndClearBodyBySelectedMonth() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadImportTemplate("2026-06", null, response);

        verify(planService, never()).createImportTemplateExcel();
        Assert.assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        Assert.assertTrue(response.getHeader("Content-Disposition").contains("workplan.xlsx"));
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dateRow = sheet.getRow(0);
            Assert.assertEquals("日期", dateRow.getCell(0).getStringCellValue());
            Assert.assertEquals("电话", readCellText(sheet.getRow(1).getCell(1)));
            Assert.assertEquals("产品", readCellText(sheet.getRow(1).getCell(2)));
            Assert.assertEquals("岗位", readCellText(sheet.getRow(1).getCell(3)));
            Assert.assertEquals("车间", readCellText(sheet.getRow(1).getCell(4)));
            Assert.assertEquals("2026-06-01", readCellText(dateRow.getCell(5)));
            Assert.assertEquals("General", dateRow.getCell(5).getCellStyle().getDataFormatString());
            Assert.assertEquals("2026-06-02", readCellText(dateRow.getCell(10)));
            Assert.assertEquals("General", dateRow.getCell(10).getCellStyle().getDataFormatString());
            Assert.assertEquals("2026-06-30", readCellText(dateRow.getCell(150)));
            Assert.assertEquals("General", dateRow.getCell(150).getCellStyle().getDataFormatString());
            assertDateGroupBlank(sheet, 155);
            assertRowsBlankExceptHeaders(sheet);
        }
    }

    @Test
    public void downloadImportTemplate_shouldKeepThirtyFirstColumnsWhenMonthHasThirtyOneDays() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadImportTemplate("2026-07", null, response);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Assert.assertEquals("电话", readCellText(sheet.getRow(1).getCell(1)));
            Assert.assertEquals("产品", readCellText(sheet.getRow(1).getCell(2)));
            Assert.assertEquals("岗位", readCellText(sheet.getRow(1).getCell(3)));
            Assert.assertEquals("车间", readCellText(sheet.getRow(1).getCell(4)));
            Assert.assertEquals("2026-07-31", readCellText(sheet.getRow(0).getCell(155)));
            Assert.assertEquals("白/夜班", readCellText(sheet.getRow(1).getCell(155)));
            Assert.assertEquals("上班时间", readCellText(sheet.getRow(1).getCell(156)));
            Assert.assertEquals("下班时间", readCellText(sheet.getRow(1).getCell(157)));
            Assert.assertEquals("是否连班", readCellText(sheet.getRow(1).getCell(158)));
            Assert.assertEquals("休假/调休", readCellText(sheet.getRow(1).getCell(159)));
            assertMergedRegion(sheet, 0, 155, 0, 159);
            Assert.assertEquals("日期表头必须居中", HorizontalAlignment.CENTER, sheet.getRow(0).getCell(155).getCellStyle().getAlignmentEnum());
            Assert.assertEquals("日期组表头必须居中", HorizontalAlignment.CENTER, sheet.getRow(1).getCell(155).getCellStyle().getAlignmentEnum());
            Assert.assertEquals("同一日期组应使用同一底色",
                    sheet.getRow(0).getCell(155).getCellStyle().getFillForegroundColor(),
                    sheet.getRow(1).getCell(159).getCellStyle().getFillForegroundColor());
            Assert.assertNotEquals("相邻日期组应使用两种颜色循环区分",
                    sheet.getRow(0).getCell(150).getCellStyle().getFillForegroundColor(),
                    sheet.getRow(0).getCell(155).getCellStyle().getFillForegroundColor());
            assertCellsBlankAfter(sheet.getRow(0), 160);
            assertCellsBlankAfter(sheet.getRow(1), 160);
        }
    }

    private String readCellText(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (value == Math.rint(value)) {
                return String.valueOf((int) value);
            }
            return String.valueOf(value);
        }
        return cell.toString();
    }

    private void assertRowsBlankExceptHeaders(Sheet sheet) {
        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                Assert.assertEquals("第" + (rowIndex + 1) + "行第" + (cellIndex + 1) + "列应为空", "", readCellText(row.getCell(cellIndex)));
            }
        }
    }

    private void assertDateGroupBlank(Sheet sheet, int startCellIndex) {
        for (int offset = 0; offset < 5; offset++) {
            Assert.assertEquals("第31天第" + (offset + 1) + "列应为空", "", readCellText(sheet.getRow(0).getCell(startCellIndex + offset)));
            Assert.assertEquals("第31天表头第" + (offset + 1) + "列应为空", "", readCellText(sheet.getRow(1).getCell(startCellIndex + offset)));
        }
    }

    private void assertMergedRegion(Sheet sheet, int firstRow, int firstColumn, int lastRow, int lastColumn) {
        for (int index = 0; index < sheet.getNumMergedRegions(); index++) {
            org.apache.poi.ss.util.CellRangeAddress region = sheet.getMergedRegion(index);
            if (region.getFirstRow() == firstRow && region.getFirstColumn() == firstColumn
                    && region.getLastRow() == lastRow && region.getLastColumn() == lastColumn) {
                return;
            }
        }
        Assert.fail("缺少合并区域: row " + firstRow + ", col " + firstColumn + " -> row " + lastRow + ", col " + lastColumn);
    }

    private void assertCellsBlankAfter(Row row, int firstBlankCellIndex) {
        if (row == null || row.getLastCellNum() <= firstBlankCellIndex) {
            return;
        }
        for (int cellIndex = firstBlankCellIndex; cellIndex < row.getLastCellNum(); cellIndex++) {
            Assert.assertEquals("第" + (cellIndex + 1) + "列应为空", "", readCellText(row.getCell(cellIndex)));
        }
    }

    @Test
    public void downloadImportTemplate_shouldDefaultBlankTemplateMonthAndStillDownloadExcel() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadImportTemplate(null, null, response);

        Assert.assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Assert.assertEquals("日期", sheet.getRow(0).getCell(0).getStringCellValue());
            Assert.assertEquals(new SimpleDateFormat("yyyy-MM").format(new Date()) + "-01", readCellText(sheet.getRow(0).getCell(5)));
        }
    }

    @Test
    public void queryEmployeeDayShift_shouldParseDateAndReturnData() throws Exception {
        WorkPlanEmployeeDayShiftVO vo = new WorkPlanEmployeeDayShiftVO();
        vo.setEmployeeId(101L);
        vo.setSource("local");
        vo.setCurrentShiftType("custom");
        vo.setCustomStart("08:30");
        vo.setCustomEnd("17:30");
        when(planService.queryEmployeeDayShift(eq(101L), org.mockito.ArgumentMatchers.any(Date.class))).thenReturn(vo);

        successResult result = controller.queryEmployeeDayShift(101L, "2026-01-04 00:00:00");

        Assert.assertTrue(result.getSuccess());
        Assert.assertTrue(result.getData() instanceof WorkPlanEmployeeDayShiftVO);
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(planService).queryEmployeeDayShift(eq(101L), captor.capture());
        Assert.assertEquals("2026-01-04", new SimpleDateFormat("yyyy-MM-dd").format(captor.getValue()));
    }

    @Test
    public void queryEmployeeDayAssignments_shouldParseDateAndReturnAllAssignments() throws Exception {
        WorkPlanEmployeeDayAssignmentsVO vo = new WorkPlanEmployeeDayAssignmentsVO();
        vo.setEmployeeId(101L);
        vo.setDayStatus("work");
        when(planService.queryEmployeeDayAssignments(eq(101L), org.mockito.ArgumentMatchers.any(Date.class))).thenReturn(vo);

        successResult result = controller.queryEmployeeDayAssignments(101L, "2026-08-24 00:00:00");

        Assert.assertTrue(result.getSuccess());
        Assert.assertTrue(result.getData() instanceof WorkPlanEmployeeDayAssignmentsVO);
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(planService).queryEmployeeDayAssignments(eq(101L), captor.capture());
        Assert.assertEquals("2026-08-24", new SimpleDateFormat("yyyy-MM-dd").format(captor.getValue()));
    }

    @Test
    public void removeEmployeeDayShift_shouldDelegateToService() throws Exception {
        successResult result = controller.removeEmployeeDayShift(102L, "2026-04-13");

        Assert.assertTrue(result.getSuccess());
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(planService).removeEmployeeDayShift(eq(102L), captor.capture());
        Assert.assertEquals("2026-04-13", new SimpleDateFormat("yyyy-MM-dd").format(captor.getValue()));
    }

    @Test
    public void saveEmployeeDayCustomShift_shouldDelegateToService() throws Exception {
        WorkPlanEmployeeDayShiftVO vo = new WorkPlanEmployeeDayShiftVO();
        vo.setEmployeeId(102L);
        vo.setCurrentShiftType("custom");
        vo.setCustomStart("09:00");
        vo.setCustomEnd("18:00");
        vo.setCustomShiftPeriod("night");
        when(planService.saveEmployeeDayShift(eq(102L), org.mockito.ArgumentMatchers.any(Date.class), eq("custom"),
                eq("09:00"), eq("18:00"), eq("night"), eq(false), eq(false), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(vo);

        successResult result = controller.saveEmployeeDayCustomShift(102L, "2026-01-05", null, "09:00", "18:00", "night", false, null, null);

        Assert.assertTrue(result.getSuccess());
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(planService).saveEmployeeDayShift(eq(102L), captor.capture(), eq("custom"), eq("09:00"), eq("18:00"), eq("night"), eq(false), eq(false), org.mockito.ArgumentMatchers.isNull());
        Assert.assertEquals("2026-01-05", new SimpleDateFormat("yyyy-MM-dd").format(captor.getValue()));
    }

    @Test
    public void saveEmployeeDayAssignments_shouldDelegateCompleteRequestToService() throws Exception {
        SaveWorkPlanEmployeeDayAssignmentBO assignment = new SaveWorkPlanEmployeeDayAssignmentBO();
        assignment.setProductName("椰子饼");
        assignment.setPositionName("装盒");
        assignment.setShiftType("custom");
        assignment.setCustomStart("08:00");
        assignment.setCustomEnd("17:00");

        SaveWorkPlanEmployeeDayAssignmentsBO request = new SaveWorkPlanEmployeeDayAssignmentsBO();
        request.setEmployeeId(102L);
        request.setWorkDate("2026-08-24");
        request.setDayStatus("work");
        request.setAssignments(Collections.singletonList(assignment));

        WorkPlanEmployeeDayAssignmentsVO vo = new WorkPlanEmployeeDayAssignmentsVO();
        vo.setEmployeeId(102L);
        vo.setDayStatus("work");
        when(planService.saveEmployeeDayAssignments(request)).thenReturn(vo);

        successResult result = controller.saveEmployeeDayAssignments(request);

        Assert.assertTrue(result.getSuccess());
        Assert.assertTrue(result.getData() instanceof WorkPlanEmployeeDayAssignmentsVO);
        verify(planService).saveEmployeeDayAssignments(request);
    }

    @Test
    public void saveEmployeeDayCustomShift_shouldDelegateDayContinuousShiftToService() throws Exception {
        WorkPlanEmployeeDayShiftVO vo = new WorkPlanEmployeeDayShiftVO();
        vo.setEmployeeId(104L);
        vo.setCurrentShiftType("custom");
        vo.setCustomShiftPeriod("day");
        vo.setCustomContinuousShift(true);
        when(planService.saveEmployeeDayShift(eq(104L), org.mockito.ArgumentMatchers.any(Date.class), eq("custom"),
                eq("08:00"), eq("17:30"), eq("day"), eq(true), eq(false), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(vo);

        successResult result = controller.saveEmployeeDayCustomShift(104L, "2026-01-07", "custom", "08:00", "17:30", "day", true, null, null);

        Assert.assertTrue(result.getSuccess());
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(planService).saveEmployeeDayShift(eq(104L), captor.capture(), eq("custom"), eq("08:00"), eq("17:30"), eq("day"), eq(true), eq(false), org.mockito.ArgumentMatchers.isNull());
        Assert.assertEquals("2026-01-07", new SimpleDateFormat("yyyy-MM-dd").format(captor.getValue()));
    }

    @Test
    public void saveEmployeeDayCustomShift_shouldDelegateExplicitManualNonContinuousShiftToService() throws Exception {
        WorkPlanEmployeeDayShiftVO vo = new WorkPlanEmployeeDayShiftVO();
        vo.setEmployeeId(106L);
        vo.setCurrentShiftType("custom");
        vo.setCustomShiftPeriod("day");
        vo.setCustomContinuousShift(false);
        when(planService.saveEmployeeDayShift(eq(106L), org.mockito.ArgumentMatchers.any(Date.class), eq("custom"),
                eq("08:00"), eq("17:30"), eq("day"), eq(false), eq(true), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(vo);

        successResult result = controller.saveEmployeeDayCustomShift(106L, "2026-01-09", "custom", "08:00", "17:30", "day", false, true, null);

        Assert.assertTrue(result.getSuccess());
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(planService).saveEmployeeDayShift(eq(106L), captor.capture(), eq("custom"), eq("08:00"), eq("17:30"), eq("day"), eq(false), eq(true), org.mockito.ArgumentMatchers.isNull());
        Assert.assertEquals("2026-01-09", new SimpleDateFormat("yyyy-MM-dd").format(captor.getValue()));
    }

    @Test
    public void saveEmployeeDayCustomShift_shouldDelegateRestShiftTypeToService() throws Exception {
        WorkPlanEmployeeDayShiftVO vo = new WorkPlanEmployeeDayShiftVO();
        vo.setEmployeeId(105L);
        vo.setCurrentShiftType("rest");
        vo.setRestShiftType("adjust");
        when(planService.saveEmployeeDayShift(eq(105L), org.mockito.ArgumentMatchers.any(Date.class), eq("rest"),
                eq(null), eq(null), eq(null), eq(false), eq(false), eq("adjust")))
                .thenReturn(vo);

        successResult result = controller.saveEmployeeDayCustomShift(105L, "2026-01-08", "rest", null, null, null, false, null, "adjust");

        Assert.assertTrue(result.getSuccess());
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(planService).saveEmployeeDayShift(eq(105L), captor.capture(), eq("rest"), eq(null), eq(null), eq(null), eq(false), eq(false), eq("adjust"));
        Assert.assertEquals("2026-01-08", new SimpleDateFormat("yyyy-MM-dd").format(captor.getValue()));
    }

    @Test
    public void saveEmployeeDayCustomShift_shouldClearContinuousShiftForRest() throws Exception {
        WorkPlanEmployeeDayShiftVO vo = new WorkPlanEmployeeDayShiftVO();
        vo.setEmployeeId(103L);
        vo.setCurrentShiftType("rest");
        when(planService.saveEmployeeDayShift(eq(103L), org.mockito.ArgumentMatchers.any(Date.class), eq("rest"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), eq(false), eq(false), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(vo);

        successResult result = controller.saveEmployeeDayCustomShift(103L, "2026-01-06", "rest", null, null, null, true, true, null);

        Assert.assertTrue(result.getSuccess());
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(planService).saveEmployeeDayShift(eq(103L), captor.capture(), eq("rest"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), eq(false), eq(false), org.mockito.ArgumentMatchers.isNull());
        Assert.assertEquals("2026-01-06", new SimpleDateFormat("yyyy-MM-dd").format(captor.getValue()));
    }
}
