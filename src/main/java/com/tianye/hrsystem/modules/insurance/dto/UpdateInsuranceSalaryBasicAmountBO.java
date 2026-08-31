package com.tianye.hrsystem.modules.insurance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class UpdateInsuranceSalaryBasicAmountBO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("每月社保记录id")
    @JsonProperty("irecordId")
    @JsonAlias({"iRecordId", "IRecordId"})
    @JsonSerialize(using = ToStringSerializer.class)
    private Long iRecordId;

    @ApiModelProperty("员工月度社保记录id；为空时按 iRecordId 更新全部参保员工")
    @JsonProperty("iempRecordIds")
    @JsonAlias({"iEmpRecordIds", "IEmpRecordIds"})
    private List<Long> iEmpRecordIds;

    @ApiModelProperty("是否累计基本工资金额设置中的长期护理/大额医疗保险金额：0 否 1 是")
    private Integer includeSalaryBasicInsuranceAmount;
}
