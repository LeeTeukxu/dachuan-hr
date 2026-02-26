package com.tianye.hrsystem.autoTask;

import com.tianye.hrsystem.autoTask.common.AbstractDingTalkTask;
import com.tianye.hrsystem.mapper.ddAccountMapper;
import com.tianye.hrsystem.model.HrmAttendanceReportData;
import com.tianye.hrsystem.model.HrmEmployeeLeaveRecord;
import com.tianye.hrsystem.model.HrmEmployeeOverTimeRecord;
import com.tianye.hrsystem.repository.hrmAttendanceReportDataRepository;
import com.tianye.hrsystem.repository.hrmAttendanceReportFieldRepository;
import com.tianye.hrsystem.repository.hrmEmployeeLeaveRecordRepository;
import com.tianye.hrsystem.repository.hrmEmployeeOverTimeRecordRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 生成加班/请假明细记录
 * 当前已禁用
 * 从报表数据中提取加班和请假信息，生成独立的明细记录
 */
@Component
public class CreateOverTimeAndLeaveTimeRecordTask extends AbstractDingTalkTask {

    @Autowired
    private hrmAttendanceReportFieldRepository fieldRep;
    @Autowired
    private hrmAttendanceReportDataRepository dataRep;
    @Autowired
    private hrmEmployeeLeaveRecordRepository leaveRep;
    @Autowired
    private hrmEmployeeOverTimeRecordRepository overTimeRep;
    @Autowired
    private ddAccountMapper ddMapper;

    private static final ThreadLocal<SimpleDateFormat> shortFormat =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    @Override
    protected String getTaskFieldName() {
        return null;
    }

    @Override
    protected boolean requireEndOfMonth() {
        return false;
    }

    @Override
    protected void doProcess(String companyId, Date currentDate) throws Exception {
        // 获取请假类型字段
        List<String> holidayFields = fieldRep.findAllByType(2).stream()
                .map(f -> f.getFieldName())
                .collect(Collectors.toList());

        Date reportSaveDate = ddMapper.getMaxReportDate();
        Date savedDate = ddMapper.getMaxSavedData();
        Date startDate;
        if (savedDate == null) {
            startDate = ddMapper.getMinReportDate();
        } else {
            startDate = DateUtils.addDays(savedDate, 1);
        }

        if (startDate == null || reportSaveDate == null) {
            logger.info("[CreateOverTimeAndLeaveTimeRecordTask][{}] 无报表数据需要处理", companyId);
            return;
        }

        for (Date d = startDate; d.before(reportSaveDate); d = DateUtils.addDays(d, 1)) {
            if (checkpoint.isDone(companyId, getClass(), d)) {
                continue;
            }

            try {
                Date begin = dateUtils.setItBegin(d);
                List<HrmAttendanceReportData> dataList = dataRep.findAllByWorkDate(begin);
                List<HrmEmployeeOverTimeRecord> overTimes = new ArrayList<>();
                List<HrmEmployeeLeaveRecord> leaves = new ArrayList<>();

                for (HrmAttendanceReportData data : dataList) {
                    processReportData(data, leaves, overTimes, holidayFields);
                }

                if (!leaves.isEmpty()) {
                    leaveRep.saveAll(leaves);
                    logger.info("[CreateOverTimeAndLeaveTimeRecordTask][{}] 生成{}条{}的请假记录",
                            companyId, leaves.size(), shortFormat.get().format(begin));
                }
                if (!overTimes.isEmpty()) {
                    overTimeRep.saveAll(overTimes);
                    logger.info("[CreateOverTimeAndLeaveTimeRecordTask][{}] 生成{}条{}的加班记录",
                            companyId, overTimes.size(), shortFormat.get().format(begin));
                }

                checkpoint.markDone(companyId, getClass(), d);

                // 有数据生成时暂停，避免一次处理太多
                if (!leaves.isEmpty() || !overTimes.isEmpty()) {
                    break;
                }
            } catch (Exception e) {
                logger.error("[CreateOverTimeAndLeaveTimeRecordTask][{}] 处理{}失败: {}",
                        companyId, shortFormat.get().format(d), e.getMessage());
                exceptionUtils.addOne(getClass(), e);
            }
        }
    }

    /**
     * 处理单条报表数据，提取加班或请假信息
     */
    private void processReportData(HrmAttendanceReportData data,
                                    List<HrmEmployeeLeaveRecord> leaves,
                                    List<HrmEmployeeOverTimeRecord> overTimes,
                                    List<String> holidayFields) {
        String val = data.getValue();
        String fieldName = data.getFieldName();

        if (!holidayFields.contains(fieldName) && !fieldName.endsWith("加班")) {
            return;
        }
        if (StringUtils.isEmpty(val) || val.equals("0.0")) {
            return;
        }

        if (holidayFields.contains(fieldName)) {
            HrmEmployeeLeaveRecord record = new HrmEmployeeLeaveRecord();
            record.setLeaveDay(Double.parseDouble(val));
            record.setLeaveType(fieldName);
            record.setEmployeeId(data.getEmpId());
            record.setCreateTime(data.getWorkDate());
            record.setCreateUserId(1L);
            record.setLeaveRecordId(System.currentTimeMillis());
            leaves.add(record);
        } else if (fieldName.endsWith("加班")) {
            HrmEmployeeOverTimeRecord record = new HrmEmployeeOverTimeRecord();
            record.setOverTimes(Double.parseDouble(val));
            if (fieldName.equals("工作日加班")) {
                record.setOverTimeType(1);
            } else if (fieldName.equals("休息日加班")) {
                record.setOverTimeType(2);
            } else if (fieldName.equals("节假日加班")) {
                record.setOverTimeType(3);
            }
            record.setEmployeeId(data.getEmpId());
            record.setCreateTime(data.getWorkDate());
            record.setAttendanceTime(data.getWorkDate());
            record.setCreateUserId(1L);
            record.setOverTimeId(System.currentTimeMillis());
            overTimes.add(record);
        }
    }

    // 当前已禁用
    // @Scheduled(cron = "0 0/2 * * * ?")
    // public void process() { execute(); }
}
