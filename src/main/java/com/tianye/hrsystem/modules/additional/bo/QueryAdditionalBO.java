package com.tianye.hrsystem.modules.additional.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryAdditionalBO extends MyPageEntity {

    @ApiModelProperty(value = "员工名称")
    private String employeeName;
}
