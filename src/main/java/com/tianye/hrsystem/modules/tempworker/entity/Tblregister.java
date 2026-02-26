package com.tianye.hrsystem.modules.tempworker.entity;

import lombok.Data;

import java.util.Date;
import java.io.Serializable;

/**
 * (Tblregister)实体类
 *
 * @author makejava
 * @since 2024-10-17 11:13:35
 */
@Data
public class Tblregister implements Serializable {
    private static final long serialVersionUID = 450016786873614289L;
    
    private Integer id;
    
    private String idcardno;
    
    private String name;
    
    private Integer sex;
    
    private Date birth;
    
    private String nation;
    
    private String liveplace;
    
    private Date expiredate;
    
    private String signorg;
    
    private String phone;
    
    private Integer status;
    
    private Date createtime;
    
    private Date lastupdatetime;
    
    private String picurl1;
    
    private String picurl2;
    
    private Integer age;

    //工号
    private String workNo;

    //序号
    private String xh;

    private String bankNo;




}

