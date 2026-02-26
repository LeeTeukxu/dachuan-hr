package com.tianye.hrsystem.modules.tempworker.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * (Tbworkshop)实体类
 *
 * @author makejava
 * @since 2024-10-17 11:13:40
 */
@Data
public class Tbworkshop implements Serializable {
    private static final long serialVersionUID = 172385926874531738L;
    
    private Integer id;
    
    private Integer companyid;
    
    private String workshop;




}

