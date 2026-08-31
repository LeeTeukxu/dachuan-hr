package com.tianye.hrsystem.modules.role.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryRoleTypesBO extends MyPageEntity {

    @ApiModelProperty(value = "角色ID")
    private Integer id;

    @ApiModelProperty(value = "父子级")
    private Integer pid;

    @ApiModelProperty(value = "角色名称")
    private String name;

    @ApiModelProperty(value = "是否启用")
    private Integer canUse;
}
