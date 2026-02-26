package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tbholiday")
public class tbholiday implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "year")
  private Integer year;
  @Column(name = "date")
  private Date date;
  @Column(name = "name")
  private String name;
  @Column(name = "target")
  private String target;
  @Column(name = "holiday")
  private Boolean holiday;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public Integer getYear() {
    return year;
  }
  public void setYear(Integer year) {
    this.year = year;
  }


  public Date getDate() {
    return date;
  }
  public void setDate(Date date) {
    this.date = date;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public String getTarget() {
    return target;
  }
  public void setTarget(String target) {
    this.target = target;
  }


  public Boolean getHoliday() {
    return holiday;
  }
  public void setHoliday(Boolean holiday) {
    this.holiday = holiday;
  }

}
