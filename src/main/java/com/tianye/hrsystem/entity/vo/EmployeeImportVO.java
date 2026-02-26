package com.tianye.hrsystem.entity.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.tianye.hrsystem.common.CustomField;
import lombok.Data;

import java.util.Date;

/**
 * @ClassName: DeptImportVO
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年05月16日 21:55
 **/
@Data
public class EmployeeImportVO {
    @ExcelProperty(value = "入职状态",index =0)
    private String entryStatus;
    @ExcelProperty(value="员工类别",index=1)
    private String status;
    @ExcelIgnore
    private String jobNumber;
    @ExcelProperty(value="员工姓名",index=2)
    private String employeeName;
    @ExcelProperty(value="员工性别",index=3)
    private String sex;
    @ExcelProperty(value="所在部门",index=4)
    private String deptId;
    @ExcelProperty(value="岗位",index=5)
    private String post;
    @ExcelProperty(value="身份证号码",index=6)
    private String idNumber;
    @ExcelProperty(value="年龄",index=7)
    private Integer age;
    @CustomField(Id="1481534121625661441")
    @ExcelProperty(value="政治面貌",index=8)
    private String politicsStatus;
    @CustomField(Id="1481534121621467166")
    @ExcelProperty(value="婚姻状态",index=10)
    private String marriageStatus;
    @ExcelProperty(value="民族",index=11)
    private String nation;
    @ExcelProperty(value="籍贯",index=12)
    private String nativePlace;
    @ExcelProperty(value="户籍住址",index=13)
    private String address;
    @ExcelProperty(value="学历",index=14)
    private String highestEducation;
    @ExcelProperty(value="毕业院校",index=15)
    private String school;
    @ExcelProperty(value="专业",index=16)
    private String major;
    @ExcelIgnore
    private String dateOfBirth;
    @ExcelProperty(value="手机号码",index=19)
    private String mobile;
    @ExcelProperty(value="入职时间",index=20)
    private String  entryTime;
    @ExcelProperty(value="转正时间",index=21)
    private String  becomeTime;
    @ExcelProperty(value="离职时间",index=22)
    private String quitTime;
}
