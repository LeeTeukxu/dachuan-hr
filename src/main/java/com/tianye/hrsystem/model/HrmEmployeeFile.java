package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_file")
public class HrmEmployeeFile implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "employee_file_id")
  private Long employeeFileId;
	/**
	 * 员工id
	 */
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * admin模块附件id
	 */
  @Column(name = "file_id")
  private Long fileId;
	/**
	 * 1 员工基本资料 2 员工档案资料 3 员工离职资料
	 */
  @Column(name = "type")
  private Integer type;
	/**
	 * 11、身份证原件 12、学历证明 13、个人证件照 14、身份证复印件 15、工资银行卡 16、社保卡 17、公积金卡 18、获奖证书 19、其他 21、劳动合同 22、入职简历 23、入职登记表 24、入职体检单 25、离职证明 26、转正申请表 27、其他
31、离职审批 32、离职证明 33 、其他

	 */
  @Column(name = "sub_type")
  private Integer subType;
	/**
	 * 创建人id
	 */
  @Column(name = "create_user_id")
  private Long createUserId;
	/**
	 * 创建时间
	 */
  @Column(name = "create_time")
  private Date createTime;
	/**
	 * 更新人id
	 */
  @Column(name = "update_user_id")
  private Long updateUserId;
	/**
	 * 更新时间
	 */
  @Column(name = "update_time")
  private Date updateTime;

  public Long getEmployeeFileId() {
    return employeeFileId;
  }
  public void setEmployeeFileId(Long employeeFileId) {
    this.employeeFileId = employeeFileId;
  }


  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public Long getFileId() {
    return fileId;
  }
  public void setFileId(Long fileId) {
    this.fileId = fileId;
  }


  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
  }


  public Integer getSubType() {
    return subType;
  }
  public void setSubType(Integer subType) {
    this.subType = subType;
  }


  public Long getCreateUserId() {
    return createUserId;
  }
  public void setCreateUserId(Long createUserId) {
    this.createUserId = createUserId;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Long getUpdateUserId() {
    return updateUserId;
  }
  public void setUpdateUserId(Long updateUserId) {
    this.updateUserId = updateUserId;
  }


  public Date getUpdateTime() {
    return updateTime;
  }
  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

}
