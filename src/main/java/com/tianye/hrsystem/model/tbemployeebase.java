package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "tbemployeebase")
public class tbemployeebase implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    /**
     * 员工编号
     */
    @NotNull(message = "员工编号不能为空")
    @Column(name = "code")
    private String code;
    /**
     * 姓名
     */
    @NotNull(message = "员工名称不能为空")
    @Column(name = "name")
    private String name;
    /**
     * 英文名称
     */
    @Column(name = "eName")
    private String eName;
    /**
     * 手机号码
     */
    @Column(name = "mobile")
    private String mobile;

    /**
    * 所属部门
    * */
    @NotNull(message = "所属部门不能为空")
    @Column(name="DepID")
    private Integer depId;
    /**
     * 证件类型
     */
    @Column(name = "idType")
    private Integer idType;
    /**
     * 证件号码
     */
    @NotNull(message = "身份证号码不能为空")
    @Column(name = "idCode")
    private String idCode;
    /**
     * 性别
     */
    @Column(name = "sex")
    private String sex;
    /**
     * 出生日期
     */
    @Column(name = "bornDate")
    private Date bornDate;
    /**
     * 生日类型
     */
    @Column(name = "birthType")
    private String birthType;
    /**
     * 生日
     */
    @Column(name = "birthDay")
    private String birthDay;
    /**
     * 年龄
     */
    @Column(name = "age")
    private Integer age;
    /**
     * 是否已婚
     */
    @Column(name = "hasMarry")
    private Integer hasMarry;
    /**
     * 是否已育
     */
    @Column(name = "hasChild")
    private Integer hasChild;
    /**
     * 国家地区
     */
    @Column(name = "area")
    private String area;
    /**
     * 民族
     */
    @Column(name = "nation")
    private String nation;
    /**
     * 政治面貌
     */
    @Column(name = "politics")
    private String politics;
    /**
     * 籍贯
     */
    @Column(name = "nativePlace")
    private String nativePlace;
    /**
     * 户籍所在地
     */
    @Column(name = "address")
    private String address;
    /**
     * 健康状态
     */
    @Column(name = "health")
    private String health;
    /**
     * 最高学历
     */
    @Column(name = "lastEducation")
    private String lastEducation;
    /**
     * 个人邮箱
     */
    @Email
    @Column(name = "email")
    private String email;
    @Column(name = "qq")
    private String qq;
    /**
     * 微信
     */
    @Column(name = "wx")
    private String wx;
    /**
     * 现家庭住址
     */
    @Column(name = "homeAddress")
    private String homeAddress;
    /**
     * 紧急联系人
     */
    @Column(name = "argencyMan")
    private String argencyMan;
    /**
     * 紧急联系电话
     */
    @Column(name = "argencyPhone")
    private String argencyPhone;
    /**
     * 创建时间
     */
    @Column(name = "createTime")
    private Date createTime;
    /**
     * 创建人
     */
    @Column(name = "createMan")
    private Integer createMan;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }


    public Integer getIdType() {
        return idType;
    }

    public void setIdType(Integer idType) {
        this.idType = idType;
    }


    public String getIdCode() {
        return idCode;
    }

    public void setIdCode(String idCode) {
        this.idCode = idCode;
    }


    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }


    public Date getBornDate() {
        return bornDate;
    }

    public void setBornDate(Date bornDate) {
        this.bornDate = bornDate;
    }


    public String getBirthType() {
        return birthType;
    }

    public void setBirthType(String birthType) {
        this.birthType = birthType;
    }


    public String getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(String birthDay) {
        this.birthDay = birthDay;
    }


    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }


    public Integer getHasMarry() {
        return hasMarry;
    }

    public void setHasMarry(Integer hasMarry) {
        this.hasMarry = hasMarry;
    }


    public Integer getHasChild() {
        return hasChild;
    }

    public void setHasChild(Integer hasChild) {
        this.hasChild = hasChild;
    }


    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }


    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }


    public String getPolitics() {
        return politics;
    }

    public void setPolitics(String politics) {
        this.politics = politics;
    }


    public String getNativePlace() {
        return nativePlace;
    }

    public void setNativePlace(String nativePlace) {
        this.nativePlace = nativePlace;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }


    public String getLastEducation() {
        return lastEducation;
    }

    public void setLastEducation(String lastEducation) {
        this.lastEducation = lastEducation;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }


    public String getWx() {
        return wx;
    }

    public void setWx(String wx) {
        this.wx = wx;
    }


    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }


    public String getArgencyMan() {
        return argencyMan;
    }

    public void setArgencyMan(String argencyMan) {
        this.argencyMan = argencyMan;
    }


    public String getArgencyPhone() {
        return argencyPhone;
    }

    public void setArgencyPhone(String argencyPhone) {
        this.argencyPhone = argencyPhone;
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

    public String geteName() {
        return eName;
    }

    public void seteName(String eName) {
        this.eName = eName;
    }

    public Integer getDepId() {
        return depId;
    }

    public void setDepId(Integer depId) {
        this.depId = depId;
    }
}
