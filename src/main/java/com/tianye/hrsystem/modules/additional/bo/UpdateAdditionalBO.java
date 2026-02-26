package com.tianye.hrsystem.modules.additional.bo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class UpdateAdditionalBO {

    @ApiModelProperty("个税专项扣除项")
    private List<Project> additionalValues;

    @Getter
    @Setter
    public static class Project {
        @TableId(value = "additional_id", type = IdType.ASSIGN_ID)
        private Long additionalId;

        @ApiModelProperty(value = "员工id")
        private Long employeeId;

        @ApiModelProperty(value = "子女教育")
        private BigDecimal childrenEducation;

        @ApiModelProperty(value = "住房租金")
        private BigDecimal housingRent;

        @ApiModelProperty(value = "住房贷款利息")
        private BigDecimal housingLoanInterest;

        @ApiModelProperty(value = "赡养老人")
        private BigDecimal supportingTheElderly;

        @ApiModelProperty(value = "继续教育")
        private BigDecimal continuingEducation;

        @ApiModelProperty(value = "养幼女")
        private BigDecimal raisingGirls;

        @ApiModelProperty(value = "年")
        private Integer year;

    }
}
