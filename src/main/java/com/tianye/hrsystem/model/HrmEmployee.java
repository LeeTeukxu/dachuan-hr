package com.tianye.hrsystem.model;

import net.sf.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee")
public class HrmEmployee implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 员工id
	 */
  @Column(name = "employee_id")
  private Long employeeId;
	/**
	 * 员工姓名
	 */
  @Column(name = "employee_name")
  private String employeeName;
	/**
	 * 手机
	 */
  @Column(name = "mobile")
  private String mobile;
	/**
	 * 国家地区
	 */
  @Column(name = "country")
  private String country;
	/**
	 * 民族
	 */
  @Column(name = "nation")
  private String nation;
	/**
	 * 证件类型 1 身份证 2 港澳通行证 3 台湾通行证 4 护照 5 其他
	 */
  @Column(name = "id_type")
  private Integer idType;
	/**
	 * 证件号码
	 */
  @Column(name = "id_number")
  private String idNumber;
	/**
	 * 性别 1 男 2 女
	 */
  @Column(name = "sex")
  private Integer sex;
	/**
	 * 邮箱
	 */
  @Column(name = "email")
  private String email;
	/**
	 * 籍贯
	 */
  @Column(name = "native_place")
  private String nativePlace;
	/**
	 * 出生日期
	 */
  @Column(name = "date_of_birth")
  private Date dateOfBirth;
	/**
	 * 生日类型 1 阳历 2 农历
	 */
  @Column(name = "birthday_type")
  private Integer birthdayType;
	/**
	 * 生日 示例：0323
	 */
  @Column(name = "birthday")
  private String birthday;
	/**
	 * 年龄
	 */
  @Column(name = "age")
  private Integer age;
	/**
	 * 户籍地址
	 */
  @Column(name = "address")
  private String address;
	/**
	 * 最高学历
	 */
  @Column(name = "highest_education")
  private Integer highestEducation;
	/**
	 * 入职时间
	 */
  @Column(name = "entry_time")
  private LocalDate entryTime;
	/**
	 * 试用期 0 无试用期
	 */
  @Column(name = "probation")
  private Integer probation;
	/**
	 * 转正日期
	 */
  @Column(name = "become_time")
  private LocalDateTime becomeTime;
  @Column(name = "job_number")
  private String jobNumber;
	/**
	 * 部门ID
	 */
  @Column(name = "dept_id")
  private Long deptId;
	/**
	 * 直属上级ID
	 */
  @Column(name = "parent_id")
  private Long parentId;
	/**
	 * 职位
	 */
  @Column(name = "post")
  private String post;
	/**
	 * 岗位职级
	 */
  @Column(name = "post_level")
  private String postLevel;
	/**
	 * 工作地点
	 */
  @Column(name = "work_address")
  private String workAddress;
	/**
	 * 工作详细地址
	 */
  @Column(name = "work_detail_address")
  private String workDetailAddress;
	/**
	 * 工作城市
	 */
  @Column(name = "work_city")
  private String workCity;
	/**
	 * 招聘渠道
	 */
  @Column(name = "channel_id")
  private Long channelId;
	/**
	 * 聘用形式 1 正式 2 非正式
	 */
  @Column(name = "employment_forms")
  private Integer employmentForms;
	/**
	 * 员工状态 1正式 2试用  3实习 4兼职 5劳务 6顾问 7返聘 8外包
	 */
  @Column(name = "status")
  private Integer status;
	/**
	 * 司龄开始日期
	 */
  @Column(name = "company_age_start_time")
  private LocalDateTime companyAgeStartTime;
	/**
	 * 司龄
	 */
  @Column(name = "company_age")
  private Integer companyAge;
	/**
	 * 入职状态 1 在职 2 待入职 3 待离职 4 离职
	 */
  @Column(name = "entry_status")
  private Integer entryStatus;
	/**
	 * 候选人id
	 */
  @Column(name = "candidate_id")
  private Long candidateId;
	/**
	 * 0 未删除 1 删除
	 */
  @Column(name = "is_del")
  private Integer isDel;
    /**
     * 是否有全勤 1、有 2、没有（销售内勤有全勤，外勤没全勤）
     */
  @Column(name = "full_attendance")
  private Integer fullAttendance;
    /**
     * 是否有全勤 1、有 2、没有（销售内勤有全勤，外勤没全勤）
     */
    @Column(name = "expand_production")
    private Integer expandProduction;
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

  public Long getEmployeeId() {
    return employeeId;
  }
  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }


  public String getEmployeeName() {
    return employeeName;
  }
  public void setEmployeeName(String employeeName) {
    this.employeeName = employeeName;
  }


  public String getMobile() {
    return mobile;
  }
  public void setMobile(String mobile) {
    this.mobile = mobile;
  }


  public String getCountry() {
    return country;
  }
  public void setCountry(String country) {
    this.country = country;
  }


  public String getNation() {
    return nation;
  }
  public void setNation(String nation) {
    this.nation = nation;
  }


  public Integer getIdType() {
    return idType;
  }
  public void setIdType(Integer idType) {
    this.idType = idType;
  }


  public String getIdNumber() {
    return idNumber;
  }
  public void setIdNumber(String idNumber) {
    this.idNumber = idNumber;
  }


  public Integer getSex() {
    return sex;
  }
  public void setSex(Integer sex) {
    this.sex = sex;
  }


  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }


  public String getNativePlace() {
    return nativePlace;
  }
  public void setNativePlace(String nativePlace) {
    this.nativePlace = nativePlace;
  }


  public Date getDateOfBirth() {
    return dateOfBirth;
  }
  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }


  public Integer getBirthdayType() {
    return birthdayType;
  }
  public void setBirthdayType(Integer birthdayType) {
    this.birthdayType = birthdayType;
  }


  public String getBirthday() {
    return birthday;
  }
  public void setBirthday(String birthday) {
    this.birthday = birthday;
  }


  public Integer getAge() {
    return age;
  }
  public void setAge(Integer age) {
    this.age = age;
  }


  public String getAddress() {
    return address;
  }
  public void setAddress(String address) {
    this.address = address;
  }


  public Integer getHighestEducation() {
    return highestEducation;
  }
  public void setHighestEducation(Integer highestEducation) {
    this.highestEducation = highestEducation;
  }


  public LocalDate getEntryTime() {
    return entryTime;
  }
  public void setEntryTime(LocalDate entryTime) {
    this.entryTime = entryTime;
  }


  public Integer getProbation() {
    return probation;
  }
  public void setProbation(Integer probation) {
    this.probation = probation;
  }


  public LocalDateTime getBecomeTime() {
    return becomeTime;
  }
  public void setBecomeTime(LocalDateTime becomeTime) {
    this.becomeTime = becomeTime;
  }


  public String getJobNumber() {
    return jobNumber;
  }
  public void setJobNumber(String jobNumber) {
    this.jobNumber = jobNumber;
  }


  public Long getDeptId() {
    return deptId;
  }
  public void setDeptId(Long deptId) {
    this.deptId = deptId;
  }


  public Long getParentId() {
    return parentId;
  }
  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }


  public String getPost() {
    return post;
  }
  public void setPost(String post) {
    this.post = post;
  }


  public String getPostLevel() {
    return postLevel;
  }
  public void setPostLevel(String postLevel) {
    this.postLevel = postLevel;
  }


  public String getWorkAddress() {
    return workAddress;
  }
  public void setWorkAddress(String workAddress) {
    this.workAddress = workAddress;
  }


  public String getWorkDetailAddress() {
    return workDetailAddress;
  }
  public void setWorkDetailAddress(String workDetailAddress) {
    this.workDetailAddress = workDetailAddress;
  }


  public String getWorkCity() {
    return workCity;
  }
  public void setWorkCity(String workCity) {
    this.workCity = workCity;
  }


  public Long getChannelId() {
    return channelId;
  }
  public void setChannelId(Long channelId) {
    this.channelId = channelId;
  }


  public Integer getEmploymentForms() {
    return employmentForms;
  }
  public void setEmploymentForms(Integer employmentForms) {
    this.employmentForms = employmentForms;
  }


  public Integer getStatus() {
    return status;
  }
  public void setStatus(Integer status) {
    this.status = status;
  }


  public LocalDateTime getCompanyAgeStartTime() {
    return companyAgeStartTime;
  }
  public void setCompanyAgeStartTime(LocalDateTime companyAgeStartTime) {
    this.companyAgeStartTime = companyAgeStartTime;
  }


  public Integer getCompanyAge() {
    return companyAge;
  }
  public void setCompanyAge(Integer companyAge) {
    this.companyAge = companyAge;
  }


  public Integer getEntryStatus() {
    return entryStatus;
  }
  public void setEntryStatus(Integer entryStatus) {
    this.entryStatus = entryStatus;
  }


  public Long getCandidateId() {
    return candidateId;
  }
  public void setCandidateId(Long candidateId) {
    this.candidateId = candidateId;
  }


  public Integer getIsDel() {
    return isDel;
  }
  public void setIsDel(Integer isDel) {
    this.isDel = isDel;
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

    public Integer getFullAttendance() {
        return fullAttendance;
    }

    public void setFullAttendance(Integer fullAttendance) {
        this.fullAttendance = fullAttendance;
    }

    public Integer getExpandProduction() {
        return expandProduction;
    }

    public void setExpandProduction(Integer expandProduction) {
        this.expandProduction = expandProduction;
    }
}
