package com.tianye.hrsystem.imple.ddTalk;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceListRecordRequest;
import com.dingtalk.api.response.OapiAttendanceListRecordResponse;
import com.github.pagehelper.util.StringUtil;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.common.DDTalkResposeLogger;
import com.tianye.hrsystem.model.HrmAttendanceClock;
import com.tianye.hrsystem.model.tbattendancedetail;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceClockRepository;
import com.tianye.hrsystem.repository.tbattendancedetailRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.service.ddTalk.IDetailRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: AttendanceDetailRecord
 * @Author: 肖新民
 * @*TODO: 4.30没有记录？
 * @CreateTime: 2024年03月18日 11:28
 **/

@Service
public class AttendanceDetailRecord implements IDetailRecord {
    @Autowired
    tbattendancedetailRepository detailRep;
    @Autowired
    tbattendanceuserRepository userRep;
    @Autowired
    IAccessToken tokenCreator;
    List<tbattendanceuser> users = null;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Autowired
    hrmAttendanceClockRepository clockRep;
    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/attendance/listRecord");
    Logger logger = LoggerFactory.getLogger(AttendanceDetailRecord.class);
    SimpleDateFormat shortFormat=new SimpleDateFormat("yyyy-MM-dd");
    @Autowired
    DDTalkResposeLogger ddLogger;
    @Override
    @Transactional
    public void GetAndSave(Date Begin, Date End) throws ApiException {
        Long pageIndex = 0L;

        List<HrmAttendanceClock> Saveds = clockRep.findAllByClockTimeBetween(Begin, End);
        List<Long> EmpIDS = Saveds.stream().map(f -> f.getClockEmployeeId()).collect(Collectors.toList());
        List<String> IDS = users.stream()
                .filter(f -> EmpIDS.contains(f.getEmpId()) == false)
                .map(f -> f.getUserId()).collect(Collectors.toList());
        Map<String,Integer> Rs=new HashMap<>();
        Integer Total=0;
        Integer ServerTotal=0;
        if (IDS.size() > 0) {
            while (true) {
                String password = tokenCreator.Refresh();
                List<String> ID = IDS.stream().skip(pageIndex * 50).limit(50).collect(Collectors.toList());
                if (ID.size() == 0) break;
                OapiAttendanceListRecordRequest req = new OapiAttendanceListRecordRequest();
                req.setUserIds(ID);
                req.setCheckDateFrom(format.format(Begin));
                req.setCheckDateTo(format.format(End));
                OapiAttendanceListRecordResponse rsp = client.execute(req, password);
                ddLogger.Info(rsp,((DefaultDingTalkClient)client).getRequestUrl(),Begin,AttendanceDetailRecord.class);
                if (rsp.isSuccess()) {
                    List<HrmAttendanceClock> cs=new ArrayList<>();
                    List<tbattendancedetail> ds=new ArrayList<>();
                    List<OapiAttendanceListRecordResponse.Recordresult> rs = rsp.getRecordresult();
                    ServerTotal+=rs.size();
                    for (int i = 0; i < rs.size(); i++) {
                        OapiAttendanceListRecordResponse.Recordresult r = rs.get(i);
                        Long Id = r.getId();
                        if (Id == null) continue;

                        String userId = r.getUserId();
                        Long empId = 0L;
                        String empName="";
                        Optional<tbattendanceuser> findUsers = users.stream().filter(f -> f.getUserId().equals(userId)).findFirst();
                        if (findUsers.isPresent()) {
                            empId = findUsers.get().getEmpId();
                            empName=findUsers.get().getUserName();

                            if(Rs.containsKey(empName)==false){
                                Rs.put(empName,1);
                            } else {
                                Integer N=Rs.get(empName)+1;
                                Rs.replace(empName,N);
                            }
                        }
                        Optional<HrmAttendanceClock> findClocks = clockRep.findById(Id);
                        if (findClocks.isPresent() == false) {
                            HrmAttendanceClock newOne = new HrmAttendanceClock();
                            newOne.setClockId(Id);
                            newOne.setClockTime(r.getUserCheckTime());
                            String CheckType = r.getCheckType();

                            newOne.setClassId(r.getClassId());
                            newOne.setPlanId(r.getPlanId());
                            if(StringUtil.isEmpty(CheckType)==false){
                                //上班还是下班。
                                if (CheckType.toUpperCase().equals("ONDUTY")) {
                                    newOne.setClockType(1);//上 班
                                } else newOne.setClockType(2);//下班
                            } else {
                                continue;
                            }

                            //计划打卡时间
                            newOne.setAttendanceTime(r.getPlanCheckTime());

                            //打卡方式
                            String sourceType = r.getSourceType();
                            if (StringUtil.isEmpty(sourceType) == false) {
                                sourceType = sourceType.toUpperCase();
                                if (sourceType.equals("USER")) {
                                    newOne.setType(1);
                                } else if (sourceType.equals("AUTO_CHECK")) {
                                    newOne.setType(3);
                                } else newOne.setType(2);
                            }

                            //打卡结果
                            String timeResult = r.getTimeResult();
                            if (timeResult.equals("Normal")) {
                                newOne.setClockStatus(0);
                            } else if (timeResult.equals("Early")) {
                                newOne.setClockStatus(2);//早退
                            } else if (timeResult.equals("Late") || timeResult.equals("SeriousLate")) {
                                newOne.setClockStatus(1);//迟到
                            } else if (timeResult.equals("Absenteeism")) {
                                newOne.setClockStatus(3);//旷工
                            } else if (timeResult.equals("NotSigned")) {
                                newOne.setClockStatus(5);//未打卡
                            }
                            

                            //打卡阶段 1 2 3,最多只能打三次卡
                            List<Long> my = rs.stream().filter(f -> f.getUserId().equals(userId))
                                    .filter(f->StringUtil.isEmpty(f.getCheckType())==false)
                                    .filter(f -> f.getCheckType().equals(CheckType))
                                    .sorted(Comparator.comparingLong(f -> f.getUserCheckTime().getTime()))
                                    .map(f -> f.getId()).collect(Collectors.toList());
                            if(my.size()>1){
                                newOne.setClockStage(my.indexOf(r.getId()) + 1);
                            } else newOne.setClockStage(1);
                            
                            newOne.setAddress(r.getUserAddress());
                            newOne.setLng(r.getUserLongitude());
                            newOne.setLat(r.getUserLatitude());
                            newOne.setSsid(r.getUserSsid());
                            newOne.setMac(r.getUserMacAddr());
                            newOne.setWorkDate(r.getWorkDate());
                            //是否外勤打卡
                            Integer X = 0;
                            if (r.getLocationResult().equals("Outside")) X = 1;
                            newOne.setIsOutWork(X);
                            newOne.setRemark(r.getOutsideRemark());
                            newOne.setClockEmployeeId(empId);
                            cs.add(newOne);
                        }

                        Optional<tbattendancedetail> findRs = detailRep.findById(Id);
                        if (findRs.isPresent() == false) {
                            tbattendancedetail newR = new tbattendancedetail();
                            newR.setId(Id);
                            if(StringUtil.isEmpty(r.getCheckType())) continue;
                            newR.setCheckType(r.getCheckType());
                            newR.setGroupId(r.getGroupId());
                            newR.setLocationResult(r.getLocationResult());
                            newR.setOutsideRemark(r.getOutsideRemark());
                            newR.setSourceType(r.getSourceType());
                            newR.setLocationResult(r.getLocationResult());
                            newR.setUserAddress(r.getUserAddress());
                            newR.setUserId(r.getUserId());
                            newR.setTimeResult(r.getTimeResult());
                            newR.setWorkDate(r.getWorkDate());
                            newR.setUserLatitude(r.getUserLatitude());
                            newR.setUserLongitude(r.getUserLongitude());
                            newR.setUserCheckTime(r.getUserCheckTime());
                            newR.setPlanCheckTime(r.getPlanCheckTime());
                            newR.setBaseCheckTime(r.getBaseCheckTime());
                            newR.setDeviceId(r.getDeviceId());
                            newR.setCreateTime(new Date());
                            newR.setEmpId(empId);
                            ds.add(newR);
                        }
                    }
                    if(cs.size()>0){
                        clockRep.saveAll(cs);
                        logger.info("保存了"+Integer.toString(cs.size())+"条打卡记录");
                        Total+=cs.size();
                    }
                    if(ds.size()>0) detailRep.saveAll(ds);
                    rs=null;
                } else {
                    logger.info("服务器返回结果为失败:"+rsp.getErrmsg());
                    throw new ApiException(rsp.getErrmsg());
                }
                rsp=null;
                pageIndex++;
                if(Rs.size()>0){
                    logger.info("服务器返回:"+ Integer.toString(Rs.size())+"条记录，实际保存:"+Integer.toString(Total)+"条记录!");
                }
            }
        } else {
            logger.info("没有可进行保存的内容!");
        }

    }

    public void GetAndSave(String EmpID,Date Begin, Date End) throws ApiException {
        Long pageIndex = 0L;
        List<Long> EmpIDS = Arrays.stream(EmpID.split(",")).map(f->Long.parseLong(f)).collect(Collectors.toList());



        List<String> IDS = users.stream()
                .filter(f -> EmpIDS.contains(f.getEmpId()) == true)
                .map(f -> f.getUserId()).collect(Collectors.toList());
        Map<String,Integer> Rs=new HashMap<>();
        Integer Total=0;
        if (IDS.size() > 0) {
            while (true) {
                String password = tokenCreator.Refresh();
                List<String> ID = IDS.stream().skip(pageIndex * 50).limit(50).collect(Collectors.toList());
                if (ID.size() == 0) break;
                OapiAttendanceListRecordRequest req = new OapiAttendanceListRecordRequest();
                req.setUserIds(ID);
                req.setCheckDateFrom(format.format(Begin));
                req.setCheckDateTo(format.format(End));
                OapiAttendanceListRecordResponse rsp = client.execute(req, password);
                ddLogger.Info(rsp,((DefaultDingTalkClient)client).getRequestUrl(),Begin,AttendanceDetailRecord.class);
                if (rsp.isSuccess()) {
                    List<HrmAttendanceClock> cs=new ArrayList<>();
                    List<tbattendancedetail> ds=new ArrayList<>();
                    List<OapiAttendanceListRecordResponse.Recordresult> rs = rsp.getRecordresult();
                    for (int i = 0; i < rs.size(); i++) {
                        OapiAttendanceListRecordResponse.Recordresult r = rs.get(i);
                        Long Id = r.getId();
                        if (Id == null) continue;

                        String userId = r.getUserId();
                        Long empId = 0L;
                        String empName="";
                        Optional<tbattendanceuser> findUsers = users.stream().filter(f -> f.getUserId().equals(userId)).findFirst();
                        if (findUsers.isPresent()) {
                            empId = findUsers.get().getEmpId();
                            empName=findUsers.get().getUserName();

                            if(Rs.containsKey(empName)==false){
                                Rs.put(empName,1);
                            } else {
                                Integer N=Rs.get(empName)+1;
                                Rs.replace(empName,N);
                            }
                            logger.info("加入了"+empName+shortFormat.format(r.getUserCheckTime())+"的打卡记录！");
                        }
                        Optional<HrmAttendanceClock> findClocks = clockRep.findById(Id);
                        if (findClocks.isPresent() == false) {
                            HrmAttendanceClock newOne = new HrmAttendanceClock();
                            newOne.setClockId(Id);
                            newOne.setClockTime(r.getUserCheckTime());
                            String CheckType = r.getCheckType();

                            newOne.setClassId(r.getClassId());
                            newOne.setPlanId(r.getPlanId());
                            if(StringUtil.isEmpty(CheckType)==false){
                                //上班还是下班。
                                if (CheckType.toUpperCase().equals("ONDUTY")) {
                                    newOne.setClockType(1);//上 班
                                } else newOne.setClockType(2);//下班
                            } else {
                                continue;
                            }

                            //计划打卡时间
                            newOne.setAttendanceTime(r.getPlanCheckTime());

                            //打卡方式
                            String sourceType = r.getSourceType();
                            if (StringUtil.isEmpty(sourceType) == false) {
                                sourceType = sourceType.toUpperCase();
                                if (sourceType.equals("USER")) {
                                    newOne.setType(1);
                                } else if (sourceType.equals("AUTO_CHECK")) {
                                    newOne.setType(3);
                                } else newOne.setType(2);
                            }

                            //打卡结果
                            String timeResult = r.getTimeResult();
                            if (timeResult.equals("Normal")) {
                                newOne.setClockStatus(0);
                            } else if (timeResult.equals("Early")) {
                                newOne.setClockStatus(2);//早退
                            } else if (timeResult.equals("Late") || timeResult.equals("SeriousLate")) {
                                newOne.setClockStatus(1);//迟到
                            } else if (timeResult.equals("Absenteeism")) {
                                newOne.setClockStatus(3);//旷工
                            } else if (timeResult.equals("NotSigned")) {
                                newOne.setClockStatus(5);//未打卡
                            }


                            //打卡阶段 1 2 3,最多只能打三次卡
                            List<Long> my = rs.stream().filter(f -> f.getUserId().equals(userId))
                                    .filter(f->StringUtil.isEmpty(f.getCheckType())==false)
                                    .filter(f -> f.getCheckType().equals(CheckType))
                                    .sorted(Comparator.comparingLong(f -> f.getUserCheckTime().getTime()))
                                    .map(f -> f.getId()).collect(Collectors.toList());
                            if(my.size()>1){
                                newOne.setClockStage(my.indexOf(r.getId()) + 1);
                            } else newOne.setClockStage(1);

                            newOne.setAddress(r.getUserAddress());
                            newOne.setLng(r.getUserLongitude());
                            newOne.setLat(r.getUserLatitude());
                            newOne.setSsid(r.getUserSsid());
                            newOne.setMac(r.getUserMacAddr());
                            newOne.setWorkDate(r.getWorkDate());
                            //是否外勤打卡
                            Integer X = 0;
                            if (r.getLocationResult().equals("Outside")) X = 1;
                            newOne.setIsOutWork(X);
                            newOne.setRemark(r.getOutsideRemark());
                            newOne.setClockEmployeeId(empId);
                            cs.add(newOne);
                        }

                        Optional<tbattendancedetail> findRs = detailRep.findById(Id);
                        if (findRs.isPresent() == false) {
                            tbattendancedetail newR = new tbattendancedetail();
                            newR.setId(Id);
                            if(StringUtil.isEmpty(r.getCheckType())) continue;
                            newR.setCheckType(r.getCheckType());
                            newR.setGroupId(r.getGroupId());
                            newR.setLocationResult(r.getLocationResult());
                            newR.setOutsideRemark(r.getOutsideRemark());
                            newR.setSourceType(r.getSourceType());
                            newR.setLocationResult(r.getLocationResult());
                            newR.setUserAddress(r.getUserAddress());
                            newR.setUserId(r.getUserId());
                            newR.setTimeResult(r.getTimeResult());
                            newR.setWorkDate(r.getWorkDate());
                            newR.setUserLatitude(r.getUserLatitude());
                            newR.setUserLongitude(r.getUserLongitude());
                            newR.setUserCheckTime(r.getUserCheckTime());
                            newR.setPlanCheckTime(r.getPlanCheckTime());
                            newR.setBaseCheckTime(r.getBaseCheckTime());
                            newR.setDeviceId(r.getDeviceId());
                            newR.setCreateTime(new Date());
                            newR.setEmpId(empId);
                            ds.add(newR);
                        }
                    }
                    if(cs.size()>0){
                        clockRep.saveAll(cs);
                        logger.info("保存了"+Integer.toString(cs.size())+"条打卡记录");
                        Total+=cs.size();
                    }
                    if(ds.size()>0) detailRep.saveAll(ds);
                } else {
                    logger.info("服务器返回结果为失败:"+rsp.getErrmsg());
                    throw new ApiException(rsp.getErrmsg());
                }
                pageIndex++;
                if(Rs.size()>0){
                    logger.info("服务器返回:"+ Integer.toString(Rs.size())+"条记录，实际保存:"+Integer.toString(Total)+"条记录!");
                }
            }
        } else {
            logger.info("没有可进行保存的内容!");
        }
    }

    public void setUsers(List<tbattendanceuser> users) {
        this.users = users;
    }
}
