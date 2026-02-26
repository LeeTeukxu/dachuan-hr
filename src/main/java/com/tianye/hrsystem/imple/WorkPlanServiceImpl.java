package com.tianye.hrsystem.imple;

import com.alibaba.fastjson.JSON;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.taobao.api.ApiException;
import java.util.concurrent.TimeUnit;
import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.model.UserObject;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.mapper.WorkPlanMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.service.IWorkPlanService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: WorkPlanServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年08月02日 18:01
 **/
@Service
public class WorkPlanServiceImpl implements IWorkPlanService {
    @Autowired
    tbPlanListRepository planRep;
    @Autowired
    IAccessToken tokener;
    @Autowired
    WorkPlanMapper workPlanMapper;

    @Override
    @Transactional
    public void AddAll(List<tbplanlist> planList) throws Exception {
        List<tbplanlist> saves = new ArrayList<>();
        for (int i = 0; i < planList.size(); i++) {
            tbplanlist plan = planList.get(i);
            if (plan.getId() == null) {
                plan.setCreateTime(new Date());
                saves.add(plan);
            } else {
                Optional<tbplanlist> findPs = planRep.findById(plan.getId());
                if (findPs.isPresent()) {
                    tbplanlist p = findPs.get();
                    p.setClassId(plan.getClassId());
                    p.setGroupId(plan.getGroupId());
                    p.setWorkDate(plan.getWorkDate());
                    p.setProductName(plan.getProductName());
                    p.setLinkName(plan.getLinkName());
                    p.setUserId(plan.getUserId());
                    saves.add(p);
                }
            }
        }
        if (saves.size() > 0) {
            planRep.saveAll(saves);
            boolean hasChange= PostToServer(saves);
            if(hasChange) getAllUsersInner();
        }

    }

    @Autowired
    StringRedisTemplate redisRep;
    @Autowired
    tbattendanceuserRepository userRep;

    public List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getAllGroups() throws Exception {
        LoginUserInfo Info = CompanyContext.get();
        ArrayList<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> Res = new ArrayList();
        String token = tokener.Refresh(Info.getCompanyId());
        Long offset = 0L;
        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> DD = new ArrayList<>();
        while (true) {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk" +
                    ".com/topapi/attendance/getsimplegroups");
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
            DD = Res;
            String Dx = JSON.toJSONString(DD);
            redisRep.opsForValue().set(getGroupKey(Info.getCompanyId()), Dx, 24, TimeUnit.HOURS);
        }
        return DD;
    }

    private String getGroupKey(String CompanyID) {
        return StringUtils.leftPad(CompanyID, 4, '0') + "_getAllGroups";
    }

    private List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getGroups(String CompanyID) throws Exception {
        String Key = getGroupKey(CompanyID);
        if (redisRep.hasKey(Key)) {
            String X = redisRep.opsForValue().get(Key);
            return JSON.parseArray(X, OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo.class);
        } else return getAllGroups();
    }

    private String getUserKey(String CompanyID) {
        return StringUtils.leftPad(CompanyID, 4, '0') + "_getAllUsers";
    }

    public List<UserObject> getUsers(String CompanyID) throws Exception {
        String Key = getUserKey(CompanyID);
        if (redisRep.hasKey(Key)) {
            String X = redisRep.opsForValue().get(Key);
            return JSON.parseArray(X, UserObject.class);
        } else return getAllUsersInner();
    }

    private List<UserObject> getAllUsersInner() throws Exception {
        LoginUserInfo Info = CompanyContext.get();
        String token = tokener.Refresh(Info.getCompanyId());
        String OpUser = tokener.GetAdminUser(Info.getCompanyId());
        List<tbattendanceuser> users = userRep.findAll();
        List<UserObject> res = new ArrayList<>();
        List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> Groups = getGroups(Info.getCompanyId());
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/memberusers/list");
        for (OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo y : Groups) {
            Long Cursor = 0L;
            while (true) {
                OapiAttendanceGroupMemberusersListRequest req = new OapiAttendanceGroupMemberusersListRequest();
                req.setCursor(Cursor);
                req.setOpUserId(OpUser);
                req.setGroupId(y.getGroupId());
                OapiAttendanceGroupMemberusersListResponse rsp = client.execute(req, token);
                if (rsp.isSuccess()) {
                    OapiAttendanceGroupMemberusersListResponse.PageResult PP = rsp.getResult();
                    List<String> IDS = PP.getResult();
                    if (IDS == null) {
                        break; // 跳过当前考勤组，继续处理下一个考勤组
                    }
                    for (int i = 0; i < IDS.size(); i++) {
                        String UserID = IDS.get(i);
                        Optional<tbattendanceuser> findUsers =
                                users.stream().filter(f -> f != null && f.getUserId() != null && f.getUserId().equals(UserID)).findFirst();
                        if (findUsers.isPresent()) {
                            tbattendanceuser user = findUsers.get();
                            UserObject item = new UserObject();
                            item.setId(UserID);
                            item.setName(user.getUserName());
                            item.setGroupId(Long.toString(y.getGroupId()));
                            res.add(item);
                        }
                    }
                    if (PP.getHasMore()) {
                        Cursor = PP.getCursor();
                    } else break;
                } else {
                    // 记录当前考勤组的错误，但继续处理下一个考勤组
                    System.out.println("考勤组 " + y.getGroupName() + " (ID: " + y.getGroupId() + ") 获取成员失败，跳过该考勤组");
                    break; // 跳出while循环，继续处理下一个考勤组
                }
            }
        }
        if (res.size() > 0) {
            redisRep.opsForValue().set(getUserKey(Info.getCompanyId()), JSON.toJSONString(res), 24, TimeUnit.HOURS);
        }
        return res;
    }

    private boolean changeGroup(String  userId, String token, String adminUserId, String  oldGroup, String  newGroup) throws Exception {
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/users/remove");
        OapiAttendanceGroupUsersRemoveRequest req = new OapiAttendanceGroupUsersRemoveRequest();
        req.setOpUserid(adminUserId);
        req.setGroupKey(oldGroup);
        req.setUserIdList(userId);
        OapiAttendanceGroupUsersRemoveResponse rsp = client.execute(req, token);
        if (rsp.getSuccess() == true) {
            DingTalkClient client1 = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/users/add");
            OapiAttendanceGroupUsersAddRequest req1 = new OapiAttendanceGroupUsersAddRequest();
            req.setOpUserid(adminUserId);
            req.setGroupKey(newGroup);
            req.setUserIdList(userId);
            OapiAttendanceGroupUsersAddResponse rsp1 = client1.execute(req1, token);
            if (rsp1.getSuccess() == true) return true;
            else throw new Exception(rsp1.getMessage());
        } else throw new Exception(rsp.getMessage());
    }

    private boolean PostToServer(List<tbplanlist> list) throws Exception {
        LoginUserInfo Info = CompanyContext.get();
        String adminUserId = tokener.GetAdminUser(Info.getCompanyId());
        String token = tokener.Refresh(Info.getCompanyId());
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/group/schedule/async");
        List<UserObject> UserHash = getUsers(Info.getCompanyId());
        Boolean HasChange=false;
        for (int i = 0; i < list.size(); i++) {
            tbplanlist plan = list.get(i);
            Date workDate = plan.getWorkDate();
            String groupId = plan.getGroupId();//上传来的，也可能发生了更改。
            String shiftId = plan.getClassId();
            String userIdd = plan.getUserId();
            String[] userIds = userIdd.split(",");
            for (int n = 0; n < userIds.length; n++) {
                String userId = userIds[n];
                Optional<UserObject> findUsers =
                        UserHash.stream().filter(f -> f.getId().equals(userId)).findFirst();
                if (findUsers.isPresent()) {
                    UserObject find = findUsers.get();
                    String  oldGrupId = find.getGroupId();
                    if (oldGrupId.equals(groupId) == false) {
                        changeGroup(userId,token,adminUserId,oldGrupId,groupId);
                        HasChange=true;
                    }
                }

                OapiAttendanceGroupScheduleAsyncRequest req = new OapiAttendanceGroupScheduleAsyncRequest();
                req.setOpUserId(adminUserId);
                req.setGroupId(Long.valueOf(groupId));
                List<OapiAttendanceGroupScheduleAsyncRequest.TopScheduleParam> list2 =
                        new ArrayList<OapiAttendanceGroupScheduleAsyncRequest.TopScheduleParam>();
                OapiAttendanceGroupScheduleAsyncRequest.TopScheduleParam obj3 =
                        new OapiAttendanceGroupScheduleAsyncRequest.TopScheduleParam();
                list2.add(obj3);
                obj3.setShiftId(Long.valueOf(shiftId));
                obj3.setIsRest(Long.valueOf(shiftId) == 1L ? true : false);
                obj3.setWorkDate(workDate.getTime());
                obj3.setUserid(userId);
                req.setSchedules(list2);
                OapiAttendanceGroupScheduleAsyncResponse rsp = client.execute(req, token);
                if (rsp.getSuccess() == false) throw new Exception(rsp.getMessage());
            }
        }
        return HasChange;
    }

    @Override
    @Transactional
    public void RemoveAll(List<Integer> IDArray) throws Exception {
        for (int i = 0; i < IDArray.size(); i++) {
            Integer ID = IDArray.get(i);
            planRep.deleteById(ID);
        }
    }

    @Override
    public PageObject<tbplanlist> getMaxDate(Integer pageSize, Integer pageNum, String sortField, String sortOrder) {
        Map<String, Object> param = new HashMap<>();
        if (pageSize == null) pageSize = 20;
        if (pageNum == null) pageNum = 0;
        if (StringUtils.isEmpty(sortField)) sortField = "createTime";
        if (StringUtils.isEmpty(sortOrder)) sortOrder = "asc";
        param.put("sortField", sortField);
        param.put("sortOrder", sortOrder);
        param.put("pageNum", pageNum);
        param.put("pageSize", pageSize);
        List<tbplanlist> datas = workPlanMapper.getMaxDate(param);
        PageObject<tbplanlist> object = new PageObject<>();
        if (datas.size() > 0) {
            object.setList(datas);
        }

        return object;
    }
}
