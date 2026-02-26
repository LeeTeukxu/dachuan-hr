package com.tianye.hrsystem.modules.role.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QueryRoleTypesVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id")
    private Integer Id;

    @ApiModelProperty(value = "父子级")
    private Integer pId;

    @ApiModelProperty(value = "角色名")
    private String Name;

    @ApiModelProperty(value = "是否可用")
    private Integer canUse;
}
