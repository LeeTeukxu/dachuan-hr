package com.tianye.hrsystem.modules.loginuser.bo;

import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QueryLoginUserBO extends MyPageEntity {

    @ApiModelProperty(value = "用户ID")
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
    private Integer loginCount;

    @ApiModelProperty(value = "最后登陆日期")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty(value = "公司编号")
    private String companyId;
}
