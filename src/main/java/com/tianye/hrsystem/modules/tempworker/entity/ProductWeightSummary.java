package com.tianye.hrsystem.modules.tempworker.entity;

import lombok.Data;

/**
 * 员工产品称重总计
 */
@Data
public class ProductWeightSummary
{
    //身份证号码
    private String cardno;

    //重量
    private Double weight;

    //产品重量总计
    private Double productprice;

    //产品
    private String breed;

    //车间
    private String workshop;
}
