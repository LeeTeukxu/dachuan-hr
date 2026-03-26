package com.tianye.hrsystem.imple;

import com.alibaba.fastjson.JSON;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.taobao.api.ApiException;
import java.util.concurrent.TimeUnit;
import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.model.HrmAttendanceShift;
import com.tianye.hrsystem.model.UserObject;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(WorkPlanServiceImpl.class);

    @Autowired
    tbPlanListRepository planRep;
    @Autowired
    IAccessToken tokener;
    @Autowired
    WorkPlanMapper workPlanMapper;
    @Autowired
    hrmAttendanceShiftRepository shiftRep;

    private void enrichAndValidatePlan(tbplanlist plan) throws Exception {
        if (plan == null) {
            throw new Exception("排班数据不能为空");
        }
        if (StringUtils.isEmpty(plan.getClassId())) {
            throw new Exception("班次不能为空");
        }
        if (StringUtils.isEmpty(plan.getUserId())) {
            throw new Exception("人员不能为空");
        }
        if (plan.getWorkDate() == null) {
            throw new Exception("排班日期不能为空");
        }
        if (StringUtils.isEmpty(plan.getGroupId())) {
            try {
                Long shiftId = Long.valueOf(plan.getClassId());
                Optional<HrmAttendanceShift> findShift = shiftRep.findById(shiftId);
                if (findShift.isPresent() && findShift.get().getGroupId() != null) {
                    plan.setGroupId(Long.toString(findShift.get().getGroupId()));
                }
            } catch (NumberFormatException ignore) {
            }
        }
        if (StringUtils.isEmpty(plan.getGroupId())) {
            String groupId = resolveGroupIdByClassIdFromGroups(plan.getClassId());
            if (StringUtils.isEmpty(groupId) == false) {
                plan.setGroupId(groupId);
            }
        }
        if (StringUtils.isEmpty(plan.getGroupId())) {
            throw new Exception("排班缺少考勤组信息, classId=" + plan.getClassId());
        }
    }

    private String resolveGroupIdByClassIdFromGroups(String classId) {
        if (StringUtils.isEmpty(classId)) {
            return null;
        }
        try {
            Long targetClassId = Long.valueOf(classId);
            List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> groups = getAllGroups();
            if (groups == null || groups.isEmpty()) {
                return null;
            }
            for (OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo group : groups) {
                if (group == null || group.getGroupId() == null || group.getSelectedClass() == null) {
                    continue;
                }
                for (OapiAttendanceGetsimplegroupsResponse.AtClassVo shift : group.getSelectedClass()) {
                    if (shift != null && shift.getClassId() != null && shift.getClassId().equals(targetClassId)) {
                        return Long.toString(group.getGroupId());
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private boolean isSameWorkDate(Date left, Date right) {
        if (left == null || right == null) {
            return false;
        }
        Calendar leftCal = Calendar.getInstance();
        leftCal.setTime(left);
        Calendar rightCal = Calendar.getInstance();
        rightCal.setTime(right);
        return leftCal.get(Calendar.YEAR) == rightCal.get(Calendar.YEAR)
                && leftCal.get(Calendar.DAY_OF_YEAR) == rightCal.get(Calendar.DAY_OF_YEAR);
    }

    private tbplanlist buildInsertPlan(tbplanlist source) {
        tbplanlist target = new tbplanlist();
        target.setProductName(source.getProductName());
        target.setLinkName(source.getLinkName());
        target.setWorkDate(source.getWorkDate());
        target.setGroupId(source.getGroupId());
        target.setClassId(source.getClassId());
        target.setUserId(source.getUserId());
        target.setCreateTime(new Date());
        return target;
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date endOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    @Override
    @Transactional
    public void AddAll(List<tbplanlist> planList) throws Exception {
        if (planList == null || planList.isEmpty()) {
            return;
        }
        List<tbplanlist> saves = new ArrayList<>();
        for (int i = 0; i < planList.size(); i++) {
            tbplanlist plan = planList.get(i);
            enrichAndValidatePlan(plan);
            if (plan.getId() == null) {
                plan.setCreateTime(new Date());
                saves.add(plan);
            } else {
                Optional<tbplanlist> findPs = planRep.findById(plan.getId());
                if (findPs.isPresent() && isSameWorkDate(findPs.get().getWorkDate(), plan.getWorkDate())) {
                    tbplanlist p = findPs.get();
                    p.setClassId(plan.getClassId());
                    p.setGroupId(StringUtils.isEmpty(plan.getGroupId()) ? p.getGroupId() : plan.getGroupId());
                    p.setWorkDate(plan.getWorkDate());
                    p.setProductName(plan.getProductName());
                    p.setLinkName(plan.getLinkName());
                    p.setUserId(plan.getUserId());
                    saves.add(p);
                } else {
                    saves.add(buildInsertPlan(plan));
                }
            }
        }
        if (saves.size() > 0) {
            planRep.saveAll(saves);
            try {
                boolean hasChange = PostToServer(saves);
                if (hasChange) {
                    getAllUsersInner();
                }
            } catch (Exception ex) {
                logger.error("排班已保存，但同步钉钉失败: {}", ex.getMessage(), ex);
            }
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
            req1.setOpUserid(adminUserId);
            req1.setGroupKey(newGroup);
            req1.setUserIdList(userId);
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
            if (StringUtils.isEmpty(groupId)) {
                throw new Exception("排班缺少考勤组信息, classId=" + shiftId);
            }
            if (StringUtils.isEmpty(shiftId)) {
                throw new Exception("排班缺少班次信息, groupId=" + groupId);
            }
            if (workDate == null) {
                throw new Exception("排班缺少日期, groupId=" + groupId);
            }
            if (StringUtils.isEmpty(userIdd)) {
                throw new Exception("排班缺少人员, groupId=" + groupId + ", classId=" + shiftId);
            }
            String[] userIds = userIdd.split(",");
            for (int n = 0; n < userIds.length; n++) {
                String userId = userIds[n];
                if (StringUtils.isEmpty(userId)) {
                    continue;
                }
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
                if (rsp.getSuccess() == false) {
                    String msg = rsp.getMessage();
                    if (StringUtils.isEmpty(msg)) {
                        msg = rsp.getErrmsg();
                    }
                    throw new Exception("钉钉排班同步失败, groupId=" + groupId + ", shiftId=" + shiftId +
                            ", userId=" + userId + ", workDate=" + workDate.getTime() + ", message=" + msg);
                }
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

    @Override
    public PageObject<tbplanlist> loadBySelectedDate(Date selectedDate, boolean loadLast, Integer pageSize,
                                                     Integer pageNum, String sortField, String sortOrder) {
        if (selectedDate == null) {
            return getMaxDate(pageSize, pageNum, sortField, sortOrder);
        }
        if (pageSize == null || pageSize <= 0) pageSize = 20;
        if (pageNum == null || pageNum < 0) pageNum = 0;
        if (StringUtils.isEmpty(sortField)) sortField = "createTime";
        if (StringUtils.isEmpty(sortOrder)) sortOrder = "asc";

        Sort sort = "desc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        Date targetDay = selectedDate;
        if (loadLast) {
            tbplanlist nearest = planRep.findTopByWorkDateLessThanOrderByWorkDateDesc(startOfDay(selectedDate));
            if (nearest == null || nearest.getWorkDate() == null) {
                return PageObject.Of(new PageImpl<>(Collections.emptyList(), pageable, 0));
            }
            targetDay = nearest.getWorkDate();
        }

        Page<tbplanlist> page = planRep.findAllByWorkDateBetween(startOfDay(targetDay), endOfDay(targetDay), pageable);
        return PageObject.Of(page);
    }
}
