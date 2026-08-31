package com.tianye.hrsystem.modules.dashboard.entity;

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
 * 看板角色权限配置
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_dashboard_role_permission")
@ApiModel(value = "HrmDashboardRolePermission对象", description = "看板角色权限配置")
public class HrmDashboardRolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty("角色id（tbroletypes.id）")
    private String roleId;

    @ApiModelProperty("配置JSON")
    private String configJson;

    @ApiModelProperty("最后修改人")
    private String updateBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}