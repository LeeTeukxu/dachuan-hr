package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_attendance_report_data")
public class HrmAttendanceReportData implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer  id;
  @Column(name = "field_id")
  private Long fieldId;
  @Column(name = "field_name")
  private String fieldName;
  @Column(name = "value")
  private String value;
  @Column(name = "emp_id")
  private Long empId;
  @Column(name = "work_date")
  private Date workDate;
  @Column(name = "create_time")
  private Date createTime;
  @Column(name = "create_user")
  private Long createUser;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public Long getFieldId() {
    return fieldId;
  }
  public void setFieldId(Long fieldId) {
    this.fieldId = fieldId;
  }


  public String getFieldName() {
    return fieldName;
  }
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }


  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }


  public Long getEmpId() {
    return empId;
  }
  public void setEmpId(Long empId) {
    this.empId = empId;
  }


  public Date getWorkDate() {
    return workDate;
  }
  public void setWorkDate(Date workDate) {
    this.workDate = workDate;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Long getCreateUser() {
    return createUser;
  }
  public void setCreateUser(Long createUser) {
    this.createUser = createUser;
  }

}
