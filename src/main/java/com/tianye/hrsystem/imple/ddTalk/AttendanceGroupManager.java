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
import org.springframework.transaction.support.TransactionTemplate;

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

    // SimpleDateFormat 非线程安全，单例字段必须 ThreadLocal 隔离
    private static final ThreadLocal<SimpleDateFormat> TIME_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss"));

    @Autowired
    DDTalkResposeLogger ddLogger;
    @Autowired
    MyDateUtils dateUtils;
    @Autowired
    TransactionTemplate transactionTemplate;

    @Override
    public void GetAndSave() throws ApiException {
        // 钉钉分页抓取（含逐班次详情外呼）在事务外执行，避免长时间占用租户连接；
        // 本地快照整体替换在单个事务内原子完成
        GroupSyncSnapshot snapshot = fetchCurrentSnapshot();
        transactionTemplate.execute(status -> {
            replaceLocalSnapshot(snapshot);
            return null;
        });
    }

    GroupSyncSnapshot fetchCurrentSnapshot() throws ApiException {
        Long offset = 0L;
        GroupSyncSnapshot snapshot = new GroupSyncSnapshot();
        while (true) {
            String token = tokenCreator.Refresh();
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getsimplegroups");
            OapiAttendanceGetsimplegroupsRequest req = new OapiAttendanceGetsimplegroupsRequest();
            req.setOffset(offset);
            req.setSize(10L);
            OapiAttendanceGetsimplegroupsResponse rsp = client.execute(req, token);

            Date begin = dateUtils.getCurrent();
            ddLogger.Info(rsp, ((DefaultDingTalkClient) client).getRequestUrl(), begin, AttendanceGroupManager.class);

            if (!rsp.isSuccess()) {
                throw new ApiException(rsp.getErrmsg());
            }

            OapiAttendanceGetsimplegroupsResponse.AtGroupListForTopVo body = rsp.getResult();
            List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> groups =
                    body == null || body.getGroups() == null ? Collections.emptyList() : body.getGroups();
            for (OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group : groups) {
                if (group == null || group.getGroupId() == null) {
                    continue;
                }
                Long groupId = group.getGroupId();
                snapshot.groups.add(buildAttendanceGroup(group));
                snapshot.shifts.addAll(buildAttendanceShifts(groupId, group, group.getSelectedClass()));
                appendGroupRelations(snapshot, groupId, group.getDeptNameList());
                logger.info("添加了考勤组:" + group.getGroupName());
            }

            offset += groups.size();
            if (body == null || !Boolean.TRUE.equals(body.getHasMore())) {
                break;
            }
        }
        return snapshot;
    }

    private void replaceLocalSnapshot(GroupSyncSnapshot snapshot) {
        groupRep.deleteAll();
        depRelRep.deleteAll();
        empRelRep.deleteAll();
        shiftRep.deleteAll();

        if (!snapshot.groups.isEmpty()) {
            groupRep.saveAll(snapshot.groups);
        }
        if (!snapshot.deptRelations.isEmpty()) {
            depRelRep.saveAll(snapshot.deptRelations);
        }
        if (!snapshot.employeeRelations.isEmpty()) {
            empRelRep.saveAll(snapshot.employeeRelations);
        }
        if (!snapshot.shifts.isEmpty()) {
            shiftRep.saveAll(snapshot.shifts);
        }
    }

    private HrmAttendanceGroup buildAttendanceGroup(OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo groupInfo) {
        HrmAttendanceGroup newOne = new HrmAttendanceGroup();
        newOne.setAttendanceGroupId(groupInfo.getGroupId());
        newOne.setName(groupInfo.getGroupName());
        newOne.setIsRest(1);
        newOne.setIsDefaultSetting(Boolean.TRUE.equals(groupInfo.getIsDefault()) ? 1 : 0);
        newOne.setShiftSetting(buildShiftSetting(groupInfo.getSelectedClass()));
        newOne.setCreateTime(new Date());
        newOne.setEffectTime(new Date());
        newOne.setCreateUserId(1L);
        return newOne;
    }

    private String buildShiftSetting(List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> shifts) {
        if (shifts == null || shifts.isEmpty()) {
            return null;
        }
        List<String> shiftIds = new ArrayList<>();
        for (OapiAttendanceGetsimplegroupsResponse.AtClassVo shift : shifts) {
            if (shift == null || shift.getClassId() == null) {
                continue;
            }
            shiftIds.add(String.valueOf(shift.getClassId()));
        }
        if (shiftIds.isEmpty()) {
            return null;
        }
        return String.join(",", shiftIds);
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
    private void appendGroupRelations(GroupSyncSnapshot snapshot, Long groupId, List<String> depNameList) {
        if (depNameList == null || depNameList.isEmpty()) {
            return;
        }
        for (String depName : depNameList) {
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
                newOne.setAttendanceGroupId(groupId);
                newOne.setDeptId(hm.getDeptId());
                newOne.setEffectTime(new Date());
                newOne.setCreateTime(new Date());
                newOne.setCreateUserId(1L);
                snapshot.deptRelations.add(newOne);
                snapshot.employeeRelations.addAll(buildGroupRelationToEmployee(groupId, hm.getDeptId()));
            }
        }
    }

    private List<HrmAttendanceGroupRelationEmployee> buildGroupRelationToEmployee(Long groupId, Long deptId) {
        List<HrmAttendanceGroupRelationEmployee> relations = new ArrayList<>();
        List<HrmEmployee> employees = empRep.findAllByDeptId(deptId);
        for (HrmEmployee em : employees) {

            HrmAttendanceGroupRelationEmployee ee = new HrmAttendanceGroupRelationEmployee();
            ee.setAttendanceGroupRelationEmployeeId(System.currentTimeMillis());
            ee.setAttendanceGroupId(groupId);
            ee.setEmployeeId(em.getEmployeeId());
            ee.setEffectTime(new Date());
            ee.setCreateTime(new Date());
            ee.setCreateUserId(1L);
            relations.add(ee);
        }
        return relations;
    }

    private List<HrmAttendanceShift> buildAttendanceShifts(Long GroupID,
            OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo groupInfo,
            List<OapiAttendanceGetsimplegroupsResponse.AtClassVo> Shifts) throws  ApiException{
        if (Shifts == null || Shifts.isEmpty()) {
            return Collections.emptyList();
        }
        List<HrmAttendanceShift> Ss=new ArrayList<>();
        for(int i=0;i<Shifts.size();i++){
            OapiAttendanceGetsimplegroupsResponse.AtClassVo shift=Shifts.get(i);
            Long classId=shift.getClassId();
            String className=shift.getClassName();
            String shiftType=groupInfo.getType();
            HrmAttendanceShift newOne=new HrmAttendanceShift();
            newOne.setShiftId(classId);
            newOne.setShiftName(className);

            if("NONE".equals(shiftType)){
                newOne.setShiftType(0);
            } else {
                newOne.setShiftType("FIXED".equals(shiftType) ? 1 : 2);
            }
            newOne.setGroupId(GroupID);
            newOne.setEffectTime(new Date());
            newOne.setCreateTime(new Date());
            newOne.setCreateUserId(1L);

            OapiAttendanceGetsimplegroupsResponse.ClassSettingVo setting= shift.getSetting();
            if (setting != null) {
                if (setting.getWorkTimeMinutes() != null) {
                    newOne.setShiftHours(Math.toIntExact(setting.getWorkTimeMinutes()/60));
                }
                OapiAttendanceGetsimplegroupsResponse.AtTimeVo beginVo=setting.getRestBeginTime();
                OapiAttendanceGetsimplegroupsResponse.AtTimeVo endVo=setting.getRestEndTime();
                if(beginVo!=null && endVo!=null){
                    newOne.setRestStartTime(TIME_FORMAT.get().format(beginVo.getCheckTime()));
                    newOne.setRestEndTime(TIME_FORMAT.get().format(endVo.getCheckTime()));
                    newOne.setRestTimeStatus(1);
                } else {
                    newOne.setRestTimeStatus(0);
                }
            } else {
                newOne.setRestTimeStatus(0);
            }
            int lateMinutes = setting == null || setting.getPermitLateMinutes() == null
                    ? 0
                    : Math.toIntExact(setting.getPermitLateMinutes());
            List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> Sections= shift.getSections();

            OapiAttendanceShiftQueryResponse.TopShiftVo detail=GetDetail(classId);
            if(detail==null){
                newOne=FillSomeInfo(newOne, lateMinutes,Sections);
            } else {
               List<OapiAttendanceShiftQueryResponse.TopSectionVo> hs=  detail.getSections();
               newOne=FillSomeInfo1(newOne,hs);
            }
            Ss.add(newOne);
            logger.info("为"+groupInfo.getGroupName()+"下添加了考勤班次:"+newOne.getShiftName());
        }
        return Ss;
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
                    newOne.setStart1(TIME_FORMAT.get().format(onTime));
                    newOne.setEnd1(TIME_FORMAT.get().format(offTime));
                    newOne.setLateCard1(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,LateMinutes)));

                }
                else if(n==1){
                    newOne.setStart2(TIME_FORMAT.get().format(onTime));
                    newOne.setEnd2(TIME_FORMAT.get().format(offTime));
                    newOne.setLateCard2(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,LateMinutes)));
                }
                else if(n==2){
                    newOne.setStart3(TIME_FORMAT.get().format(onTime));
                    newOne.setEnd3(TIME_FORMAT.get().format(offTime));
                    newOne.setLateCard3(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,LateMinutes)));
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
                    newOne.setStart1(TIME_FORMAT.get().format(onTime));
                    newOne.setEnd1(TIME_FORMAT.get().format(offTime));

                    newOne.setAdvanceCard1(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,-1*onBeginMin)));//上班最早打卡时间
                    newOne.setLateCard1(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,onEndMin)));//上班最晚打卡时间

                    newOne.setEarlyCard1(TIME_FORMAT.get().format(DateUtils.addMinutes(offTime,-1*offBeginMin)));//下班最早打卡时间1
                    newOne.setPostponeCard1(TIME_FORMAT.get().format(DateUtils.addMinutes(offTime,offEndMin)));
                }
                else if(n==1){
                    newOne.setStart2(TIME_FORMAT.get().format(onTime));
                    newOne.setEnd2(TIME_FORMAT.get().format(offTime));

                    newOne.setAdvanceCard2(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,-1*onBeginMin)));//上班最早打卡时间
                    newOne.setLateCard2(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,offEndMin)));//上班最晚打卡时间

                    newOne.setEarlyCard2(TIME_FORMAT.get().format(DateUtils.addMinutes(offTime,-1*offBeginMin)));//下班最早打卡时间1
                    newOne.setPostponeCard2(TIME_FORMAT.get().format(DateUtils.addMinutes(offTime,offEndMin)));
                }
                else if(n==2){
                    newOne.setStart3(TIME_FORMAT.get().format(onTime));
                    newOne.setEnd3(TIME_FORMAT.get().format(offTime));

                    newOne.setAdvanceCard3(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,-1*onBeginMin)));//上班最早打卡时间
                    newOne.setLateCard3(TIME_FORMAT.get().format(DateUtils.addMinutes(onTime,offEndMin)));//上班最晚打卡时间

                    newOne.setEarlyCard3(TIME_FORMAT.get().format(DateUtils.addMinutes(offTime,-1*offBeginMin)));//下班最早打卡时间1
                    newOne.setPostponeCard3(TIME_FORMAT.get().format(DateUtils.addMinutes(offTime,offEndMin)));
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

    static class GroupSyncSnapshot {
        final List<HrmAttendanceGroup> groups = new ArrayList<>();
        final List<HrmAttendanceShift> shifts = new ArrayList<>();
        final List<HrmAttendanceGroupRelationDept> deptRelations = new ArrayList<>();
        final List<HrmAttendanceGroupRelationEmployee> employeeRelations = new ArrayList<>();
    }
}
