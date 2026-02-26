package com.tianye.hrsystem.modules.loginuser.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.time.LocalDateTime;

@Getter
@Setter
public class QueryLoginUserVO {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id")
    private Long Id;

    @ApiModelProperty(value = "姓名")
    private String name;

    @ApiModelProperty(value = "登陆帐号")
    private String account;

    @ApiModelProperty(value = "登陆密码")
    private String password;

    @ApiModelProperty(value = "所在部门")
    private Long depId;

    @ApiModelProperty(value = "角色")
    private Integer roleId;

    @ApiModelProperty(name = "部门名称")
    private String deptName;

    @ApiModelProperty(name = "角色名称")
    private String roleName;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "是否可以登陆")
    private Integer canLogin;

    @ApiModelProperty(value = "登陆次数")
    private Integer loginCount;

    @ApiModelProperty(value = "最后登陆日期")
    private LocalDateTime lastLoginTime;
}
