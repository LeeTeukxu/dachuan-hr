package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuerySalaryArchivesListDto extends PageEntity {

    @ApiModelProperty(value = "搜索条件")
    private String search;

    @ApiModelProperty("岗位")
    private String post;

    @ApiModelProperty("部门id")
    private Long deptId;

    @ApiModelProperty(value = "员工状态 1正式 2试用  3实习 4兼职 5劳务 6顾问 7返聘 8外包  11 在职 12 全职")
    private Integer status;

    @ApiModelProperty(value = "调薪状态 0 未定薪 1 已定薪 2 已调薪")
    private Integer changeType;

    @ApiModelProperty("员工id")
    private Long employeeId;

    @ApiModelProperty("员工id list")
    private List<Long> employeeIds;

    @ApiModelProperty("年份")
    private Integer year;

    @ApiModelProperty("月份")
    private Integer month;


    @Override
    public String toString() {
        return "QuerySalaryArchivesListDto{" +
                "search='" + search + '\'' +
                ", post='" + post + '\'' +
                ", deptId=" + deptId +
                ", status=" + status +
                ", changeType=" + changeType +
                '}';
    }
}
