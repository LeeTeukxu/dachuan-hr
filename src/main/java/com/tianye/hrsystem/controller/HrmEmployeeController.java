package com.tianye.hrsystem.controller;

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
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
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
    @OperateLog(apply = ApplyEnum.HRM, object = OperateObjectEnum.HRM_EMPLOYEE, behavior = BehaviorEnum.DETERMINE_ENTRY)
    public Result confirmEntry(@RequestBody AddEmployeeFieldManageBO employeeBO) {
        OperationLog operationLog = employeeService.confirmEntry(employeeBO);
        return OperationResult.ok(operationLog);
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

    @PostMapping("/queryAllEmployeeList")
    @ApiOperation("查询所用员工(表单选择使用)")
    public Result<List<SimpleHrmEmployeeVO>> queryAllEmployeeList(@RequestParam(name = "employeeName", required = false) String employeeName) {
        List<SimpleHrmEmployeeVO> list = employeeService.queryAllEmployeeList(employeeName);
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
            ax.printStackTrace();
        }
        return Result.ok();
    }
}
