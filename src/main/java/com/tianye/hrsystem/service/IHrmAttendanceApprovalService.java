package com.tianye.hrsystem.service;

import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddAttendanceApprovalBO;
import com.tianye.hrsystem.entity.bo.AttendanceApprovalMonthBO;
import com.tianye.hrsystem.entity.bo.DeleteAttendanceApprovalBO;
import com.tianye.hrsystem.entity.bo.QueryAttendanceApprovalPageBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalDurationBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalStatisticsStatusBO;
import com.tianye.hrsystem.entity.bo.UpdateAttendanceApprovalSubtypeBO;
import com.tianye.hrsystem.entity.vo.AttendanceApprovalMonthPortionVO;
import com.tianye.hrsystem.entity.vo.QueryAttendanceApprovalPageVO;

import java.time.YearMonth;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IHrmAttendanceApprovalService {

    BasePage<QueryAttendanceApprovalPageVO> queryPageList(QueryAttendanceApprovalPageBO queryBO);

    /**
     * 计算一条审批单在指定月份的展示区间与时长（跨月拆分口径的唯一入口，现算不落库）。
     * 区间与所选月无交集、或时间/时长非法时返回 null。
     *
     * @param employeeId 员工ID（决定走行政单双休日历还是排班剔除）
     */
    AttendanceApprovalMonthPortionVO calculateMonthPortion(Long employeeId,
                                                           Date beginTime,
                                                           Date endTime,
                                                           String duration,
                                                           String durationDay,
                                                           YearMonth month);

    Map<String, Object> checkMonthData(AttendanceApprovalMonthBO queryBO);

    Map<String, Object> fetchMonthData(AttendanceApprovalMonthBO queryBO) throws Exception;

    /** 提交路径同步抢占按公司运行标记；false = 该公司已有获取任务在运行 */
    boolean tryBeginFetch(String companyId);

    /** 后台任务结束（成功/失败/提交失败）后归还可公司运行标记 */
    void finishFetch(String companyId);

    /** 提交后同步写入 RUNNING 进度，覆盖上一次运行的旧完成状态 */
    void beginFetchProgress();

    /** 带自动重试的审批获取入口：失败后指数退避自动重试（重复拉取幂等） */
    Map<String, Object> fetchMonthDataWithAutoRetry(AttendanceApprovalMonthBO queryBO) throws Exception;

    Map<String, Object> queryFetchProgress();

    List<String> querySubtypeOptions();

    Map<String, Object> updateSubtype(UpdateAttendanceApprovalSubtypeBO updateBO);

    Map<String, Object> updateDuration(UpdateAttendanceApprovalDurationBO updateBO);

    Map<String, Object> updateStatisticsStatus(UpdateAttendanceApprovalStatisticsStatusBO updateBO);

    Map<String, Object> addManualApproval(AddAttendanceApprovalBO addBO);

    Map<String, Object> deleteApproval(DeleteAttendanceApprovalBO deleteBO);
}
