package com.tianye.hrsystem.modules.tempworker.entity;

import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * (Tbweight)实体类
 *
 * @author makejava
 * @since 2024-10-17 11:13:40
 */
@Data
public class Tbweight implements Serializable {
    private static final long serialVersionUID = -55405266055620487L;
    
    private Integer id;
    
    private String companycode;
    
    private String cardno;
    
    private String idno;
    
    private Double weight;
    
    private Date weighttime;

    private String weighttimeStr;
    
    private Integer producttype;

    private String price;

    private String breed;

    private String workshop;




}

