package com.tianye.hrsystem.modules.menu.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import com.tianye.hrsystem.modules.menu.entity.TbRoleMenu;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QueryRoleMenuBO extends MyPageEntity {

    @ApiModelProperty(value = "角色ID")
    private Integer roleId;

    @ApiModelProperty(value = "菜单ID")
    private Integer menuId;

    private List<TbRoleMenu> listRoleMenu;
}
