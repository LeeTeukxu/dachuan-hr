package com.tianye.hrsystem.entity.bo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @ClassName: AddDeptBo
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月21日 20:19
 **/
@Data
@ApiModel(value = "部门添加对象")
public class AddDeptBO {

    @TableId(value = "dept_id")
    private Long deptId;

    @ApiModelProperty(value = "父级ID 顶级部门为0")
    private Long parentId;

    @ApiModelProperty(value = "部门类型 1 公司 2 部门")
    @NotNull(message = "部门类型不能为空")
    private Integer deptType;

    @ApiModelProperty(value = "部门名称")
    @NotBlank(message = "部门名称不能为空")
    private String name;

    @ApiModelProperty(value = "部门编码")
    private String code;

    /** 部门编制人数（2026-09-10，4 部门统计编制真实化）：NULL=未配置；>=0=与在职人数对比算缺/超/已满 */
    @ApiModelProperty(value = "部门编制人数")
    private Integer planNum;

    @ApiModelProperty(value = "部门负责人ID")
    private Long mainEmployeeId;

    @ApiModelProperty(value = "分管领导")
    private Long leaderEmployeeId;

}
