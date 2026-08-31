package com.tianye.hrsystem.entity.bo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "部门编码生成对象")
public class GenerateDeptCodeBO {

    @ApiModelProperty(value = "编辑时传当前部门ID，新增时可为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;
}
