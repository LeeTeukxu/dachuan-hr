package com.tianye.hrsystem.modules.role.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryRoleTypesBO extends MyPageEntity {

    @ApiModelProperty(value = "角色名称")
    private String name;
}
