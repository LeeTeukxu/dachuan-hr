package com.tianye.hrsystem.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 重复合同分组 VO
 */
@Data
@ApiModel(value = "DuplicateContractVO", description = "重复合同分组")
public class DuplicateContractVO {

    @ApiModelProperty("分组标识（员工ID+合同类型+开始日期+结束日期）")
    private String groupKey;

    @ApiModelProperty("员工ID")
    private Long employeeId;

    @ApiModelProperty("员工姓名")
    private String employeeName;

    @ApiModelProperty("员工电话")
    private String employeePhone;

    @ApiModelProperty("合同类型")
    private Integer contractType;

    @ApiModelProperty("合同类型名称")
    private String contractTypeName;

    @ApiModelProperty("合同开始日期")
    private LocalDate startTime;

    @ApiModelProperty("合同结束日期")
    private LocalDate endTime;

    @ApiModelProperty("重复合同数量")
    private Integer duplicateCount;

    @ApiModelProperty("合同详情列表")
    private List<ContractDetailVO> contracts;

    /**
     * 合同详情
     */
    @Data
    @ApiModel(value = "ContractDetailVO", description = "合同详情")
    public static class ContractDetailVO {

        @ApiModelProperty("合同ID")
        private Long contractId;

        @ApiModelProperty("合同编号")
        private String contractNum;

        @ApiModelProperty("合同状态")
        private Integer status;

        @ApiModelProperty("合同状态名称")
        private String statusName;

        @ApiModelProperty("签约公司")
        private String signCompany;

        @ApiModelProperty("合同签订日期")
        private LocalDate signTime;

        @ApiModelProperty("创建时间")
        private LocalDateTime createTime;

        @ApiModelProperty("备注")
        private String remarks;
    }
}