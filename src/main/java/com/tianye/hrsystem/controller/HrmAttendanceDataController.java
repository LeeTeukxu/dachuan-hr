package com.tianye.hrsystem.controller;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.common.DateTimeUtils;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IHrmAttendanceDataService;
import com.tianye.hrsystem.service.IWorkPlanService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.util.MyDateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.acl.Group;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @ClassName: HrmAttendanceDataController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月15日 14:12
 **/
@Controller
@RequestMapping("/attendanceData")
public class HrmAttendanceDataController {
    @Autowired
    IHrmAttendanceDataService dataService;
    @Autowired
    hrmEmployeeRepository empRep;
    @Autowired
    IAccessToken tokener;
    @Autowired
    tbattendanceuserRepository userRep;
    @Autowired
    StringRedisTemplate redisRep;
    @Autowired
    MyDateUtils dateUtils;
    @Autowired
    IWorkPlanService planService;

    SimpleDateFormat ss1 = new SimpleDateFormat("HH:mm");

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    Logger logger = LoggerFactory.getLogger(HrmAttendanceDataController.class);

    @RequestMapping("/sync")
    @ResponseBody
    public successResult GetData(String EmpID, String Begin, String End) {
        successResult result = new successResult();
        try {
            LoginUserInfo Info = CompanyContext.get();
            Date BeginDate = format.parse(Begin);
            Date EndDate = format.parse(End);
            logger.info("开始同步:" + Info.getCompanyId() + "的人员:" + EmpID + "的考勤数据!");
            dataService.SyncData(EmpID, BeginDate, EndDate);
        } catch (Exception ax) {
            ax.printStackTrace();
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/syncAll")
    @ResponseBody
    public successResult SyncAll(String Begin, String End) {
        successResult result = new successResult();
        try {
            LoginUserInfo Info = CompanyContext.get();
            Date BeginDate = format.parse(Begin);
            Date EndDate = format.parse(End);
            List<HrmEmployee> alls = empRep.findAll().stream().collect(Collectors.toList());
            String EmpID =
                    StringUtils.join(alls.stream().map(f -> Long.toString(f.getEmployeeId())).collect(Collectors.toList()), ',');
            logger.info("开始同步:" + Info.getCompanyId() + "的" + Integer.toString(alls.size()) + "个人员的考勤数据!");

            dataService.SyncData(EmpID, BeginDate, EndDate);
        } catch (Exception ax) {
            ax.printStackTrace();
            result.raiseException(ax);
        }
        return result;
    }


    @RequestMapping("/getOverTime")
    @ResponseBody
    public successResult getOvertTime(String Begin, String End) {
        successResult result = new successResult();
        try {
            String Ids = "1078299679";
            result.setData(getTotalByFields(Begin, End, Ids));
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    private Map<String, Double> getTotalByFields(String Begin, String End, String Field) throws Exception {
        LoginUserInfo Info = CompanyContext.get();
        List<tbattendanceuser> allUsers = userRep.findAll();
        List<String> userIds = allUsers.stream().map(f -> f.getUserId()).collect(Collectors.toList());
        Date begin = format.parse(Begin);
        Date end = format.parse(End);
        String password = tokener.Refresh(Info.getCompanyId());
        Map<String, Double> Nums = new HashMap<>();
        userIds.forEach(userId -> {
            AtomicReference<Double> Total = new AtomicReference<>(0.0);
            String userName =
                    allUsers.stream().filter(f -> f.getUserId().equals(userId)).findFirst().get().getUserName();
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getcolumnval");
            OapiAttendanceGetcolumnvalRequest req = new OapiAttendanceGetcolumnvalRequest();
            req.setFromDate(begin);
            req.setToDate(end);
            req.setColumnIdList(Field);
            req.setUserid(userId);
            OapiAttendanceGetcolumnvalResponse rsp = null;
            try {
                rsp = client.execute(req, password);

            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
            if (rsp.isSuccess()) {
                OapiAttendanceGetcolumnvalResponse.ColumnValListForTopVo rr = rsp.getResult();
                rr.getColumnVals().forEach(fs -> {
                    fs.getColumnVals().forEach(f -> {
                        String Value = f.getValue();
                        Double Num = Double.parseDouble(Value);
                        Total.updateAndGet(v -> v + Num);
                    });
                });
            }
            if (Total.get() > 0) {
                logger.info(userName + begin + "至" + end + "加班：" + Double.toString(Total.get()) + "小时");
                Nums.put(userId, Total.get());
            }
        });
        return Nums;
    }

    /**
     * create by: mmzs
     * description: TODO
     * create time:
     * 返回所有的班次列表,用于选择
     *
     * @return
     */
    @RequestMapping("getShiftList")
    @ResponseBody
    public successResult getShiftList() {
        successResult result = new successResult();
        try {
            LoginUserInfo Info = CompanyContext.get();
            String token = tokener.Refresh(Info.getCompanyId());
            String adminId = tokener.GetAdminUser(Info.getCompanyId());
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/shift/list");
            OapiAttendanceShiftListRequest req = new OapiAttendanceShiftListRequest();
            req.setOpUserId(adminId);
            OapiAttendanceShiftListResponse rsp = client.execute(req, token);
            if (rsp.isSuccess()) {
                List<OapiAttendanceShiftListResponse.TopMinimalismShiftVo> shifts = rsp.getResult().getResult();
                result.setData(shifts);
            }
        } catch (Exception ax) {

        }
        return result;
    }

    /**
     * create by: mmzs
     * description: TODO
     * create time:
     * 返回带有排班的信息的考勤组。有于首页展示
     *
     * @return
     */
    @RequestMapping("/getGoupList")
    @ResponseBody
    public successResult getGroupList() {
        successResult result = new successResult();
        try {
            LoginUserInfo Info = CompanyContext.get();
            ArrayList<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> Res = new ArrayList();
            String token = tokener.Refresh(Info.getCompanyId());
            Long offset = 0L;
            List<GroupObject> res = new ArrayList<>();
            while (true) {
                DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getsimplegroups");
                OapiAttendanceGetsimplegroupsRequest req = new OapiAttendanceGetsimplegroupsRequest();
                req.setOffset(offset);
                req.setSize(10L);
                OapiAttendanceGetsimplegroupsResponse rsp = client.execute(req, token);
                if (rsp.isSuccess()) {
                    OapiAttendanceGetsimplegroupsResponse.AtGroupListForTopVo rMain = rsp.getResult();
                    Res.addAll(rMain.getGroups());
                    if (rMain.getHasMore() == true) offset += 10;
                    else break;
                } else break;
            }
            if (Res.size() > 0) {
                List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> DD =
                        Res.stream().filter(f -> f.getType().equals("TURN")).collect(Collectors.toList());
                DD.forEach(f -> {
                    GroupObject obj = new GroupObject();
                    obj.setGroupId(f.getGroupId());
                    obj.setGroupName(f.getGroupName());
                    obj.setMemberCount(f.getMemberCount());

                    List<ComboboxItem> groupItems = new ArrayList<>();
                    ComboboxItem sItem = new ComboboxItem();
                    sItem.setId("1");
                    sItem.setText("休假");
                    groupItems.add(sItem);
                    List<ComboboxItem> iits = new ArrayList<>();
                    f.getSelectedClass().forEach(x -> {
                        String Id = Long.toString(x.getClassId());
                        Optional<ComboboxItem> findItems = groupItems.stream().filter(y -> y.getId().equals(Id)).findFirst();
                        if (findItems.isPresent() == false) {
                            ComboboxItem item = new ComboboxItem();
                            item.setId(Id);
                            item.setText(x.getClassName());
                            List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> Ts = x.getSections();
                            if (Ts.size() > 0) {
                                OapiAttendanceGetsimplegroupsResponse.AtSectionVo F1 = Ts.get(0);
                                List<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> Times1 = F1.getTimes();
                                if (Times1.size() == 2) {
                                    String Ch1 = Times1.get(0).getCheckType();
                                    String Ch2 = Times1.get(1).getCheckType();

                                    String Tm1 = ss1.format(Times1.get(0).getCheckTime());
                                    String Tm2 = ss1.format(Times1.get(1).getCheckTime());
                                    if (Ch1.equals("OnDuty")) item.setBegin1(Tm1);
                                    if (Ch2.equals("OffDuty")) item.setEnd1(Tm2);
                                }
                            }
                            if (Ts.size() > 1) {
                                OapiAttendanceGetsimplegroupsResponse.AtSectionVo F2 = Ts.get(1);
                                List<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> Times1 = F2.getTimes();
                                if (Times1.size() == 2) {
                                    String Ch1 = Times1.get(0).getCheckType();
                                    String Ch2 = Times1.get(1).getCheckType();

                                    String Tm1 = ss1.format(Times1.get(0).getCheckTime());
                                    String Tm2 = ss1.format(Times1.get(1).getCheckTime());
                                    if (Ch1.equals("OnDuty")) item.setBegin2(Tm1);
                                    if (Ch2.equals("OffDuty")) item.setEnd2(Tm2);
                                }
                            }
                            groupItems.add(item);
                            iits.add(item);
                        }
                    });
                    String Key = "ClassList_" + Long.toString(f.getGroupId());
                    redisRep.opsForValue().set(Key, JSON.toJSONString(groupItems));
                    obj.setShifts(iits);
                    res.add(obj);
                });
                result.setData(res);
            }
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("/getUsersByGroup")
    public successResult getUsersByGroup(String GroupID) {
        successResult result = new successResult();
        try {
            LoginUserInfo Info = CompanyContext.get();
            String token = tokener.Refresh(Info.getCompanyId());
            String OpUser = tokener.GetAdminUser(Info.getCompanyId());
            Long Cursor = 0L;
            List<ComboboxItem> res = new ArrayList<>();
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/memberusers/list");
            List<tbattendanceuser> users = userRep.findAll();
            while (true) {
                OapiAttendanceGroupMemberusersListRequest req = new OapiAttendanceGroupMemberusersListRequest();
                req.setCursor(Cursor);
                req.setOpUserId(OpUser);
                req.setGroupId(Long.parseLong(GroupID));
                OapiAttendanceGroupMemberusersListResponse rsp = client.execute(req, token);
                if (rsp.isSuccess()) {
                    OapiAttendanceGroupMemberusersListResponse.PageResult PP = rsp.getResult();
                    List<String> IDS = PP.getResult();
                    for (int i = 0; i < IDS.size(); i++) {
                        String UserID = IDS.get(i);
                        Optional<tbattendanceuser> findUsers =
                                users.stream().filter(f -> f.getUserId().equals(UserID)).findFirst();
                        if (findUsers.isPresent()) {
                            tbattendanceuser user = findUsers.get();
                            ComboboxItem item = new ComboboxItem();
                            item.setId(UserID);
                            item.setText(user.getUserName());
                            res.add(item);
                        }
                    }
                    if (PP.getHasMore()) {
                        Cursor = PP.getCursor();
                    } else break;
                } else throw new ApiException("当前考勤组下没有参与考勤的人员!");
            }
            result.setData(res);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    private List<String> getUserListByGroup(String GroupID) throws ApiException {
        LoginUserInfo Info = CompanyContext.get();
        String token = tokener.Refresh(Info.getCompanyId());
        String OpUser = tokener.GetAdminUser(Info.getCompanyId());
        Long Cursor = 0L;
        List<String> Users = new ArrayList<>();
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/memberusers/list");
        while (true) {
            OapiAttendanceGroupMemberusersListRequest req = new OapiAttendanceGroupMemberusersListRequest();
            req.setCursor(Cursor);
            req.setOpUserId(OpUser);
            req.setGroupId(Long.parseLong(GroupID));
            OapiAttendanceGroupMemberusersListResponse rsp = client.execute(req, token);
            if (rsp.isSuccess()) {
                OapiAttendanceGroupMemberusersListResponse.PageResult PP = rsp.getResult();
                Users.addAll(PP.getResult());
                if (PP.getHasMore()) {
                    Cursor = PP.getCursor();
                } else break;
            } else throw new ApiException("当前考勤组下没有参与考勤的人员!");
        }
        return Users;
    }

    @ResponseBody
    @RequestMapping("/getClassListByGroup")
    public successResult getClassListByGroupId(String GroupID) {
        successResult result = new successResult();
        try {
            String Key = "ClassList_" + GroupID;
            if (redisRep.hasKey(Key)) {
                String X = redisRep.opsForValue().get(Key);
                List<ComboboxItem> Items = JSON.parseArray(X, ComboboxItem.class);
                result.setData(Items);
            } else {
                throw new Exception("请先运行getGoupList接口后再执行操作!");
            }
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd");

    @RequestMapping("/getPlanDataByGroup")
    @ResponseBody
    public successResult getPlanDataByGroup(String Begin, String End, String GroupID) {
        successResult result = new successResult();
        try {
            List<PlanObject> res = new ArrayList<>();
            LoginUserInfo Info = CompanyContext.get();
            String OpUser = tokener.GetAdminUser(Info.getCompanyId());
            String token = tokener.Refresh(Info.getCompanyId());
            Date begin = simple.parse(Begin);
            Date end = simple.parse(End);
            List<String> UserIDS = getUserListByGroup(GroupID);
            List<Date[]> Days = dateUtils.getDateRangeByLimit(begin, end, 7);
            List<tbattendanceuser> allUsers = userRep.findAll();
            for (int i = 0; i < Days.size(); i++) {
                Date[] D = Days.get(i);
                DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk" +
                        ".com/topapi/attendance/schedule/listbyusers");
                OapiAttendanceScheduleListbyusersRequest req = new OapiAttendanceScheduleListbyusersRequest();
                req.setOpUserId(OpUser);
                req.setUserids(String.join(",", UserIDS));
                req.setFromDateTime(D[0].getTime());
                req.setToDateTime(D[1].getTime());
                OapiAttendanceScheduleListbyusersResponse rsp = client.execute(req, token);
                if (rsp.isSuccess()) {
                    List<OapiAttendanceScheduleListbyusersResponse.TopScheduleVo> Rows = rsp.getResult();
                    if (Rows == null) {
                        continue;
                    }
                    for (int n = 0; n < Rows.size(); n++) {
                        OapiAttendanceScheduleListbyusersResponse.TopScheduleVo row = Rows.get(n);
                        if(row.getShiftId()==null)continue;
                        //String X=JSON.toJSONString(row);
                        //System.out.println(X);
                        String workDate=simple.format(row.getWorkDate());
                        Optional<PlanObject> findObjects =
                                res.stream().filter(f ->
                                        f.getUserId().equals(Long.parseLong(row.getUserid()))
                                        && Long.toString(f.getShiftId()).equals(Long.toString(row.getShiftId()))
                                                && f.getWorkDate().equals(workDate)).findFirst();
                        if (findObjects.isPresent() == false) {
                            PlanObject Obj = new PlanObject();
                            Obj.setPlanId(row.getId());
                            Obj.setGroupId(row.getGroupId());
                            Obj.setShiftId(row.getShiftId());
                            Optional<tbattendanceuser> findUsers =
                                    allUsers.stream().filter(f -> f.getUserId().equals(row.getUserid())).findFirst();
                            if (findUsers.isPresent()) {
                                tbattendanceuser user = findUsers.get();
                                Obj.setUserId(Long.parseLong(row.getUserid()));
                                Obj.setUserName(user.getUserName());
                            }
                            Obj.setWorkDate(workDate);
                            res.add(Obj);
                        }
                    }
                } else {
                    throw new Exception(rsp.getErrmsg());
                }
            }
            result.setData(res);
        } catch (Exception ax) {
            ax.printStackTrace();
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/getAllShifts")
    @ResponseBody
    public successResult getAllShiftList() {
        successResult result = new successResult();
        try {
            List<ShiftItem> Items=new ArrayList<>();
            ShiftItem first=new ShiftItem();
            first.setShiftId("1");
            first.setShiftName("休假");
            Items.add(first);
            List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> DD =planService.getAllGroups();
            DD.forEach(f -> {
                String groupName=f.getGroupName();
                Long groupId=f.getGroupId();
                f.getSelectedClass().forEach(x -> {
                    Long  Id =x.getClassId();
                    Optional<ShiftItem> findItems = Items.stream().filter(y -> y.getShiftId().equals(Id)).findFirst();
                    if (findItems.isPresent() == false) {
                        ShiftItem item = new ShiftItem();
                        item.setGroupId(Long.toString(groupId));
                        item.setGroupName(groupName);
                        item.setShiftId(Long.toString(Id));
                        item.setShiftName(x.getClassName());
                        List<OapiAttendanceGetsimplegroupsResponse.AtSectionVo> Ts = x.getSections();
                        if (Ts.size() > 0) {
                            OapiAttendanceGetsimplegroupsResponse.AtSectionVo F1 = Ts.get(0);
                            List<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> Times1 = F1.getTimes();
                            if (Times1.size() == 2) {
                                String Ch1 = Times1.get(0).getCheckType();
                                String Ch2 = Times1.get(1).getCheckType();

                                String Tm1 = ss1.format(Times1.get(0).getCheckTime());
                                String Tm2 = ss1.format(Times1.get(1).getCheckTime());
                                if (Ch1.equals("OnDuty")) item.setBegin1(Tm1);
                                if (Ch2.equals("OffDuty")) item.setEnd1(Tm2);
                            }
                        }
                        if (Ts.size() > 1) {
                            OapiAttendanceGetsimplegroupsResponse.AtSectionVo F2 = Ts.get(1);
                            List<OapiAttendanceGetsimplegroupsResponse.SetionTimeVO> Times1 = F2.getTimes();
                            if (Times1.size() == 2) {
                                String Ch1 = Times1.get(0).getCheckType();
                                String Ch2 = Times1.get(1).getCheckType();

                                String Tm1 = ss1.format(Times1.get(0).getCheckTime());
                                String Tm2 = ss1.format(Times1.get(1).getCheckTime());
                                if (Ch1.equals("OnDuty")) item.setBegin2(Tm1);
                                if (Ch2.equals("OffDuty")) item.setEnd2(Tm2);
                            }
                        }
                        Items.add(item);
                    }
                });
            });
            result.setData(Items);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }
    @ResponseBody
    @RequestMapping("/getAllUsers")
    public successResult getAllUsers() {
        successResult result = new successResult();
        try {
            LoginUserInfo Info=CompanyContext.get();
            List<UserObject> res=planService.getUsers(Info.getCompanyId());
            result.setData(res);
        } catch (Exception ax) {
            ax.printStackTrace();
            result.raiseException(ax);
        }
        return result;
    }
}
