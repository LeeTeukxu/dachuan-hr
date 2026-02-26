package com.tianye.hrsystem.modules.salary.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.vo.OperationResult;
import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.enums.TaxType;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.salary.dto.*;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import com.tianye.hrsystem.modules.salary.service.SalaryMonthRecordServiceNew;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryMonthRecordVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryMonthRecrodButtonStatusVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryPageListVO;
import com.tianye.hrsystem.modules.salary.vo.SalaryOptionHeadVO;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hrmSalaryMonthRecord")
@Api(tags = "薪资管理-薪资管理")
@Slf4j
public class HrmSalaryMonthRecordController
{

    @Autowired
    private SalaryMonthRecordServiceNew salaryMonthRecordService;

    @Autowired
    private IHrmEmployeeService employeeService;


    @PostMapping("/querySalaryPageList")
    @ApiOperation("查询薪资列表")
    public Result<BasePage<QuerySalaryPageListVO>> querySalaryPageList(@RequestBody QuerySalaryPageListDto querySalaryPageListBO)
    {
        BasePage<QuerySalaryPageListVO> page = salaryMonthRecordService.querySalaryPageList(querySalaryPageListBO);
        return Result.ok(page);
    }

    @PostMapping("/querySalaryMonthRecordList")
    @ApiOperation("查询每月薪资记录列表")
    public Result<Page<QuerySalaryMonthRecordVO>> querySalaryMonthRecordList(@RequestBody QuerySalaryMonthRecordDto querySalaryMonthRecordDto) {
        Page<QuerySalaryMonthRecordVO> page = salaryMonthRecordService.querySalaryMonthRecordList(querySalaryMonthRecordDto);
        return Result.ok(page);
    }

    @PostMapping("/computeSalaryData")
    @ApiOperation("核算薪资数据")
    public Result computeSalaryData(@ApiParam("薪资记录id") @RequestParam("srecordId") Long srecordId,
                                    @ApiParam("是否同步社保数据") @RequestParam("isSyncInsuranceData") Boolean isSyncInsuranceData,
                                    @ApiParam("是否同步考勤数据") @RequestParam(name = "isSyncAttendanceData", defaultValue = "false") Boolean isSyncAttendanceData,
                                    @ApiParam("员工id") @RequestParam(name = "employeeId", required = false) Long employeeId
    ) {
        salaryMonthRecordService.computeSalaryData(srecordId, isSyncInsuranceData, isSyncAttendanceData, employeeId);
        return Result.ok();
    }

//    @PostMapping("/computeSalaryData")
//    @ApiOperation("核算薪资数据")
//    public Result computeSalaryData(@ApiParam("薪资记录id") @RequestParam("srecordId") Long srecordId,
//                                    @ApiParam("是否同步社保数据") @RequestParam("isSyncInsuranceData") Boolean isSyncInsuranceData,
//                                    @ApiParam("是否同步考勤数据") @RequestParam(name = "isSyncAttendanceData", defaultValue = "false") Boolean isSyncAttendanceData)
//
//     {
//        salaryMonthRecordService.computeSalaryData(srecordId, isSyncInsuranceData, isSyncAttendanceData, null);
//        return Result.ok();
//    }



    @PostMapping("/computeSalaryDataAuto")
    @ApiOperation("自动核算薪资数据")
    public Result computeSalaryDataAuto(@ApiParam("数据库名称") @RequestParam("dbName") String dbName)
    {
        //切换到指定的数据库
        LoginUserInfo Info=new LoginUserInfo();
        Info.setCompanyId(dbName);
        CompanyContext.set(Info);
        //根据最新的月薪资记录，来核算当月工资
        HrmSalaryMonthRecord hrmSalaryMonthRecord = salaryMonthRecordService.queryLastSalaryMonthRecord();
//        salaryMonthRecordService.computeSalaryData(hrmSalaryMonthRecord.getSRecordId(), true, true, null, null, null);
        return Result.ok();
    }



    @PostMapping("/queryLastSalaryMonthRecord")
    @ApiOperation("查询最新的薪资记录")
    public Result<HrmSalaryMonthRecord> queryLastSalaryMonthRecord()
    {
        HrmSalaryMonthRecord salaryMonthRecord = salaryMonthRecordService.queryLastSalaryMonthRecord();
        return Result.ok(salaryMonthRecord);
    }


    @PostMapping("/addNextMonthSalary")
    @ApiOperation("创建下月薪资表")
    public Result addNextMonthSalary() {
        OperationLog operationLog = salaryMonthRecordService.addNextMonthSalary();
        return Result.ok(operationLog);
    }

    @PostMapping("/querySalaryOptionHead")
    @ApiOperation("查询薪资项表头")
    public Result<List<SalaryOptionHeadVO>> querySalaryOptionHead() {
        List<SalaryOptionHeadVO> salaryOptionHeadVOList = salaryMonthRecordService.querySalaryOptionHead();
        return Result.ok(salaryOptionHeadVOList);
    }


    @RequestMapping(value = "/downCumulativeTaxOfLastMonthTemp")
    @ApiOperation(value = "下载上月个税累计导入模板", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downCumulativeTaxOfLastMonthTemp(HttpServletResponse response) {
        List<Map<String, Object>> mapList = salaryMonthRecordService.queryPaySalaryEmployeeListByType(1, TaxType.SALARY_TAX_TYPE);
        mapList.forEach(map -> {
            map.remove("employeeId");
            map.remove("deptId");
            map.remove("isProduceDept");
            map.remove("postLevel");
            map.remove("fullMoney");
            map.remove("status");
            map.put("累计收入额（截至上月）", "");
            map.put("累计减除费用（截至上月）", "");
            map.put("累计公积金社保扣除（截至上月）", "");
            map.put("累计已缴税额", "");
        });
        ExcelWriter writer = ExcelUtil.getWriter();
        writer.addHeaderAlias("employeeName", "员工名称");
        writer.addHeaderAlias("post", "岗位");
        writer.addHeaderAlias("jobNumber", "工号");
        writer.addHeaderAlias("deptName", "部门");
        writer.addHeaderAlias("累计收入额（截至上月）", "累计收入额（截至上月）");
        writer.addHeaderAlias("累计减除费用（截至上月）", "累计减除费用（截至上月）");
        writer.addHeaderAlias("累计公积金社保扣除（截至上月）", "累计公积金社保扣除（截至上月）");
        writer.addHeaderAlias("累计已缴税额", "累计已缴税额");
        writer.merge(8, "上月个税累计信息数据模板");
        for (int i = 0; i < 9; i++) {
            writer.setColumnWidth(i, 30);
        }
        writer.write(mapList, true);
        response.setContentType("application/octet-stream;charset=UTF-8");
        //默认Excel名称
        response.setHeader("Content-Disposition", "attachment;filename=cumulative_tax_of_last_month_temp.xls");
        try (ServletOutputStream out = response.getOutputStream()) {
            writer.flush(out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }


    @RequestMapping(value = "/downloadAdditionalDeductionTemp")
    @ApiOperation(value = "下载个税专项附加扣除累计导入模板", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAdditionalDeductionTemp(HttpServletResponse response) {
        List<Map<String, Object>> mapList = salaryMonthRecordService.queryPaySalaryEmployeeListByType(1, TaxType.SALARY_TAX_TYPE);
        mapList.forEach(map -> {
            map.remove("employeeId");
            map.remove("deptId");
            map.remove("isProduceDept");
            map.remove("postLevel");
            map.remove("fullMoney");
            map.remove("status");
            map.put("累计子女教育", "");
            map.put("累计住房租金", "");
            map.put("累计住房贷款利息", "");
            map.put("累计赡养老人", "");
            map.put("累计继续教育", "");
            map.put("累计养幼女", "");
        });
        ExcelWriter writer = ExcelUtil.getWriter();
        writer.addHeaderAlias("employeeName", "员工名称");
        writer.addHeaderAlias("post", "岗位");
        writer.addHeaderAlias("jobNumber", "工号");
        writer.addHeaderAlias("deptName", "部门");
        writer.addHeaderAlias("累计子女教育", "累计子女教育");
        writer.addHeaderAlias("累计住房租金", "累计住房租金");
        writer.addHeaderAlias("累计住房贷款利息", "累计住房贷款利息");
        writer.addHeaderAlias("累计赡养老人", "累计赡养老人");
        writer.addHeaderAlias("累计继续教育", "累计继续教育");
        writer.addHeaderAlias("累计养幼女", "累计养幼女");
        writer.merge(9, "个税专项附加扣除累计数据模板");
        for (int i = 0; i < 9; i++) {
            writer.setColumnWidth(i, 30);
        }
        writer.write(mapList, true);
        response.setContentType("application/octet-stream;charset=UTF-8");
        //默认Excel名称
        response.setHeader("Content-Disposition", "attachment;filename=additional_deduction_temp.xls");
        try (ServletOutputStream out = response.getOutputStream()) {
            writer.flush(out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }

    /**
     * 导入专项附加扣除累计数据
     * @param
     * @return
     */

    @PostMapping("/importAdditionalDeduction")
    @ApiOperation("导入专项附加扣除累计数据")
    public Result importAdditionalDeduction(@ApiParam("专项附加扣除累计") @RequestParam(name = "additionalDeductionFile", required = false) MultipartFile additionalDeductionFile)
    {
        //附加扣除项map
        Map<String, Map<Integer, String>> additionalDeductionDataMap;
        List<UpdateSalaryBO> updateSalaryBOList = new ArrayList<>();
        try
        {
            additionalDeductionDataMap = salaryMonthRecordService.resolveAdditionalDeductionData(additionalDeductionFile);
            HrmSalaryMonthRecord salaryMonthRecord = salaryMonthRecordService.queryLastSalaryMonthRecord();
            for (Map.Entry<String, Map<Integer, String>> entry : additionalDeductionDataMap.entrySet())
            {
                String jobNumber = entry.getKey();
                HrmEmployee hrmEmployee = employeeService.queryByJobNumber(jobNumber);
                Map<Integer, String> salaryValues = entry.getValue();
                //判断是否包含非0的元素,如果是，则进行更新操作
                boolean isNotContainsZero = !salaryValues.containsValue("0");
                if(isNotContainsZero)
                {
                    UpdateSalaryBO updateSalaryBO = new UpdateSalaryBO();
                    updateSalaryBO.setSalaryValues(salaryValues);
                    updateSalaryBO.setEmployeeId(hrmEmployee.getEmployeeId());
                    updateSalaryBO.setMonth(salaryMonthRecord.getMonth());
                    updateSalaryBOList.add(updateSalaryBO);
                }
            }
            salaryMonthRecordService.updateSalary(updateSalaryBOList);
            return Result.ok();

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Result.error(500,"导入失败");
        }

    }


    @PostMapping("/updateSalary")
    @ApiOperation("在线修改薪资")
    public Result updateSalary(@RequestBody List<UpdateSalaryBO> updateSalaryBOList)
    {
        OperationResult operationResult = salaryMonthRecordService.updateSalary(updateSalaryBOList);
        return Result.ok();
    }

    @PostMapping("/updateCheckStatus")
    @ApiOperation("更新状态")
    public Result updateCheckStatus(@ApiParam("状态") @RequestParam("checkStatus") Integer checkStatus,
                                    @ApiParam("年") @RequestParam("year") Integer year,
                                    @ApiParam("月") @RequestParam("month") Integer month) {
        OperationResult operationResult = salaryMonthRecordService.updateCheckStatus(checkStatus, year, month);
        return Result.ok(operationResult);
    }


    @PostMapping("/salaryAudit")
    @ApiOperation("月工资记录审核")
    public Result salaryAudit(@RequestBody SalaryAuditDto auditDto) {
       try
       {
           salaryMonthRecordService.salaryAudit(auditDto);
           return Result.ok();
       }
       catch (Exception ex)
       {
           ex.printStackTrace();
           return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"审核失败");
       }
    }

    @PostMapping("/showButtonStatus")
    @ApiOperation("按钮显示状态")
    public Result<QuerySalaryMonthRecrodButtonStatusVO> showButtonStatus(@RequestBody SalaryAuditDto auditDto) {
        QuerySalaryMonthRecrodButtonStatusVO vo = salaryMonthRecordService.showButtonStatus(auditDto);
        return Result.ok(vo);
    }

    @PostMapping("/exportSalary")
    @ApiOperation("工资导出")
    public void exportSalary(@RequestBody QuerySalaryExportDto querySalaryExportDto, HttpServletResponse response)
    {
        try
        {
            salaryMonthRecordService.exportSalaryNew(querySalaryExportDto,response);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
