package com.tianye.hrsystem.modules.tempworker.entity;

import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * (Tbprice)实体类
 *
 * @author makejava
 * @since 2024-10-17 11:13:40
 */
@Data
public class Tbprice implements Serializable {
    private static final long serialVersionUID = -46354694460358020L;
    
    private Integer id;
    
    private Integer producttype;
    
    private Integer groupid;
    
    private String iden;
    
    private Double price;
    
    private Double totalweight;
    
    private Integer adduser;
    
    private Date addtime;



}

