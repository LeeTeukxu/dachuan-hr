package com.tianye.hrsystem.modules.insurance.controller;

import com.tianye.hrsystem.common.Result;
import com.tianye.hrsystem.modules.insurance.dto.InsuranceProjectDto;
import com.tianye.hrsystem.modules.insurance.dto.InsuranceSchemeDto;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceProjectService;
import com.tianye.hrsystem.modules.insurance.service.HrmInsuranceSchemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrmInsuranceProject")
@CrossOrigin
public class HrmInsuranceProjectController {
    @Autowired
    private HrmInsuranceSchemeService hrmInsuranceSchemeService;

    @PostMapping("/saveInsuranceProject")
    public Result saveInsuranceProject(@RequestBody InsuranceSchemeDto insuranceSchemeDto){
        hrmInsuranceSchemeService.saveInsuranceProject(insuranceSchemeDto);
        return Result.OK();
    }

}
