package com.tianye.hrsystem.modules.tempworker.entity;

import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * (Tbbreed)实体类
 *
 * @author makejava
 * @since 2024-10-17 11:13:41
 */
@Data
public class Tbbreed implements Serializable {
    private static final long serialVersionUID = 225457862715867986L;
    
    private Integer id;
    
    private Integer workshopid;
    
    private String breed;
    
    private String measuringmethod;
    
    private Double beginprice;
    
    private Double endprice;
    
    private Double price;
    
    private String level;
    
    private String personlevel;
    
    private String yf;
    
    private Integer adduser;
    
    private Date addtime;

    private String WorkShop;



}

