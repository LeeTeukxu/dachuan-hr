package com.tianye.hrsystem.modules.menu.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryMenuBO extends MyPageEntity {

    @ApiModelProperty(value = "菜单名称")
    private String name;
}
