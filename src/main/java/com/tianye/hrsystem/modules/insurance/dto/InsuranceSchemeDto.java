package com.tianye.hrsystem.modules.insurance.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.tianye.hrsystem.common.MyPageEntity;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceProject;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class InsuranceSchemeDto extends MyPageEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "社保方案id")
    private Long schemeId;

    @ApiModelProperty(value = "方案名称")
    @NotBlank(message = "方案名称不能为空")
    private String schemeName;

    @ApiModelProperty(value = "参保城市")
    private Long city;

    @ApiModelProperty(value = "社保级别 1 总监 2 经理 3主管 4 员工")
    private int level;

    @ApiModelProperty("社保参保项目")
    private List<HrmInsuranceProjectBO> socialSecurityProjectList;

    @ApiModelProperty("公积金参保项目")
    private List<HrmInsuranceProjectBO> providentFundProjectList;

    @Override
    public String toString() {
        return "AddInsuranceSchemeBO{" +
                "schemeId=" + schemeId +
                ", schemeName='" + schemeName + '\'' +
                ", city='" + city + '\'' +
                ", socialSecurityProjectList=" + socialSecurityProjectList +
                ", providentFundProjectList=" + providentFundProjectList +
                '}';
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HrmInsuranceProjectBO {

        @ApiModelProperty(value = "项目id")
        private Long projectId;

        @ApiModelProperty(value = "1 养老保险基数 2 医疗保险基数 3 失业保险基数 4 工伤保险基数 5 生育保险基数 6 补充大病医疗保险 7 补充养老保险 8 残保险 9 社保自定义 10 公积金 11 公积金自定义")
        private Integer type;

        @ApiModelProperty(value = "项目名称")
        private String projectName;

        @ApiModelProperty(value = "默认基数")
        @DecimalMin(value = "0", message = "默认基数必须是正数")
        private BigDecimal defaultAmount;

        @ApiModelProperty(value = "公司比例")
        @DecimalMin(value = "0", message = "公司比例必须是正数")
        private BigDecimal corporateProportion;

        @ApiModelProperty(value = "个人比例")
        @DecimalMin(value = "0", message = "个人比例必须是正数")
        private BigDecimal personalProportion;

        @ApiModelProperty(value = "公司缴纳金额")
        @DecimalMin(value = "0", message = "公司缴纳金额必须是正数")
        private BigDecimal corporateAmount;

        @ApiModelProperty(value = "个人缴纳金额")
        @DecimalMin(value = "0", message = "个人缴纳金额金额必须是正数")
        private BigDecimal personalAmount;

        @Override
        public String toString() {
            return "HrmInsuranceProjectBO{" +
                    "projectId=" + projectId +
                    ", type=" + type +
                    ", projectName='" + projectName + '\'' +
                    ", defaultAmount=" + defaultAmount +
                    ", corporateProportion=" + corporateProportion +
                    ", personalProportion=" + personalProportion +
                    ", corporateAmount=" + corporateAmount +
                    ", personalAmount=" + personalAmount +
                    '}';
        }
    }
}
