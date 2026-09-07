package com.tianye.hrsystem.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门钉钉同步配置表
 */
@Data
@Accessors(chain = true)
@TableName("hrm_dept_sync_config")
@ApiModel(value = "HrmDeptSyncConfig对象", description = "部门钉钉同步配置表")
public class HrmDeptSyncConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty("配置键")
    private String configKey;

    @ApiModelProperty("配置值")
    private String configValue;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
