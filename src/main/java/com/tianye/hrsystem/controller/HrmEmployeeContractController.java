package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.entity.po.HrmEmployeeContract;
import com.tianye.hrsystem.entity.vo.ContractInformationVO;
import com.tianye.hrsystem.entity.vo.DuplicateContractVO;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.service.employee.IHrmEmployeeContractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 员工合同 前端控制器
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@RestController
@RequestMapping("/hrmEmployeeContract")
@Api(tags = "员工管理-员工合同接口")
public class HrmEmployeeContractController {

    private static final Logger logger = LoggerFactory.getLogger(HrmEmployeeContractController.class);

    @Autowired
    private IHrmEmployeeContractService employeeContractService;

    /**
     * 合同信息
     */
    @PostMapping("/contractInformation/{employeeId}")
    @ApiOperation("合同基本信息")
    public Result<List<ContractInformationVO>> contractInformation(@PathVariable("employeeId") Long employeeId) {
        List<ContractInformationVO> contractInformationVOList = employeeContractService.contractInformation(employeeId);
        return Result.ok(contractInformationVOList);
    }

    @PostMapping("/addContract")
    @ApiOperation("/添加合同")
    public Result addContract(@RequestBody HrmEmployeeContract employeeContract) {
        employeeContract.setContractId(null);
        employeeContractService.addOrUpdateContract(employeeContract);
        return Result.ok();
    }

    @RequestMapping("/import")
    @ResponseBody
    @ApiOperation("导入员工合同")
    public Result<Map<String, Object>> importContracts(MultipartFile file) {
        try {
            return Result.ok(employeeContractService.importContracts(file));
        } catch (Exception ax) {
            logger.error("员工合同导入失败", ax);
            return Result.error(500, ax.getMessage());
        }
    }

    @GetMapping("/downloadContractTemplate")
    @ApiOperation("下载员工合同模版")
    public void downloadContractTemplate(HttpServletResponse response) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("export/hetong_module.xlsx")) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "hetong_module.xlsx not found");
                return;
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("hetong_module.xlsx", "UTF-8"));
            response.setHeader("Set-Cookie", "fileDownload=true; path=/");
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        }
    }

    @PostMapping("/setContract")
    @ApiOperation("/修改合同")
    public Result setContract(@RequestBody HrmEmployeeContract employeeContract) {
        employeeContractService.addOrUpdateContract(employeeContract);
        return Result.ok();
    }

    @PostMapping("/deleteContract/{contractId}")
    @ApiOperation("删除合同")
    public Result deleteContract(@PathVariable("contractId") Long contractId) {
        employeeContractService.deleteContract(contractId);
        return Result.ok();
    }

    @PostMapping("/queryDuplicateContracts")
    @ApiOperation("查询重复合同列表")
    public Result<List<DuplicateContractVO>> queryDuplicateContracts() {
        try {
            return Result.ok(employeeContractService.queryDuplicateContracts());
        } catch (Exception ax) {
            logger.error("查询重复合同失败", ax);
            return Result.error(500, ax.getMessage());
        }
    }

    @PostMapping("/deleteDuplicateContracts")
    @ApiOperation("批量删除重复合同")
    public Result<Integer> deleteDuplicateContracts(@RequestBody List<Long> contractIds) {
        try {
            return Result.ok(employeeContractService.deleteDuplicateContracts(contractIds));
        } catch (Exception ax) {
            logger.error("删除重复合同失败", ax);
            return Result.error(500, ax.getMessage());
        }
    }
}

