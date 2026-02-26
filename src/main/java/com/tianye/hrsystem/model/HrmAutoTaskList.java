package com.tianye.hrsystem.model;


import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

/**
 * @ClassName: HrmAutoTaskList
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年10月07日 10:00
 **/

@Entity
@Table(name = "hrm_auto_taskList")
public class HrmAutoTaskList implements Serializable {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name="beginTime")
    private Date beginTime;
    @Column(name="endTime")
    private Date endTime;
    @Column(name="userProcess")
    private Boolean userProcess;
    @Column(name="groupProcess")
    private Boolean groupProcess;
    @Column(name="planProcess")
    private Boolean planProcess;
    @Column(name="detailProcess")
    private Boolean detailProcess;
    @Column(name="reportProcess")
    private Boolean reportProcess;
    @Column(name="leaveProcess")
    private Boolean leaveProcess;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Boolean getUserProcess() {
        if(userProcess==null) userProcess=false;
        return userProcess;
    }

    public void setUserProcess(Boolean userProcess) {
        this.userProcess = userProcess;
    }

    public Boolean getGroupProcess() {

        if(groupProcess==null) groupProcess=false;


        return groupProcess;
    }

    public void setGroupProcess(Boolean groupProcess) {
        this.groupProcess = groupProcess;
    }

    public Boolean getPlanProcess() {


        if(planProcess==null) planProcess=false;
        return planProcess;
    }

    public void setPlanProcess(Boolean planProcess) {
        this.planProcess = planProcess;
    }

    public Boolean getDetailProcess() {

        if(detailProcess==null) detailProcess=false;
        return detailProcess;
    }

    public void setDetailProcess(Boolean detailProcess) {
        this.detailProcess = detailProcess;
    }

    public Boolean getReportProcess() {

        if(reportProcess==null) reportProcess=false;
        return reportProcess;
    }

    public void setReportProcess(Boolean reportProcess) {
        this.reportProcess = reportProcess;
    }

    public Boolean getLeaveProcess() {

        if(leaveProcess==null) leaveProcess=false;
        return leaveProcess;
    }

    public void setLeaveProcess(Boolean leaveProcess) {
        this.leaveProcess = leaveProcess;
    }
}
