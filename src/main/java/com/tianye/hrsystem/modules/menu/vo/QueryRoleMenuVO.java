package com.tianye.hrsystem.modules.menu.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryRoleMenuVO {

    @ApiModelProperty(value = "菜单")
    private String name;
}
