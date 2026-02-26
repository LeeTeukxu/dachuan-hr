package com.tianye.hrsystem.modules.loginuser.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

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
@TableName("tbloginuser")
@ApiModel(value = "TbLoginUser对象", description = "登陆用户")
public class TbLoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Integer id;

    @ApiModelProperty(value = "姓名")
    private String name;

    @ApiModelProperty(value = "登陆帐号")
    private String account;

    @ApiModelProperty(value = "登陆密码")
    private String password;

    @ApiModelProperty(value = "所在部门")
    private Long depid;

    @ApiModelProperty(value = "角色")
    private Integer roleid;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createtime;

    @ApiModelProperty(value = "是否可以登陆")
    private Integer canlogin;

    @ApiModelProperty(value = "登陆次数")
    private Integer logincount;

    @ApiModelProperty(value = "最后登陆日期")
    private LocalDateTime lastlogintime;
}
