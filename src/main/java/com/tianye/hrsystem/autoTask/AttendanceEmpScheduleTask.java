package com.tianye.hrsystem.autoTask;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceScheduleListbyusersRequest;
import com.dingtalk.api.response.OapiAttendanceScheduleListbyusersResponse;
import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.entity.po.HrmEmpSchedule;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.service.IHrmEmpScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 同步员工排班记录
 * 当前已禁用
 */
@Component
public class AttendanceEmpScheduleTask extends AbstractDingTalkTask {

    @Autowired
    private IHrmEmpScheduleService hrmEmpScheduleService;

    @Value("${hrm.system.adminId}")
    private String adminId;

    @Override
    protected String getTaskFieldName() {
        return null; // 不需要进度追踪
    }

    @Override
    protected boolean requireEndOfMonth() {
        return false;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date beginDate = cal.getTime();

        logger.info("[AttendanceEmpScheduleTask][{}] 开始同步员工排班信息", companyId);

        String password = accessToken.Refresh();
        List<tbattendanceuser> users = attendanceUserRep.findAll();
        List<String> userIdList = users.stream()
                .map(tbattendanceuser::getUserId)
                .collect(Collectors.toList());

        // 每次最多查50人
        List<List<String>> batches = divide(userIdList, 50);
        int savedCount = 0;

        for (List<String> batch : batches) {
            rateLimiter.acquire(companyId);

            String userIdStr = String.join(",", batch);
            DingTalkClient client = new DefaultDingTalkClient(
                    "https://oapi.dingtalk.com/topapi/attendance/schedule/listbyusers");
            OapiAttendanceScheduleListbyusersRequest req = new OapiAttendanceScheduleListbyusersRequest();
            req.setOpUserId(adminId);
            req.setUserids(userIdStr);
            req.setFromDateTime(beginDate.getTime());
            req.setToDateTime(currentDate.getTime());

            OapiAttendanceScheduleListbyusersResponse rsp = client.execute(req, password);

            if (rsp != null && rsp.getErrcode() == 0L) {
                List<OapiAttendanceScheduleListbyusersResponse.TopScheduleVo> list = rsp.getResult();
                if (!CollectionUtils.isEmpty(list)) {
                    for (OapiAttendanceScheduleListbyusersResponse.TopScheduleVo vo : list) {
                        Optional<tbattendanceuser> findUser = users.stream()
                                .filter(u -> u.getUserId().equals(vo.getUserid()))
                                .findFirst();
                        if (findUser.isPresent()) {
                            HashMap<String, Object> params = new HashMap<>();
                            params.put("employeeId", findUser.get().getEmpId());
                            params.put("workDate", vo.getWorkDate());
                            HrmEmpSchedule existing = hrmEmpScheduleService.getEmpScheduleByParams(params);
                            if (existing == null) {
                                HrmEmpSchedule schedule = new HrmEmpSchedule();
                                schedule.setEmpId(findUser.get().getEmpId());
                                schedule.setId(vo.getId());
                                schedule.setWorkDate(vo.getWorkDate());
                                schedule.setPlanCheckTime(vo.getPlanCheckTime());
                                schedule.setIsRest(vo.getIsRest());
                                schedule.setClassId(vo.getShiftId());
                                schedule.setGroupId(vo.getGroupId());
                                schedule.setCheckType(vo.getCheckType());
                                hrmEmpScheduleService.save(schedule);
                                savedCount++;
                            }
                        }
                    }
                }
            }
        }

        logger.info("[AttendanceEmpScheduleTask][{}] 完成，保存{}条排班记录", companyId, savedCount);
    }

    /**
     * 集合拆分
     */
    private static <T> List<List<T>> divide(List<T> origin, int size) {
        if (CollectionUtils.isEmpty(origin)) return Collections.emptyList();
        int block = (origin.size() + size - 1) / size;
        return IntStream.range(0, block)
                .boxed()
                .map(i -> {
                    int start = i * size;
                    int end = Math.min(start + size, origin.size());
                    return origin.subList(start, end);
                })
                .collect(Collectors.toList());
    }

    // 当前已禁用
    // @Scheduled(cron = "0 0 23 * * ?")
    // public void process() { execute(); }
}
