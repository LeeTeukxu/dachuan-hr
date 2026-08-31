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
 * 用户看板显示偏好
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("hrm_user_dashboard_config")
@ApiModel(value = "HrmUserDashboardConfig对象", description = "用户看板显示偏好")
public class HrmUserDashboardConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty("用户id")
    private String userId;

    @ApiModelProperty("看板标识 personnel/salary/perf/flow")
    private String boardKey;

    @ApiModelProperty("配置JSON")
    private String configJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
