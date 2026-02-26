package com.tianye.hrsystem.modules.menu.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QueryMenuVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id")
    private Integer id;

    @ApiModelProperty(value = "父子级")
    private Integer pid;

    @ApiModelProperty(value = "菜单名称")
    private String name;

    @ApiModelProperty(value = "path")
    private String path;

    @ApiModelProperty(value = "是否启用(1、启用，2、停用)")
    private Integer canUse;

    @ApiModelProperty(value = "component")
    private String component;

    @ApiModelProperty(value = "redirect")
    private String redirect;

    @ApiModelProperty(value = "icon")
    private String icon;

    @ApiModelProperty(value = "showMenu")
    private String showMenu;
}
