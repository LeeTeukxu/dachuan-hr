package com.tianye.hrsystem.imple.ddTalk;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceGetupdatedataRequest;
import com.dingtalk.api.request.OapiAttendanceListRecordRequest;
import com.dingtalk.api.request.OapiAttendanceListRequest;
import com.dingtalk.api.response.OapiAttendanceGetupdatedataResponse;
import com.dingtalk.api.response.OapiAttendanceListRecordResponse;
import com.dingtalk.api.response.OapiAttendanceListResponse;
import com.tianye.hrsystem.model.tbattendancerecord;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.tbattendanceapproveRepository;
import com.tianye.hrsystem.repository.tbattendancerecordRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.service.ddTalk.IRecordManager;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.tianye.hrsystem.model.*;

/**
 * @ClassName: AttendanceRecordRefreshTask
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月15日 9:51
 **/
@Service
public class AttendanceRecordManager implements IRecordManager {
    @Autowired
    IAccessToken tokenCreator;
    @Autowired
    tbattendanceuserRepository userRep;

    @Autowired
    tbattendancerecordRepository recRep;

    @Autowired
    tbattendanceapproveRepository appRep;
    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getupdatedata");
    List<String> IDS = new ArrayList<>();

    @Override
    @Transactional
    public void GetAndSave(Date Begin,Date End) throws Exception {
        List<tbattendanceuser> users = userRep.findAll();
        List<tbattendanceuser> us = users.stream().collect(Collectors.toList());
        IDS = us.stream().map(f -> f.getUserId()).collect(Collectors.toList());
        Date Now = new Date();
        Date PreDate = DateUtils.addDays(Now, -1);
        ProcessOne(Now);
        ProcessOne(PreDate);
    }

    private void ProcessOne(Date NowDate) throws Exception {
        int userIndex = 0;
        while (true) {
            String password = tokenCreator.Refresh();
            String UserID = IDS.get(userIndex);
            OapiAttendanceGetupdatedataRequest req = new OapiAttendanceGetupdatedataRequest();
            req.setUserid(UserID);
            req.setWorkDate(NowDate);
            OapiAttendanceGetupdatedataResponse rsp = client.execute(req, password);
            if (rsp.isSuccess()) {
                OapiAttendanceGetupdatedataResponse.AtCheckInfoForOpenVo rs = rsp.getResult();
                List<OapiAttendanceGetupdatedataResponse.AtAttendanceResultForOpenVo> Rs = rs.getAttendanceResultList();
                List<OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo> As = rs.getApproveList();
                SaveAttendanceList(rs.getUserid(), rs.getWorkDate(), Rs);
                SaveApproveList(rs.getUserid(), rs.getWorkDate(), As);
            }
            userIndex++;
            if (userIndex >= IDS.size()) break;
        }
    }

    private void SaveAttendanceList(String UserID, Date WorkDate,
            List<OapiAttendanceGetupdatedataResponse.AtAttendanceResultForOpenVo> Us) {
        for (int i = 0; i < Us.size(); i++) {
            OapiAttendanceGetupdatedataResponse.AtAttendanceResultForOpenVo r = Us.get(i);
            Long Id = r.getRecordId();
            if(Id==null) {
               String timeResult=r.getTimeResult();
               String source=r.getSourceType();
               Date workDate=r.getUserCheckTime();
                Optional<tbattendancerecord> findRs=recRep.findFirstByUserIdAndTimeResultAndUserCheckTime(UserID,
                        timeResult,workDate);
                if(findRs.isPresent()){
                    Id=findRs.get().getId();
                } else {
                    Random R=new Random();
                    Id=R.nextLong();
                }
            }
            Optional<tbattendancerecord> findRs = recRep.findById(Id);
            if (findRs.isPresent() == false) {
                tbattendancerecord newR = new tbattendancerecord();
                newR.setId(Id);
                newR.setCheckType(r.getCheckType());
                newR.setGroupId(r.getGroupId());
                newR.setLocationResult(r.getLocationResult());
                newR.setOutsideRemark(r.getOutsideRemark());
                newR.setSourceType(r.getSourceType());
                newR.setLocationResult(r.getLocationResult());
                newR.setUserAddress(r.getUserAddress());
                newR.setUserId(UserID);
                newR.setTimeResult(r.getTimeResult());
                newR.setWorkDate(WorkDate);
                newR.setUserCheckTime(r.getUserCheckTime());
                newR.setPlanCheckTime(r.getPlanCheckTime());
                newR.setCreateTime(new Date());
                recRep.save(newR);
            }
        }
    }
    private void SaveApproveList(String UserID, Date WorkDate,
            List<OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo> Us) {
        for (int i = 0; i < Us.size(); i++) {

            OapiAttendanceGetupdatedataResponse.AtApproveForOpenVo r = Us.get(i);
            String IstId = r.getProcInstId();
            Optional<tbattendanceapprove> findApps = appRep.findById(IstId);
            if (findApps.isPresent() == false) {
                tbattendanceapprove app = new tbattendanceapprove();
                app.setId(IstId);
                app.setBeginTime(r.getBeginTime());
                app.setEndTime(r.getEndTime());
                app.setBizType(r.getBizType());
                app.setSubType(r.getSubType());
                app.setTagName(r.getTagName());
                app.setDurationUnit(r.getDurationUnit());
                app.setUserId(UserID);
                app.setDuration(r.getDuration());
                app.setDurationDay(toLeaveDay(r.getDuration(), r.getDurationUnit()));
                app.setWorkDate(WorkDate);
                app.setCreateTime(new Date());
                appRep.save(app);
            }
        }
    }

    /**
     * 由存储的 (时长,单位) 推导一致的"天"口径：小时→/8、分钟→/60/8、天→原值，保留 2 位小数。
     * 与 HrmAttendanceApprovalProcessInstanceParser.toDayText 口径一致，保证 durationDay 派生列自洽。
     */
    private String toLeaveDay(String duration, String unit) {
        if (duration == null || duration.trim().isEmpty()) {
            return "";
        }
        double value;
        try {
            value = Double.parseDouble(duration.trim());
        } catch (NumberFormatException ex) {
            return "";
        }
        if (value <= 0) {
            return "";
        }
        double days;
        String u = unit == null ? "" : unit.trim();
        if ("分钟".equals(u)) {
            days = value / 60.0 / 8.0;
        } else if ("天".equals(u) || "日".equals(u)) {
            days = value;
        } else {
            days = value / 8.0;
        }
        days = Math.round(days * 100.0) / 100.0;
        if (days == Math.floor(days)) {
            return String.valueOf((long) days);
        }
        return String.valueOf(days);
    }
}
