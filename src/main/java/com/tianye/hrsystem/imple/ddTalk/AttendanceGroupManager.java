package com.tianye.hrsystem.imple.ddTalk;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceGetsimplegroupsRequest;
import com.dingtalk.api.request.OapiAttendanceGroupAddRequest;
import com.dingtalk.api.request.OapiAttendanceGroupsIdtokeyRequest;
import com.dingtalk.api.request.OapiAttendanceShiftQueryRequest;
import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import com.dingtalk.api.response.OapiAttendanceGroupsIdtokeyResponse;
import com.dingtalk.api.response.OapiAttendanceShiftQueryResponse;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.common.DDTalkResposeLogger;
import com.tianye.hrsystem.common.DateTimeUtils;
import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.repository.*;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.service.ddTalk.IGroupManager;
import com.tianye.hrsystem.util.MyDateUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @ClassName: AttendanceGroupManager
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月14日 18:11
 **/
@Service
public class AttendanceGroupManager implements IGroupManager {
    Logger logger = LoggerFactory.getLogger(AttendanceGroupManager.class);

    @Autowired
    IAccessToken tokenCreator;

    @Autowired
    hrmAttendanceGroupRepository groupRep;
    @Autowired
    hrmAttendanceGroupRelationDeptRepository depRelRep;
    @Autowired
    hrmAttendanceGroupRelationEmployeeRepository empRelRep;

    @Autowired
    hrmDeptRepository deptRep;

    @Autowired
    hrmEmployeeRepository empRep;
    @Autowired
    hrmAttendanceShiftRepository shiftRep;

    SimpleDateFormat timeFormat=new SimpleDateFormat("HH:mm:ss");

    @Autowired
    DDTalkResposeLogger ddLogger;
    @Autowired
    MyDateUtils dateUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void GetAndSave() throws ApiException {
        Long Offset = 0L;
        groupRep.deleteAll();
        depRelRep.deleteAll();
        empRelRep.deleteAll();
        while (true) {
            String token = tokenCreator.Refresh();
            List<HrmAttendanceGroup> Groups = new ArrayList<>();
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getsimplegroups");
            OapiAttendanceGetsimplegroupsRequest req = new OapiAttendanceGetsimplegroupsRequest();
            req.setOffset(Offset);
            req.setSize(10L);
            OapiAttendanceGetsimplegroupsResponse rsp = client.execute(req, token);

            Date Begin=dateUtils.getCurrent();
            ddLogger.Info(rsp,((DefaultDingTalkClient)client).getRequestUrl(),Begin,AttendanceGroupManager.class);

            if (rsp.isSuccess()) {
                OapiAttendanceGetsimplegroupsResponse.AtGroupListForTopVo Vs = rsp.getResult();
                List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> Ds = Vs.getGroups();
                if (Ds.size() > 0) {

                    for (int i = 0; i < Ds.size(); i++) {
                        OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo vo = Ds.get(i);
                        Long groupId = vo.getGroupId();
                        HrmAttendanceGroup newOne = new HrmAttendanceGroup();
                        newOne.setAttendanceGroupId(groupId);
                        newOne.setName(vo.getGroupName());
                        newOne.setIsRest(1);
                        newOne.setIsDefaultSetting(vo.getIsDefault() == true ? 1 : 0);
                        newOne.setCreateTime(new Date());
                        newOne.setEffectTime(new Date());
                        newOne.setCreateUserId(1L);
                        groupRep.save(newOne);
                        logger.info("添加了考勤组:"+vo.getGroupName());
                        List<String> depNameList = vo.getDeptNameList();
                        if(depNameList==null || depNameList.size()==0) {
                            continue;
                        }
                        AddGroupRelationToDept(groupId,depNameList);

                        List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> ys= vo.getSelectedClass();
                        AddAttendanceShift(groupId,vo,ys);
                    }
                }
                Offset += Ds.size();
                if (Vs.getHasMore() == false) break;
            }
        }

    }

    public String GetGroupKeyByID(String OwnerID, Long GroupID) throws ApiException {
        String token = tokenCreator.Refresh();
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/groups/idtokey");
        OapiAttendanceGroupsIdtokeyRequest req = new OapiAttendanceGroupsIdtokeyRequest();
        req.setOpUserId(OwnerID);
        req.setGroupId(GroupID);
        OapiAttendanceGroupsIdtokeyResponse rsp = client.execute(req, token);
        if (rsp.isSuccess()) {
            return rsp.getResult();
        } else return "";
    }
    private void AddGroupRelationToDept(Long GroupID, List<String> depNameList)throws  ApiException {
        List<HrmAttendanceGroupRelationDept> DDS = new ArrayList<>();
        for (int i = 0; i < depNameList.size(); i++) {
            String depName = depNameList.get(i);
            HrmDept hm = null;
            List<HrmDept> Deps = deptRep.findAllByName(depName);
            if (Deps.size() == 1) {
                hm = Deps.get(0);
            } else if (Deps.size() > 1) {
                Deps.sort(Comparator.comparingLong(f -> -f.getParentId()));
                hm = Deps.get(0);
            }
            if(hm!=null){
                HrmAttendanceGroupRelationDept newOne = new HrmAttendanceGroupRelationDept();
                newOne.setAttendanceGroupRelationDeptId(System.currentTimeMillis());
                newOne.setAttendanceGroupId(GroupID);
                newOne.setDeptId(hm.getDeptId());
                newOne.setEffectTime(new Date());
                newOne.setCreateTime(new Date());
                newOne.setCreateUserId(1L);
                DDS.add(newOne);
                AddGroupRelationToEmployee(GroupID,hm.getDeptId());
            }
        }
        if (DDS.size() > 0) {
            depRelRep.saveAll(DDS);
        }
    }
    private void AddGroupRelationToEmployee(Long GroupID, Long DeptID) {
        List<HrmAttendanceGroupRelationEmployee> Vs = new ArrayList<>();
        List<HrmEmployee> Ems = empRep.findAllByDeptId(DeptID);
        for (int i = 0; i < Ems.size(); i++) {
            HrmEmployee em = Ems.get(i);

            HrmAttendanceGroupRelationEmployee ee = new HrmAttendanceGroupRelationEmployee();
            ee.setAttendanceGroupRelationEmployeeId(System.currentTimeMillis());
            ee.setAttendanceGroupId(GroupID);
            ee.setEmployeeId(em.getEmployeeId());
            ee.setEffectTime(new Date());
            ee.setCreateTime(new Date());
            ee.setCreateUserId(1L);
            empRelRep.save(ee);
        }
    }
    private void AddAttendanceShift(Long GroupID,
            OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo groupInfo,
            List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> Shifts) throws  ApiException{
        shiftRep.deleteAllByGroupId(GroupID);
        List<HrmAttendanceShift> Ss=new ArrayList<>();
        for(int i=0;i<Shifts.size();i++){
            OapiAttendanceGetsimplegroupsResponse.AtClassVo shift=Shifts.get(i);
            Long classId=shift.getClassId();
            String className=shift.getClassName();
            String shiftType=groupInfo.getType();
            HrmAttendanceShift newOne=new HrmAttendanceShift();
            newOne.setShiftId(classId);
            newOne.setShiftName(className);

            if(shiftType.equals("NONE")){
                newOne.setShiftType(0);
            } else {
                newOne.setShiftType(shiftType.equals("FIXED")==true?1:2);
            }
            newOne.setGroupId(GroupID);
            newOne.setEffectTime(new Date());
            newOne.setCreateTime(new Date());
            newOne.setCreateUserId(1L);

            OapiAttendanceGetsimplegroupsResponse.ClassSettingVo setting= shift.getSetting();
            newOne.setShiftHours(Math.toIntExact(setting.getWorkTimeMinutes()/60));
            OapiAttendanceGetsimplegroupsResponse.AtTimeVo beginVo=setting.getRestBeginTime();
            OapiAttendanceGetsimplegroupsResponse.AtTimeVo endVo=setting.getRestEndTime();
            if(beginVo!=null && endVo!=null){
                newOne.setRestStartTime(timeFormat.format(beginVo.getCheckTime()));
                newOne.setRestEndTime(timeFormat.format(endVo.getCheckTime()));
                newOne.setRestTimeStatus(1);
            } else {
                newOne.setRestTimeStatus(0);
            }
            int  LateMinutes=Math.toIntExact(setting.getPermitLateMinutes());
            List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> Sections= shift.getSections();

            OapiAttendanceShiftQueryResponse.TopShiftVo detail=GetDetail(classId);
            if(detail==null){
                newOne=FillSomeInfo(newOne,LateMinutes,Sections);
            } else {
               List<OapiAttendanceShiftQueryResponse.TopSectionVo> hs=  detail.getSections();
               newOne=FillSomeInfo1(newOne,hs);
            }
            Ss.add(newOne);
            logger.info("为"+groupInfo.getGroupName()+"下添加了考勤班次:"+newOne.getShiftName());
        }
        if(Ss.size()>0) shiftRep.saveAll(Ss);
    }
    private HrmAttendanceShift FillSomeInfo(HrmAttendanceShift newOne,
            Integer LateMinutes,List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> Sections){
        for(int n=0;n<Sections.size();n++){
            OapiAttendanceGetsimplegroupsResponse.AtSectionVo section=Sections.get(n);
            List<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> times= section.getTimes();
            Optional<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> onOnes=
                    times.stream().filter(f->f.getCheckType().equals("OnDuty")).findFirst();
            Optional<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO>  offOnes=
                    times.stream().filter(f->f.getCheckType().equals("OffDuty")).findFirst();
            if(onOnes.isPresent() && offOnes.isPresent()){
                Date onTime=onOnes.get().getCheckTime();
                Date offTime=offOnes.get().getCheckTime();
                if(n==0){
                    newOne.setStart1(timeFormat.format(onTime));
                    newOne.setEnd1(timeFormat.format(offTime));
                    newOne.setLateCard1(timeFormat.format(DateUtils.addMinutes(onTime,LateMinutes)));

                }
                else if(n==1){
                    newOne.setStart2(timeFormat.format(onTime));
                    newOne.setEnd2(timeFormat.format(offTime));
                    newOne.setLateCard2(timeFormat.format(DateUtils.addMinutes(onTime,LateMinutes)));
                }
                else if(n==2){
                    newOne.setStart3(timeFormat.format(onTime));
                    newOne.setEnd3(timeFormat.format(offTime));
                    newOne.setLateCard3(timeFormat.format(DateUtils.addMinutes(onTime,LateMinutes)));
                }
            }
        }
        return newOne;
    }
    private HrmAttendanceShift FillSomeInfo1(HrmAttendanceShift newOne,
            List<OapiAttendanceShiftQueryResponse.TopSectionVo> Sections){
        for(int n=0;n<Sections.size();n++){
            OapiAttendanceShiftQueryResponse.TopSectionVo section=Sections.get(n);
            List<OapiAttendanceShiftQueryResponse.TopPunchVo> times= section.getPunches();
            Optional<OapiAttendanceShiftQueryResponse.TopPunchVo> onOnes=
                    times.stream().filter(f->f.getCheckType().equals("OnDuty")).findFirst();
            Optional<OapiAttendanceShiftQueryResponse.TopPunchVo>  offOnes=
                    times.stream().filter(f->f.getCheckType().equals("OffDuty")).findFirst();
            if(onOnes.isPresent() && offOnes.isPresent()){
                OapiAttendanceShiftQueryResponse.TopPunchVo onOne=onOnes.get();
                OapiAttendanceShiftQueryResponse.TopPunchVo offOne=offOnes.get();

                int  onBeginMin=Math.toIntExact(onOne.getBeginMin()==null?0L:onOne.getBeginMin());//允许的最早提前打卡时间，分钟为单位。
                int  onEndMin=Math.toIntExact(onOne.getEndMin()==null?0L: onOne.getEndMin());//允许的最晚的打卡时间。

                int  offBeginMin=Math.toIntExact(offOne.getBeginMin()==null?0L:offOne.getBeginMin());//允许的最早提前打卡时间，分钟为单位。
                int  offEndMin=Math.toIntExact(offOne.getEndMin()==null?0L: offOne.getEndMin());//允许的最晚的打卡时间。

                Date onTime=onOne.getCheckTime();//打卡时间
                Date offTime=offOne.getCheckTime();//打卡时间

                if(n==0){
                    newOne.setStart1(timeFormat.format(onTime));
                    newOne.setEnd1(timeFormat.format(offTime));

                    newOne.setAdvanceCard1(timeFormat.format(DateUtils.addMinutes(onTime,-1*onBeginMin)));//上班最早打卡时间
                    newOne.setLateCard1(timeFormat.format(DateUtils.addMinutes(onTime,onEndMin)));//上班最晚打卡时间

                    newOne.setEarlyCard1(timeFormat.format(DateUtils.addMinutes(offTime,-1*offBeginMin)));//下班最早打卡时间1
                    newOne.setPostponeCard1(timeFormat.format(DateUtils.addMinutes(offTime,offEndMin)));
                }
                else if(n==1){
                    newOne.setStart2(timeFormat.format(onTime));
                    newOne.setEnd2(timeFormat.format(offTime));

                    newOne.setAdvanceCard2(timeFormat.format(DateUtils.addMinutes(onTime,-1*onBeginMin)));//上班最早打卡时间
                    newOne.setLateCard2(timeFormat.format(DateUtils.addMinutes(onTime,offEndMin)));//上班最晚打卡时间

                    newOne.setEarlyCard2(timeFormat.format(DateUtils.addMinutes(offTime,-1*offBeginMin)));//下班最早打卡时间1
                    newOne.setPostponeCard2(timeFormat.format(DateUtils.addMinutes(offTime,offEndMin)));
                }
                else if(n==2){
                    newOne.setStart3(timeFormat.format(onTime));
                    newOne.setEnd3(timeFormat.format(offTime));

                    newOne.setAdvanceCard3(timeFormat.format(DateUtils.addMinutes(onTime,-1*onBeginMin)));//上班最早打卡时间
                    newOne.setLateCard3(timeFormat.format(DateUtils.addMinutes(onTime,offEndMin)));//上班最晚打卡时间

                    newOne.setEarlyCard3(timeFormat.format(DateUtils.addMinutes(offTime,-1*offBeginMin)));//下班最早打卡时间1
                    newOne.setPostponeCard3(timeFormat.format(DateUtils.addMinutes(offTime,offEndMin)));
                }
            }
        }
        return newOne;
    }
    private OapiAttendanceShiftQueryResponse.TopShiftVo GetDetail(Long shiftId) throws ApiException {
        String password=tokenCreator.Refresh();
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/shift/query");
        OapiAttendanceShiftQueryRequest req = new OapiAttendanceShiftQueryRequest();
        req.setShiftId(shiftId);
        req.setOpUserId("296842114330963873");
        OapiAttendanceShiftQueryResponse rsp = client.execute(req, password);
        if(rsp.isSuccess()){
            return rsp.getResult();
        } else return null;
    }
}
