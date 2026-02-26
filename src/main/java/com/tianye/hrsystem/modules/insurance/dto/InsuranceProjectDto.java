package com.tianye.hrsystem.modules.insurance.dto;

import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceProject;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class InsuranceProjectDto {
    private Long projectId;
    private Long schemeId;
    private String schemeName;
    private Long city;
    private Integer type;
    private String projectName;
    private Double defaultAmount;
    private Double corporateProportion;
    private Double personalProportion;
    private Double corporateAmount;
    private Double personalAmount;
    private Integer level;
    private Long createUserId;
    private Date createTime;
    private Long updateUserId;
    private Date updateTime;
}
