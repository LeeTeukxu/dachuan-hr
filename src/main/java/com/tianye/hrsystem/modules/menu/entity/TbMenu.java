package com.tianye.hrsystem.modules.menu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 角色
 * </p>
 *
 * @author jiangyongming
 * @since 2024-05-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tbmenu")
@ApiModel(value = "TbMenu对象", description = "菜单")
public class TbMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Integer id;

    @ApiModelProperty(value = "父子级")
    private Integer pid;

    @ApiModelProperty(value = "菜单名称")
    private String name;

    @ApiModelProperty(value = "菜单链接")
    private String path;

    @ApiModelProperty(value = "是否启用(1、启用，2、停用)")
    private Integer canuse;

    @ApiModelProperty(value = "")
    private String component;

    @ApiModelProperty(value = "")
    private String redirect;

    @ApiModelProperty(value = "")
    private String icon;

    @ApiModelProperty(value = "")
    private String showmenu;
}
