package com.tianye.hrsystem.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据字典表
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tbdictdata")
@ApiModel(value = "TbDictData对象", description = "数据字典表表")
public class TbDictData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer id;

    @ApiModelProperty(value = "pid")
    private Integer pid;

    @ApiModelProperty(value = "dtid")
    private Integer dtid;

    @ApiModelProperty(value = "sn")
    private Integer sn;

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "是否可用")
    private Integer canUse;

    @ApiModelProperty(value = "创建人")
    private Integer createMan;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

}
