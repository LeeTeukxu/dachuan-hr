package com.tianye.hrsystem.imple.ddTalk;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceGetupdatedataRequest;
import com.dingtalk.api.response.OapiAttendanceGetAttendUpdateDataResponse;
import com.dingtalk.api.response.OapiAttendanceGetupdatedataResponse;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.autoTask.AttendanceGroupRefreshTask;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.HrmEmployeeLeaveRecord;
import com.tianye.hrsystem.model.HrmEmployeeOverTimeRecord;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmEmployeeLeaveRecordRepository;
import com.tianye.hrsystem.repository.hrmEmployeeOverTimeRecordRepository;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.service.ddTalk.ILeaveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName: AttendanceLeaveRecord
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年04月11日 15:56
 **/
@Service
public class AttendanceLeaveRecord implements ILeaveRecord {
    @Autowired
    hrmEmployeeLeaveRecordRepository leaveRep;
    @Autowired
    hrmEmployeeOverTimeRecordRepository overTimeRep;
    List<tbattendanceuser> users = new ArrayList<>();
    @Autowired
    StringRedisTemplate redisRep;

    @Autowired
    IAccessToken tokenCreator;
    Logger logger= LoggerFactory.getLogger(AttendanceLeaveRecord.class);
    SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd");
    String LeaveKey ="";
    @Override
    public void GetAndSave(Date WorkDate) throws ApiException {
        LoginUserInfo Info = CompanyContext.get();
        String companyId = Info.getCompanyId();
        LeaveKey = companyId + "::Leave::Record";
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getupdatedata");
        String password = tokenCreator.Refresh();
        for (int i = 0; i < users.size(); i++) {
            tbattendanceuser user = users.get(i);
            String storeKey = simple.format(WorkDate) + "::" + Long.toString(user.getEmpId());
            if (redisRep.opsForHash().hasKey(LeaveKey, storeKey) == false) {
                OapiAttendanceGetupdatedataRequest req = new OapiAttendanceGetupdatedataRequest();
                req.setUserid(user.getUserId());
                req.setWorkDate(WorkDate);
                OapiAttendanceGetupdatedataResponse rsp = client.execute(req, password);
                if (rsp.getSuccess()) {
                    OapiAttendanceGetupdatedataResponse.AtCheckInfoForOpenVo rspp= rsp.getResult();
                    List<OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo> Os = rspp.getApproveList();
                    List<OapiAttendanceGetupdatedataResponse.AtAttendanceResultForOpenVo> Cs = rspp.getAttendanceResultList();
                    if (Os.size() > 0) {
                        AddLeaveRecordOrOverTimeRecord(user,WorkDate,Os,Cs);

                    } else {
                        redisRep.opsForHash().put(LeaveKey, storeKey, "1");
                    }
                }
            }
        }
    }
    private void AddLeaveRecordOrOverTimeRecord(tbattendanceuser user,
            Date WorkDate,
            List<OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo> Os,
            List<OapiAttendanceGetupdatedataResponse.AtAttendanceResultForOpenVo> Cs ){

        String storeKey = simple.format(WorkDate) + "::" + Long.toString(user.getEmpId());
        for (int n = 0; n < Os.size(); n++) {
            OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo O = Os.get(n);
            String subType = O.getSubType();
            Long bizType=O.getBizType();
            String approveId = O.getProcInstId();
            if (bizType==1L) {//加班记录
                logger.info("开始同步{}在{}的加班记录！",user.getUserName(),simple.format(WorkDate));
                Optional<HrmEmployeeOverTimeRecord> findOvers = overTimeRep.findFirstByExamineId(approveId);
                if (findOvers.isPresent() == false) {
                    HrmEmployeeOverTimeRecord overTimeRecord = new HrmEmployeeOverTimeRecord();
                    overTimeRecord.setOverTimeId(System.currentTimeMillis());
                    overTimeRecord.setEmployeeId(user.getEmpId());
                    overTimeRecord.setExamineId(O.getProcInstId());
                    overTimeRecord.setOverTimeStartTime(O.getBeginTime());
                    overTimeRecord.setOverTimeEndTime(O.getEndTime());
                    Double During = Double.parseDouble(O.getDuration());
                    overTimeRecord.setOverTimes(During);
                    //480232663 休息日加班
                    //480232662 工作日加班

                    overTimeRecord.setOverTimeType(1);

                    Optional<OapiAttendanceGetupdatedataResponse.AtAttendanceResultForOpenVo> findBs =
                            Cs.stream().filter(f -> f.getCheckType().equals("OnDuty")).findFirst();
                    if (findBs.isPresent()) {
                        OapiAttendanceGetupdatedataResponse.AtAttendanceResultForOpenVo B = findBs.get();
                        overTimeRecord.setAttendanceTime(B.getUserCheckTime());
                    }
                    overTimeRecord.setCreateTime(new Date());
                    overTimeRecord.setCreateUserId(1L);
                    overTimeRep.save(overTimeRecord);

                    redisRep.opsForHash().put(LeaveKey, storeKey, "1");
                }
            } else {
                logger.info("开始同步{}在{}的请假记录！",user.getUserName(),simple.format(WorkDate));
                Optional<HrmEmployeeOverTimeRecord> findOvers = overTimeRep.findFirstByExamineId(approveId);
                if (findOvers.isPresent() == false) {
                    HrmEmployeeLeaveRecord leaveRecord = new HrmEmployeeLeaveRecord();
                    leaveRecord.setLeaveRecordId(System.currentTimeMillis());
                    leaveRecord.setEmployeeId(user.getEmpId());
                    leaveRecord.setLeaveStartTime(O.getBeginTime());
                    leaveRecord.setLeaveEndTime(O.getEndTime());
                    Double Days = Double.parseDouble(O.getDuration());
                    leaveRecord.setLeaveDay(Days);
                    leaveRecord.setExamineId(approveId);
                    leaveRecord.setLeaveType(O.getTagName());
                    leaveRecord.setCreateTime(new Date());
                    leaveRecord.setCreateUserId(1L);
                    leaveRep.save(leaveRecord);
                    redisRep.opsForHash().put(LeaveKey, storeKey, "1");
                }
            }
        }
    }

    @Override
    public void setUsers(List<tbattendanceuser> users) {
        this.users = users;
    }
}
