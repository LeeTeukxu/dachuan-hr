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

    @ApiModelProperty(value = "是否同步到其他企业")
    private Boolean syncToOtherCompanies;

    @ApiModelProperty(value = "要同步的企业ID列表（为空则同步到该角色下所有账号的所有企业）")
    private List<String> targetCompanyIds;
}
