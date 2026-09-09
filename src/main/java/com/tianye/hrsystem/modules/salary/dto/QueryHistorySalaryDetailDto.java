package com.tianye.hrsystem.modules.salary.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tianye.hrsystem.common.MyPageEntity;
import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QueryHistorySalaryDetailDto extends PageEntity {

    @JsonProperty("sRecordId")
    private Long sRecordId;

    private String employeeName;

    private String jobNumber;

    private Long deptId;

    @ApiModelProperty(value = "部门ID列表(含子部门，由service层填充)")
    @TableField(exist = false)
    private List<Long> deptIds;

    @Override
    public String toString() {
        return "QueryHistorySalaryDetailBO{" +
                "sRecordId=" + sRecordId +
                ", employeeName='" + employeeName + '\'' +
                ", jobNumber='" + jobNumber + '\'' +
                ", deptId=" + deptId +
                '}';
    }
}
