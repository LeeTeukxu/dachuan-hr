package com.tianye.hrsystem.modules.insurance.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class UpdateInsuranceProjectBO {

    @ApiModelProperty("员工每月社保记录id")
    private Long iEmpRecordId;

    @ApiModelProperty("员工每月社保记录id数组(批量操作)")
    private List<Long> iEmpRecordIds;

    private List<Long> tempIEmpRecordIds;

    @ApiModelProperty("参保方案id")
    private Long schemeId;

    @ApiModelProperty("修改后的参保项目")
    private List<Project> projectList;

    @Override
    public String toString() {
        return "UpdateInsuranceProjectBO{" +
                "iEmpRecordId=" + iEmpRecordId +
                ", iEmpRecordIds=" + iEmpRecordIds +
                ", schemeId=" + schemeId +
                ", projectList=" + projectList +
                '}';
    }

    @Getter
    @Setter
    public static class Project {
        @ApiModelProperty("项目id")
        private Long projectId;

        @ApiModelProperty(value = "1 养老保险基数 2 医疗保险基数 3 失业保险基数 4 工伤保险基数 5 生育保险基数 6 补充大病医疗保险 7 补充养老保险 8 残保险 9 社保自定义 10 公积金 11 公积金自定义 12 医疗长期护理保险")
        private Integer type;

        @ApiModelProperty(value = "项目名称")
        private String projectName;

        @ApiModelProperty(value = "默认基数")
        private BigDecimal defaultAmount;

        @ApiModelProperty(value = "公司比例")
        private BigDecimal corporateProportion;

        @ApiModelProperty(value = "个人比例")
        private BigDecimal personalProportion;

        @ApiModelProperty(value = "公司缴纳金额")
        private BigDecimal corporateAmount;

        @ApiModelProperty(value = "个人缴纳金额")
        private BigDecimal personalAmount;

        @ApiModelProperty(value = "是否启用：0 禁用 1 启用")
        private Integer isEnabled;

    }
}
