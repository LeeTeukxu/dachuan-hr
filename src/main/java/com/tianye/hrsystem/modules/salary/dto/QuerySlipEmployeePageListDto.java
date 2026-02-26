package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.common.MyPageEntity;
import com.tianye.hrsystem.common.PageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySlipEmployeePageListDto extends MyPageEntity {

    @ApiModelProperty(value = "员工名称")
    private String employeeName;

    @ApiModelProperty("部门名称")
    private Long deptId;

    @ApiModelProperty(value = "发送状态 0 未发送 1 已发送")
    private Integer sendStatus;

    @Override
    public String toString() {
        return "QuerySlipEmployeePageListBO{" +
                "employeeName='" + employeeName + '\'' +
                ", deptId=" + deptId +
                ", sendStatus=" + sendStatus +
                '}';
    }
}
