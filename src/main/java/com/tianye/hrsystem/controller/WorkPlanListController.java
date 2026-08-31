package com.tianye.hrsystem.controller;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tianye.hrsystem.common.*;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.SaveWorkPlanEmployeeDayAssignmentsBO;
import com.tianye.hrsystem.entity.vo.WorkPlanCustomShiftOptionVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayAssignmentsVO;
import com.tianye.hrsystem.entity.vo.WorkPlanEmployeeDayShiftVO;
import com.tianye.hrsystem.entity.vo.WorkPlanImportPreviewVO;
import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IWorkPlanService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: WorkPlanListController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年08月02日 17:40
 **/

@Controller
@RequestMapping("/workPlan")
public class WorkPlanListController {
    private static final String IMPORT_TEMPLATE_RESOURCE = "export/workplan.xlsx";
    private static final String IMPORT_TEMPLATE_FILE_NAME = "workplan.xlsx";
    private static final DateTimeFormatter TEMPLATE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TEMPLATE_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String[] IMPORT_TEMPLATE_CHILD_HEADERS = new String[]{
            "白/夜班", "上班时间", "下班时间", "是否连班", "休假/调休"
    };
    private static final String[] IMPORT_TEMPLATE_PRODUCT_POSITION_HEADERS = new String[]{
            "产品", "岗位"
    };

    private static final String[] DATE_PATTERNS = new String[]{
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
    };

    @Autowired
    IWorkPlanService planService;

    @Autowired
    tbPlanListRepository planRep;

    @Autowired
    tbattendanceuserRepository userRep;

    @Autowired
    hrmAttendanceShiftRepository shiftRep;
    @Autowired
    IAccessToken accessToken;

    @RequestMapping("/saveAll")
    @ResponseBody
    public successResult Add(String Data){
        successResult result=new successResult();
        try {
            List<tbplanlist> list = parsePlanList(Data);
            result.setData(planService.AddAll(list));
        }
        catch(Exception ax){
            result.raiseException(ax);
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("/getData")
    public PageObject<tbplanlist> getData(String  GroupID,String Begin,String End, Integer pageSize,Integer pageNum,
            String sortField,
            String sortOrder){
        Page<tbplanlist> datas=null;
        try {
            if(pageSize==null) pageSize=20;
            if(pageNum==null) pageNum=0;
            if(StringUtils.isEmpty(sortField)) sortField="createTime";
            if(StringUtils.isEmpty(sortOrder))sortOrder="asc";

            PageEntity entity=new PageEntity();
            entity.setOrder(sortOrder);
            entity.setSortField(sortField);
            entity.setPageNum(pageNum);
            entity.setPageSize(pageSize);
            Pageable pageable= PageableUtils.From(entity);

            if(StringUtils.isEmpty(Begin) && StringUtils.isEmpty(End)){
                datas=planRep.findAllByGroupId(GroupID,pageable);
            } else {
                if(StringUtils.isEmpty(Begin) || StringUtils.isEmpty(End)){
                    throw new Exception("请同时指明开始(Begin)和结束(End)时间!");
                }
                Date begin = parseDateValue(Begin, "Begin");
                Date end = parseDateValue(End, "End");
                if(StringUtils.isEmpty(GroupID)==false){
                    datas=planRep.findAllByGroupIdAndWorkDateBetween(GroupID,begin,end,pageable);
                } else {
                    datas=planRep.findAllByWorkDateBetween(begin,end,pageable);
                }
            }
            if (datas != null) {
                planService.fillCustomShiftMeta(datas.getContent());
            }
        }
        catch(Exception ax){

            return PageObject.Error(ax.getMessage());
        }
        return PageObject.Of(datas);
    }

    @RequestMapping("/loadIsLast")
    @ResponseBody
    public PageObject<tbplanlist> loadIsLast(String SelectDate, String WorkDate, String Begin, Boolean LoadLast,
                                             Boolean isLast, Integer pageSize, Integer pageNum,
                                             String sortField, String sortOrder) {
        PageObject<tbplanlist> datas = null;
        try {
            String selectedDateText = SelectDate;
            if (StringUtils.isEmpty(selectedDateText)) {
                selectedDateText = WorkDate;
            }
            if (StringUtils.isEmpty(selectedDateText)) {
                selectedDateText = Begin;
            }

            Boolean loadLast = LoadLast != null ? LoadLast : isLast;
            if (loadLast == null) {
                loadLast = true;
            }

            if (StringUtils.isEmpty(selectedDateText)) {
                datas = planService.getMaxDate(pageSize, pageNum, sortField, sortOrder);
            } else {
                Date selectedDate = parseDateValue(selectedDateText, "SelectDate");
                datas = planService.loadBySelectedDate(selectedDate, loadLast, pageSize, pageNum, sortField, sortOrder);
            }
            datas.setLastPage(true);

        }catch (Exception ax) {
            return PageObject.Error(ax.getMessage());
        }
        return datas;
    }

    @RequestMapping("/querySubmitProgress")
    @ResponseBody
    public successResult querySubmitProgress(String taskId) {
        successResult result = new successResult();
        try {
            if (StringUtils.isBlank(taskId)) {
                throw new Exception("taskId不能为空");
            }
            result.setData(planService.querySubmitProgress(taskId));
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/previewImportExcel")
    @ResponseBody
    public successResult previewImportExcel(MultipartFile file) {
        successResult result = new successResult();
        try {
            WorkPlanImportPreviewVO preview = planService.previewImportExcel(file);
            result.setData(preview);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/downloadImportTemplate")
    public void downloadImportTemplate(String templateMonth, String templateDate, HttpServletResponse response) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(IMPORT_TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, IMPORT_TEMPLATE_FILE_NAME + " not found");
                return;
            }
            try (Workbook workbook = WorkbookFactory.create(inputStream);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                YearMonth selectedMonth = resolveTemplateMonth(templateMonth, templateDate);
                fillTemplateDateRow(workbook, selectedMonth);
                clearTemplateBodyRows(workbook);
                workbook.write(outputStream);
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(IMPORT_TEMPLATE_FILE_NAME, "UTF-8"));
                response.setHeader("Set-Cookie", "fileDownload=true; path=/");
                response.getOutputStream().write(outputStream.toByteArray());
                response.flushBuffer();
            }
        } catch (Exception ax) {
            try {
                response.setCharacterEncoding("utf-8");
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(ax.getLocalizedMessage());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private YearMonth resolveTemplateMonth(String templateMonth, String templateDate) throws Exception {
        if (StringUtils.isNotBlank(templateMonth)) {
            return YearMonth.parse(templateMonth, TEMPLATE_MONTH_FORMATTER);
        }
        if (StringUtils.isNotBlank(templateDate)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parseDateValue(templateDate, "templateDate"));
            return YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
        }
        Calendar calendar = Calendar.getInstance();
        return YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
    }

    private void fillTemplateDateRow(Workbook workbook, YearMonth selectedMonth) {
        Sheet sheet = workbook.getSheetAt(0);
        removeTemplateHeaderMergedRegions(sheet);
        Row dateRow = recreateRow(sheet, 0);
        Row headerRow = recreateRow(sheet, 1);
        CellStyle[] dateGroupStyles = createDateGroupStyles(workbook);
        setCellText(dateRow, 0, "日期");
        clearCell(dateRow, 1);
        clearCell(dateRow, 2);
        clearCell(dateRow, 3);
        clearCell(dateRow, 4);
        setCellText(headerRow, 0, "姓名");
        setCellText(headerRow, 1, "电话");
        setCellText(headerRow, 2, "产品");
        setCellText(headerRow, 3, "岗位");
        setCellText(headerRow, 4, "车间");
        for (int dateIndex = 0; dateIndex < 31; dateIndex++) {
            int cellIndex = 5 + dateIndex * 5;
            if (dateIndex < selectedMonth.lengthOfMonth()) {
                LocalDate date = selectedMonth.atDay(dateIndex + 1);
                fillDateGroup(dateRow, headerRow, cellIndex, TEMPLATE_DATE_FORMATTER.format(date),
                        dateGroupStyles[dateIndex % dateGroupStyles.length]);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, cellIndex, cellIndex + 4));
            } else {
                clearDateGroupCells(dateRow, cellIndex);
                clearDateGroupCells(headerRow, cellIndex);
            }
        }
    }

    private void fillDateGroup(Row dateRow, Row headerRow, int startCellIndex, String dateText, CellStyle style) {
        for (int offset = 0; offset < IMPORT_TEMPLATE_CHILD_HEADERS.length; offset++) {
            Cell dateCell = getOrCreateCell(dateRow, startCellIndex + offset);
            dateCell.setCellStyle(style);
            if (offset == 0) {
                dateCell.setCellValue(dateText);
            } else {
                dateCell.setBlank();
            }

            Cell headerCell = getOrCreateCell(headerRow, startCellIndex + offset);
            headerCell.setCellStyle(style);
            headerCell.setCellValue(IMPORT_TEMPLATE_CHILD_HEADERS[offset]);
        }
    }

    private Cell getOrCreateCell(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            cell = row.createCell(cellIndex);
        }
        return cell;
    }

    private CellStyle[] createDateGroupStyles(Workbook workbook) {
        return new CellStyle[]{
                createDateGroupStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE),
                createDateGroupStyle(workbook, IndexedColors.LIGHT_YELLOW)
        };
    }

    private CellStyle createDateGroupStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat((short) 0);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private Row recreateRow(Sheet sheet, int rowIndex) {
        Row existingRow = sheet.getRow(rowIndex);
        if (existingRow != null) {
            sheet.removeRow(existingRow);
        }
        return sheet.createRow(rowIndex);
    }

    private void removeTemplateHeaderMergedRegions(Sheet sheet) {
        for (int index = sheet.getNumMergedRegions() - 1; index >= 0; index--) {
            CellRangeAddress region = sheet.getMergedRegion(index);
            if (region != null && region.getFirstRow() <= 1 && region.getLastRow() >= 0
                    && region.getFirstColumn() >= 1) {
                sheet.removeMergedRegion(index);
            }
        }
    }

    private void setCellText(Row row, int cellIndex, String text) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            cell = row.createCell(cellIndex);
        }
        cell.setCellValue(text);
    }

    private void clearCell(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell != null) {
            cell.setBlank();
        }
    }

    private void clearDateGroupCells(Row dateRow, int startCellIndex) {
        for (int offset = 0; offset < 5; offset++) {
            Cell cell = dateRow.getCell(startCellIndex + offset);
            if (cell != null) {
                cell.setBlank();
            }
        }
    }

    private void clearTemplateBodyRows(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null) {
                    cell.setBlank();
                }
            }
        }
    }

    @RequestMapping("/queryCustomShiftList")
    @ResponseBody
    public successResult queryCustomShiftList() {
        successResult result = new successResult();
        try {
            List<WorkPlanCustomShiftOptionVO> options = planService.queryCustomShiftOptions();
            result.setData(options);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/queryEmployeeDayShift")
    @ResponseBody
    public successResult queryEmployeeDayShift(Long employeeId, String workDate) {
        successResult result = new successResult();
        try {
            if (employeeId == null) {
                throw new Exception("employeeId不能为空");
            }
            WorkPlanEmployeeDayShiftVO data =
                    planService.queryEmployeeDayShift(employeeId, parseDateValue(workDate, "workDate"));
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/queryEmployeeDayAssignments")
    @ResponseBody
    public successResult queryEmployeeDayAssignments(Long employeeId, String workDate) {
        successResult result = new successResult();
        try {
            if (employeeId == null) {
                throw new Exception("employeeId不能为空");
            }
            WorkPlanEmployeeDayAssignmentsVO data =
                    planService.queryEmployeeDayAssignments(employeeId, parseDateValue(workDate, "workDate"));
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/saveEmployeeDayAssignments")
    @ResponseBody
    public successResult saveEmployeeDayAssignments(@RequestBody SaveWorkPlanEmployeeDayAssignmentsBO request) {
        successResult result = new successResult();
        try {
            result.setData(planService.saveEmployeeDayAssignments(request));
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/removeEmployeeDayShift")
    @ResponseBody
    public successResult removeEmployeeDayShift(Long employeeId, String workDate) {
        successResult result = new successResult();
        try {
            if (employeeId == null) {
                throw new Exception("employeeId不能为空");
            }
            planService.removeEmployeeDayShift(employeeId, parseDateValue(workDate, "workDate"));
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/saveEmployeeDayCustomShift")
    @ResponseBody
    public successResult saveEmployeeDayCustomShift(Long employeeId, String workDate,
                                                    String shiftType,
                                                    String customStart, String customEnd,
                                                    String customShiftPeriod,
                                                    Boolean customContinuousShift,
                                                    Boolean customContinuousShiftExplicit,
                                                    String restShiftType) {
        successResult result = new successResult();
        try {
            if (employeeId == null) {
                throw new Exception("employeeId不能为空");
            }
            String effectiveShiftType = StringUtils.isBlank(shiftType) ? "custom" : shiftType;
            String effectiveShiftPeriod = StringUtils.defaultString(customShiftPeriod);
            Boolean effectiveContinuousShift = "custom".equalsIgnoreCase(effectiveShiftType)
                    && !"night".equalsIgnoreCase(effectiveShiftPeriod)
                    && Boolean.TRUE.equals(customContinuousShift);
            Boolean effectiveContinuousShiftExplicit = "custom".equalsIgnoreCase(effectiveShiftType)
                    && Boolean.TRUE.equals(customContinuousShiftExplicit);
            WorkPlanEmployeeDayShiftVO data = planService.saveEmployeeDayShift(
                    employeeId, parseDateValue(workDate, "workDate"), effectiveShiftType,
                    customStart, customEnd, customShiftPeriod, effectiveContinuousShift,
                    effectiveContinuousShiftExplicit, restShiftType);
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/removeAll")
    @ResponseBody
    public successResult RemoveAll(String IDS){
        successResult result=new successResult();
        try {
            List<Integer> IDArray= ListUtils.parse(IDS,Integer.class);
            planService.RemoveAll(IDArray);
        }
        catch(Exception ax){
            result.raiseException(ax);
        }
        return result;
    }
    @RequestMapping("/exportExcel")
    public void ExportToExcel(String GroupID,String Begin,String End, HttpServletResponse response){
        try {
            if(StringUtils.isEmpty(Begin) || StringUtils.isEmpty(End)){
                throw new Exception("请同时指明开始(Begin)和结束(End)时间!");
            }
            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
            Date begin = parseDateValue(Begin, "Begin");
            Date end = parseDateValue(End, "End");
            List<tbplanlist> tbplanlists=planRep.findAllByGroupIdAndWorkDateBetweenOrderByProductNameAsc(GroupID,begin,end);
            tbplanlists=tbplanlists.stream().sorted(Comparator.comparing(tbplanlist::getWorkDate)).collect(Collectors.toList());
            planService.fillCustomShiftMeta(tbplanlists);
            if(tbplanlists.size()>0){
                List<tbattendanceuser> users=userRep.findAll();
                List<HrmAttendanceShift>  shifts=shiftRep.findAll();
                List<tbPlanListVo> Vs=new ArrayList<>();
                for(int i=0;i<tbplanlists.size();i++){
                    tbplanlist plan=tbplanlists.get(i);
                    tbPlanListVo vo=new tbPlanListVo();
                    String classId=plan.getClassId();
                    String userId=plan.getUserId();
                    List<String> UserIDS=ListUtils.parse(userId,String.class);

                    vo.setProductName(plan.getProductName());
                    vo.setLinkName(plan.getLinkName());
                    vo.setWorkDate(format.format(plan.getWorkDate()));

                    Optional<HrmAttendanceShift> findShifts=
                            shifts.stream().filter(f->Long.toString(f.getShiftId()).equals(classId)).findFirst();
                    if(findShifts.isPresent()){
                        HrmAttendanceShift shift= findShifts.get();
                        String shiftName=shift.getShiftName();
                        String  start1=shift.getStart1();
                        String  end1  =shift.getEnd1();
                        if(StringUtils.isEmpty(start1)==false && StringUtils.isEmpty(end1)==false){
                            String[] s1=start1.split(":");
                            if(s1.length==3){
                                start1=s1[0]+":"+s1[1];
                            } else start1=s1[0];
                            String[] s2=end1.split(":");
                           if(s2.length==3) end1=s2[0]+":"+s2[1]; else end1=s2[0];
                            shiftName=shiftName+"("+start1+"~"+end1+")";
                        }
                        String  start2=shift.getStart2();
                        String  end2  =shift.getEnd2();
                        if(StringUtils.isEmpty(start2)==false && StringUtils.isEmpty(end2)==false){
                            String[] s1=start2.split(":");
                            if(s1.length==3){
                                start1=s1[0]+":"+s1[1];
                            } else start2=s1[0];
                            String[] s2=end2.split(":");
                            if(s2.length==3) end1=s2[0]+":"+s2[1]; else end2=s2[0];
                            shiftName=shiftName+"("+start2+"~"+end2+")";
                        }
                        vo.setClassName(shiftName);
                    } else if ("custom".equalsIgnoreCase(plan.getShiftType())
                            && StringUtils.isNotBlank(plan.getCustomStart())
                            && StringUtils.isNotBlank(plan.getCustomEnd())) {
                        String displayEnd = Boolean.TRUE.equals(plan.getCustomCrossDay())
                                ? "次日" + plan.getCustomEnd() : plan.getCustomEnd();
                        vo.setClassName("自定义班次(" + plan.getCustomStart() + "~" + displayEnd + ")");
                    } else continue;

                    if(UserIDS.size()>0){
                        List<tbattendanceuser> Us=
                                users.stream().filter(f->UserIDS.contains(f.getUserId())).collect(Collectors.toList());
                        if(Us.size()>0){
                            String Names=StringUtils.join(Us.stream().map(f->f.getUserName()).collect(Collectors.toList()), ',');
                            vo.setUserName(Names);
                        }
                    }
                    Vs.add(vo);
                }
                if(Vs.size()>0){
                    Map<String,Object>Info=new HashMap<>();
                    Info.put("planList",Vs);
                    String templateText=getTemplateByCode("workPlan");
                    String DD= StringUtilTool.createByTemplate(templateText,Info);
                    byte[] BB= DD.getBytes("utf-8");
                    WebFileUtils.download("生产排班("+Begin+"至"+End+")"+".xlsx",BB,response);
                } else {
                    throw new Exception("没有可导出的数据!");
                }
            } else throw new Exception("没有可导出的数据!");
        }
        catch(Exception ax){
            try {
                response.setCharacterEncoding("utf-8");
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(ax.getLocalizedMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public String getTemplateByCode(String code) throws Exception {
        String BaseDir = GlobalContext.getStaticUrl()+"\\" ;
        return StringUtilTool.readAll(BaseDir + code + ".ftl");
    }

    private List<tbplanlist> parsePlanList(String data) throws Exception {
        List<tbplanlist> plans = new ArrayList<>();
        if (StringUtils.isEmpty(data)) {
            return plans;
        }
        JSONArray array = JSON.parseArray(data);
        if (array == null || array.size() == 0) {
            return plans;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            tbplanlist plan = new tbplanlist();
            plan.setId(item.getInteger("id"));
            plan.setProductName(item.getString("productName"));
            plan.setLinkName(item.getString("linkName"));
            plan.setWorkshopName(StringUtils.trimToNull(item.getString("workshopName")));
            plan.setGroupId(item.getString("groupId"));
            plan.setClassId(item.getString("classId"));
            plan.setCustomShiftId(item.getLong("customShiftId"));
            plan.setUserId(item.getString("userId"));
            String shiftType = item.getString("shiftType");
            plan.setShiftType(shiftType);
            plan.setCustomShiftPeriod(item.getString("customShiftPeriod"));
            plan.setCustomContinuousShift(item.getBoolean("customContinuousShift"));
            plan.setCustomContinuousShiftExplicit(item.getBoolean("customContinuousShiftExplicit"));
            plan.setRestShiftType(StringUtils.isBlank(item.getString("restShiftType")) ? shiftType : item.getString("restShiftType"));
            plan.setCustomStart(StringUtils.trimToNull(item.getString("customStart")));
            plan.setCustomEnd(StringUtils.trimToNull(item.getString("customEnd")));
            plan.setCustomCrossDay(item.getBoolean("customCrossDay"));
            plan.setCreateTime(parseDateValue(item.get("createTime"), "createTime"));
            plan.setWorkDate(parseDateValue(item.get("workDate"), "workDate"));
            plans.add(plan);
        }
        return plans;
    }

    private Date parseDateValue(Object value, String fieldName) throws Exception {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        String text = String.valueOf(value).trim();
        if (StringUtils.isEmpty(text) || "null".equalsIgnoreCase(text)) {
            return null;
        }
        if (text.matches("^\\d{13}$")) {
            return new Date(Long.parseLong(text));
        }
        for (String pattern : DATE_PATTERNS) {
            Date parsed = parseByPattern(text, pattern);
            if (parsed != null) {
                return parsed;
            }
        }
        throw new Exception("字段" + fieldName + "日期格式错误: " + text + "，支持yyyy-MM-dd或yyyy-MM-dd HH:mm:ss");
    }

    private Date parseByPattern(String text, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date parsed = format.parse(text, position);
        if (parsed == null || position.getIndex() != text.length()) {
            return null;
        }
        return parsed;
    }

    @RequestMapping("/publishPlan")
    @ResponseBody
    public successResult PublishPlan(String imgData,String fileName){
        successResult result=new successResult();
        try {
            LoginUserInfo Info= CompanyContext.get();
            byte[] BB=Base64.getDecoder().decode(imgData);
            String filePath=CompanyPathUtils.getTempPath(fileName);
            File fi=new File(filePath);
            FileUtils.writeByteArrayToFile(fi,BB);
            String Token=accessToken.GetMessageToken(Info.getCompanyId());
            uploadResult upResult=uploadOne(Token,fi);

            String Content="{\n" +
                    "    \"msgtype\": \"file\",\n" +
                    "    \"file\": {\n" +
                    "        \"media_id\": \""+upResult.getMedia_id()+"\"\n" +
                    "    }\n" +
                    "}";

            String Url="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key="+Token;
            String Vx= HttpUtil.post(Url,Content);
            uploadResult rr=JSON.parseObject(Vx,uploadResult.class);
            if(rr.getErrcode()!=0){
                throw new Exception(rr.getErrmsg());
            }
        }catch(Exception ax){
            result.raiseException(ax);
        }
        return result;
    }
    uploadResult uploadOne(String Token,File file){
        Map<String,Object> param=new HashMap<>();
        param.put("file", file);
        String Url="https://qyapi.weixin.qq.com/cgi-bin/webhook/upload_media?type=file&key="+Token;
        String request=HttpUtil.post(Url,param);
        return JSON.parseObject(request,uploadResult.class);
    }
}
