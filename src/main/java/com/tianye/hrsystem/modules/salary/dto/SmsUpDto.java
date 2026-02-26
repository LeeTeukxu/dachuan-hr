package com.tianye.hrsystem.modules.salary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SmsUpDto {

    //员工id
    private Long employeeId;

    @ApiModelProperty("工资发放年")
    private Integer year;

    @ApiModelProperty(value = "工资发放月")
    private Integer month;

//    @ApiModelProperty(value = "工资条模板项")
//    private List<HrmSalarySlipTemplateOption> slipTemplateOption;

    @ApiModelProperty(value = "员工名称")
    private String employeeName;

    @ApiModelProperty("部门名称")
    private Long deptId;

    @ApiModelProperty(value = "发送状态 0 未发送 1 已发送")
    private Integer sendStatus;

//    @Override
//    public String toString() {
//        return "SendSalarySlipBO{" +
//                "sEmpRecordIds=" + sEmpRecordIds +
//                ", isAll=" + isAll +
//                ", hideEmpty=" + hideEmpty +
//                ", slipTemplateOption=" + slipTemplateOption +
//                ", employeeName='" + employeeName + '\'' +
//                ", deptId=" + deptId +
//                ", sendStatus=" + sendStatus +
//                '}';
//    }
}
