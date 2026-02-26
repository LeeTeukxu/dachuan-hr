package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbemployeeeducation")
public class tbemployeeeducation implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "empId")
  private Integer empId;
	/**
	 * 学历
	 */
  @Column(name = "eduLevel")
  private String eduLevel;
	/**
	 * 毕业院校
	 */
  @Column(name = "school")
  private String school;
	/**
	 * 专业
	 */
  @Column(name = "proType")
  private String proType;
	/**
	 * 入学时间
	 */
  @Column(name = "schoolBegin")
  private Date schoolBegin;
	/**
	 * 毕业时间
	 */
  @Column(name = "schoolEnd")
  private Date schoolEnd;
	/**
	 * 教学方式
	 */
  @Column(name = "studyType")
  private String studyType;
	/**
	 * 是否是第一学历
	 */
  @Column(name = "isFisrtEdu")
  private Integer isFisrtEdu;
  @Column(name = "createTime")
  private Date createTime;
  @Column(name = "createMan")
  private Integer createMan;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public Integer getEmpId() {
    return empId;
  }
  public void setEmpId(Integer empId) {
    this.empId = empId;
  }


  public String getEduLevel() {
    return eduLevel;
  }
  public void setEduLevel(String eduLevel) {
    this.eduLevel = eduLevel;
  }


  public String getSchool() {
    return school;
  }
  public void setSchool(String school) {
    this.school = school;
  }


  public String getProType() {
    return proType;
  }
  public void setProType(String proType) {
    this.proType = proType;
  }


  public Date getSchoolBegin() {
    return schoolBegin;
  }
  public void setSchoolBegin(Date schoolBegin) {
    this.schoolBegin = schoolBegin;
  }


  public Date getSchoolEnd() {
    return schoolEnd;
  }
  public void setSchoolEnd(Date schoolEnd) {
    this.schoolEnd = schoolEnd;
  }


  public String getStudyType() {
    return studyType;
  }
  public void setStudyType(String studyType) {
    this.studyType = studyType;
  }


  public Integer getIsFisrtEdu() {
    return isFisrtEdu;
  }
  public void setIsFisrtEdu(Integer isFisrtEdu) {
    this.isFisrtEdu = isFisrtEdu;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Integer getCreateMan() {
    return createMan;
  }
  public void setCreateMan(Integer createMan) {
    this.createMan = createMan;
  }

}
