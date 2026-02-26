package com.tianye.hrsystem.imple.ddTalk;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceListscheduleRequest;
import com.dingtalk.api.response.OapiAttendanceListscheduleResponse;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.common.DDTalkResposeLogger;
import com.tianye.hrsystem.mapper.HrmDeptMapper;
import com.tianye.hrsystem.model.HrmAttendancePlan;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendancePlanRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.util.MyDateUtils;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: AttendancePlanRecord
 * @Author: 肖新民
 * @*TODO:同步排班。
 * @CreateTime: 2024年04月18日 15:26
 **/
@Component
public class AttendancePlanRecord {
    @Autowired
    IAccessToken token;
    @Autowired
    hrmAttendancePlanRepository planRep;

    @Autowired
    tbattendanceuserRepository userRep;



    @Autowired
    HrmDeptMapper deptMapper;
    @Autowired
    TransactionTemplate transactionTemplate;
    Logger logger= LoggerFactory.getLogger(AttendancePlanRecord.class);
    SimpleDateFormat shortFormat=new SimpleDateFormat("yyyy-MM-dd");
    @Autowired
    DDTalkResposeLogger ddLogger;
    @Autowired
    MyDateUtils dateUtils;
    List<tbattendanceuser> users = null;
    
    // 使用共享的全局锁，确保所有考勤数据库写操作串行化
    
    public void setUsers(List<tbattendanceuser> users) {
        this.users = users;
    }
    public void GetAndSave(Date WorkDate) throws ApiException {
        String password = token.Refresh();
        Long offset = 0L;
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/listschedule");

        while (true) {
            OapiAttendanceListscheduleRequest req = new OapiAttendanceListscheduleRequest();
            req.setWorkDate(WorkDate);
            req.setOffset(offset);
            req.setSize(200L);
            OapiAttendanceListscheduleResponse rsp = client.execute(req, password);
            Date begin=dateUtils.getCurrent();
            ddLogger.Info(rsp,((DefaultDingTalkClient)client).getRequestUrl(),begin,HrmAttendanceReportManager.class);
            if (rsp.isSuccess()) {
                OapiAttendanceListscheduleResponse.AtScheduleListForTopVo vo = rsp.getResult();
                List<OapiAttendanceListscheduleResponse.AtScheduleForTopVo> vs = vo.getSchedules();
                List<HrmAttendancePlan> ps=new ArrayList<>();
                for(int i=0;i<vs.size();i++){
                    OapiAttendanceListscheduleResponse.AtScheduleForTopVo v=vs.get(i);
                    HrmAttendancePlan plan=new HrmAttendancePlan();
                    plan.setPlanId(v.getPlanId());
                    plan.setCheckType(v.getCheckType());
                    plan.setPlanCheckTime(v.getPlanCheckTime());
                    plan.setUserId(v.getUserid());
                    Optional<tbattendanceuser> findUsers=users.stream().filter(f->f.getUserId().equals(v.getUserid())).findFirst();
                    if(findUsers.isPresent()){
                        tbattendanceuser user= findUsers.get();
                        plan.setEmpId(user.getEmpId());
                    }
                    plan.setClassId(v.getClassId());
                    plan.setClassSettingId(v.getClassSettingId());
                    plan.setGroupId(v.getGroupId());
                    plan.setCreateUser(1L);
                    plan.setCreateTime(new Date());
                    plan.setWorkDate(WorkDate);
                    ps.add(plan);
                }
                if(ps.size()>0){
                    planRep.saveAll(ps);
                    logger.info("保存记录:"+Integer.toString(ps.size()));
                }
                if (vo.getHasMore()) {
                    offset += 200L;
                } else break;
            } else break;
        }
    }
    /**
     * 获取并保存考勤计划数据
     * 策略：API调用并行 + 数据库写操作串行化
     * 这样既利用了并发获取数据的优势，又避免了MySQL锁冲突
     */
    public void GetAndSave(String  EmpIDS,Date WorkDate) throws ApiException {
        String password = token.Refresh();
        Long offset = 0L;
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/listschedule");
        List<Long> EmpIDD= Arrays.stream(EmpIDS.split(",")).map(f->Long.parseLong(f)).collect(Collectors.toList());
        List<String> UserIDS= users.stream().filter(f->EmpIDD.contains(f.getEmpId())).map(f->f.getUserId()).distinct().collect(Collectors.toList());

        // 先收集所有要保存的数据（API调用是并行的）
        List<HrmAttendancePlan> allPlans = new ArrayList<>();
        
        while (true) {
            OapiAttendanceListscheduleRequest req = new OapiAttendanceListscheduleRequest();
            req.setWorkDate(WorkDate);
            req.setOffset(offset);
            req.setSize(200L);
            OapiAttendanceListscheduleResponse rsp = client.execute(req, password);
            if (rsp.isSuccess()) {
                OapiAttendanceListscheduleResponse.AtScheduleListForTopVo vo = rsp.getResult();
                List<OapiAttendanceListscheduleResponse.AtScheduleForTopVo> vs = vo.getSchedules();
                for(int i=0;i<vs.size();i++){
                    OapiAttendanceListscheduleResponse.AtScheduleForTopVo v=vs.get(i);
                    String UID=v.getUserid();
                    if(UserIDS.contains(UID)==false)continue;

                    HrmAttendancePlan plan=new HrmAttendancePlan();
                    Optional<tbattendanceuser> findUsers=users.stream().filter(f->f.getUserId().equals(v.getUserid())).findFirst();
                    if(findUsers.isPresent()){
                        tbattendanceuser user= findUsers.get();
                        plan.setEmpId(user.getEmpId());
                    }

                    plan.setPlanId(v.getPlanId());
                    plan.setCheckType(v.getCheckType());
                    plan.setPlanCheckTime(v.getPlanCheckTime());
                    plan.setUserId(v.getUserid());
                    plan.setClassId(v.getClassId());
                    plan.setClassSettingId(v.getClassSettingId());
                    plan.setGroupId(v.getGroupId());
                    plan.setCreateUser(1L);
                    plan.setCreateTime(new Date());
                    plan.setWorkDate(WorkDate);
                    allPlans.add(plan);
                }
                if (vo.getHasMore()) {
                    offset += 200L;
                } else break;
            } else break;
        }
        
        // 数据库写操作串行化（使用全局锁），避免InnoDB索引锁冲突
        // 这部分执行很快（毫秒级），不影响整体性能
        synchronized (com.tianye.hrsystem.common.AttendanceDbLock.LOCK) {
            transactionTemplate.execute(status -> {
                // 先删除
                planRep.deleteAllByEmpIdInAndWorkDate(EmpIDD, WorkDate);
                // 再保存
                if (!allPlans.isEmpty()) {
                    planRep.saveAll(allPlans);
                    logger.info("保存记录:" + allPlans.size());
                }
                return null;
            });
        }
    }
    
    /**
     * 获取并保存考勤计划数据（不执行删除，由外部统一删除）
     * 用于多线程环境，避免重复删除导致锁冲突
     */
    public void GetAndSaveWithoutDelete(String  EmpIDS,Date WorkDate) throws ApiException {
        String password = token.Refresh();
        Long offset = 0L;
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/listschedule");
        List<Long> EmpIDD= Arrays.stream(EmpIDS.split(",")).map(f->Long.parseLong(f)).collect(Collectors.toList());
        List<String> UserIDS= users.stream().filter(f->EmpIDD.contains(f.getEmpId())).map(f->f.getUserId()).distinct().collect(Collectors.toList());

        // 不执行删除，由外部统一处理
        
        while (true) {
            OapiAttendanceListscheduleRequest req = new OapiAttendanceListscheduleRequest();
            req.setWorkDate(WorkDate);
            req.setOffset(offset);
            req.setSize(200L);
            OapiAttendanceListscheduleResponse rsp = client.execute(req, password);
            if (rsp.isSuccess()) {
                OapiAttendanceListscheduleResponse.AtScheduleListForTopVo vo = rsp.getResult();
                List<OapiAttendanceListscheduleResponse.AtScheduleForTopVo> vs = vo.getSchedules();
                List<HrmAttendancePlan> ps=new ArrayList<>();
                for(int i=0;i<vs.size();i++){
                    OapiAttendanceListscheduleResponse.AtScheduleForTopVo v=vs.get(i);
                    String UID=v.getUserid();
                    if(UserIDS.contains(UID)==false)continue;


                    HrmAttendancePlan plan=new HrmAttendancePlan();
                    Optional<tbattendanceuser> findUsers=users.stream().filter(f->f.getUserId().equals(v.getUserid())).findFirst();
                    if(findUsers.isPresent()){
                        tbattendanceuser user= findUsers.get();
                        plan.setEmpId(user.getEmpId());
                    }


                    plan.setPlanId(v.getPlanId());
                    plan.setCheckType(v.getCheckType());
                    plan.setPlanCheckTime(v.getPlanCheckTime());
                    plan.setUserId(v.getUserid());


                    plan.setClassId(v.getClassId());
                    plan.setClassSettingId(v.getClassSettingId());
                    plan.setGroupId(v.getGroupId());
                    plan.setCreateUser(1L);
                    plan.setCreateTime(new Date());
                    plan.setWorkDate(WorkDate);
                    ps.add(plan);
                }
                if(ps.size()>0){
                    final List<HrmAttendancePlan> plansToSave = ps;
                    transactionTemplate.execute(status -> {
                        planRep.saveAll(plansToSave);
                        logger.info("保存记录:" + plansToSave.size());
                        return null;
                    });
                }
                if (vo.getHasMore()) {
                    offset += 200L;
                } else break;
            } else break;
        }
    }
    
    /**
     * 批量删除（事务方法）
     * 使用 REQUIRES_NEW 强制创建新事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteBatch(List<Long> empIds, Date workDate) {
        planRep.deleteAllByEmpIdInAndWorkDate(empIds, workDate);
    }
    
    /**
     * 批量保存（事务方法）
     * 使用 REQUIRES_NEW 强制创建新事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(List<HrmAttendancePlan> plans) {
        planRep.saveAll(plans);
        logger.info("保存记录:" + plans.size());
    }

    public void DeleteRepeatUser(Date Begin,Date End){
        List<String> RepeatUsers=deptMapper.getRepeatUser();
        List<String> EmptyUsers=deptMapper.getEmptyPlanUser(shortFormat.format(Begin), shortFormat.format(End));
        
        // 收集要删除的用户ID
        List<Integer> userIdsToDelete = new ArrayList<>();
        for(int i=0;i< RepeatUsers.size();i++){
            String UserName= RepeatUsers.get(i);
            List<tbattendanceuser> users=userRep.findAllByUserName(UserName);
            for(int n=0;n<users.size();n++){
                tbattendanceuser user=users.get(n);
                String userId=user.getUserId();
                if(EmptyUsers.contains(userId)){
                    userIdsToDelete.add(user.getId());
                }
            }
        }
        
        // 批量删除，避免并发问题，每个删除操作独立事务
        if (!userIdsToDelete.isEmpty()) {
            for (Integer id : userIdsToDelete) {
                try {
                    // 使用独立的事务删除每个用户
                    transactionTemplate.execute(status -> {
                        try {
                            userRep.deleteById(id);
                            return null;
                        } catch (Exception e) {
                            // 忽略已删除的记录
                            logger.debug("用户已删除或不存在: {}", id);
                            return null;
                        }
                    });
                } catch (Exception e) {
                    // 忽略事务提交失败，多线程环境下可能重复删除
                    logger.debug("删除用户事务失败: {}", id);
                }
            }
            logger.info("删除重复用户: {} 个", userIdsToDelete.size());
        }
    }
}
