package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.enums.SalaryRecordStatus;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import com.tianye.hrsystem.modules.salary.vo.SalaryMonthRecoveryPreviewVO;
import com.tianye.hrsystem.modules.salary.vo.SalaryMonthRecoveryRecordVO;
import com.tianye.hrsystem.modules.salary.vo.SalaryMonthRecoveryResultVO;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SalaryMonthRecordRecoveryTest {

    @Test
    public void previewSalaryMonthRecovery_shouldAllowDeletingEmptyLaterMonthRecords() {
        TestableSalaryMonthRecordService service = new TestableSalaryMonthRecordService();
        HrmSalaryMonthRecord july = record(1001L, 2026, 7, SalaryRecordStatus.HISTORY.getValue());
        HrmSalaryMonthRecord august = record(2001L, 2026, 8, null);
        HrmSalaryMonthRecord september = record(3001L, 2026, 9, null);
        service.records.addAll(Arrays.asList(july, august, september));

        SalaryMonthRecoveryPreviewVO preview = service.previewSalaryMonthRecovery(2026, 7);

        Assert.assertTrue(preview.getRecoverable());
        Assert.assertEquals(Long.valueOf(1001L), preview.getTargetRecordId());
        Assert.assertEquals(Integer.valueOf(SalaryRecordStatus.CREATED.getValue()), preview.getRestoredCheckStatus());
        Assert.assertEquals(2, preview.getLaterRecords().size());
        Assert.assertEquals("2026-08", preview.getLaterRecords().get(0).getYearMonth());
        Assert.assertTrue(preview.getMessage().contains("2条"));
        for (SalaryMonthRecoveryRecordVO recordVO : preview.getLaterRecords()) {
            Assert.assertTrue(recordVO.getRecoverable());
            Assert.assertTrue(recordVO.getMessage().contains("可恢复"));
        }
    }

    @Test
    public void previewSalaryMonthRecovery_shouldBlockWhenLaterMonthsHavePayrollOrSlipData() {
        TestableSalaryMonthRecordService service = new TestableSalaryMonthRecordService();
        service.records.add(record(1001L, 2026, 7, SalaryRecordStatus.HISTORY.getValue()));
        service.records.add(record(2001L, 2026, 8, null));
        HrmSalaryMonthRecord september = record(3001L, 2026, 9, null);
        september.setIsSend(1);
        service.records.add(september);
        service.employeeRecordCounts.put(2001L, 4L);
        service.salarySlipRecordCounts.put(3001L, 1L);
        service.salarySlipCounts.put(3001L, 4L);

        SalaryMonthRecoveryPreviewVO preview = service.previewSalaryMonthRecovery(2026, 7);

        Assert.assertFalse(preview.getRecoverable());
        Assert.assertEquals(2, preview.getLaterRecords().size());
        Assert.assertFalse(preview.getLaterRecords().get(0).getRecoverable());
        Assert.assertTrue(preview.getLaterRecords().get(0).getMessage().contains("员工薪资明细"));
        Assert.assertFalse(preview.getLaterRecords().get(1).getRecoverable());
        Assert.assertTrue(preview.getBlockingReasons().stream().anyMatch(reason -> reason.contains("工资条发放记录")));
        Assert.assertTrue(preview.getBlockingReasons().stream().anyMatch(reason -> reason.contains("已标记发送工资条")));
    }

    @Test
    public void recoverSalaryMonth_shouldDeleteSafeLaterRecordsAndRestoreTargetStatus() {
        TestableSalaryMonthRecordService service = new TestableSalaryMonthRecordService();
        HrmSalaryMonthRecord july = record(1001L, 2026, 7, SalaryRecordStatus.HISTORY.getValue());
        service.records.add(july);
        service.records.add(record(2001L, 2026, 8, null));
        service.records.add(record(3001L, 2026, 9, null));
        service.employeeRecordCounts.put(1001L, 12L);

        SalaryMonthRecoveryResultVO result = service.recoverSalaryMonth(2026, 7);

        Assert.assertEquals(Arrays.asList(2001L, 3001L), service.deletedRecordIds);
        Assert.assertEquals(Arrays.asList("2026-08", "2026-09"), result.getDeletedMonths());
        Assert.assertEquals(Integer.valueOf(SalaryRecordStatus.COMPUTE.getValue()), result.getRestoredCheckStatus());
        Assert.assertEquals(Integer.valueOf(SalaryRecordStatus.COMPUTE.getValue()), service.updatedTarget.getCheckStatus());
        Assert.assertTrue(result.getMessage().contains("已恢复到2026-07"));
    }

    @Test
    public void recoverSalaryMonth_shouldThrowAndKeepRecordsWhenLaterMonthIsUnsafe() {
        TestableSalaryMonthRecordService service = new TestableSalaryMonthRecordService();
        service.records.add(record(1001L, 2026, 7, SalaryRecordStatus.HISTORY.getValue()));
        service.records.add(record(2001L, 2026, 8, null));
        service.employeeRecordCounts.put(2001L, 1L);

        try {
            service.recoverSalaryMonth(2026, 7);
            Assert.fail("expected HrmException");
        } catch (HrmException ex) {
            Assert.assertTrue(ex.getMsg().contains("不能自动恢复"));
        }

        Assert.assertTrue(service.deletedRecordIds.isEmpty());
        Assert.assertNull(service.updatedTarget);
    }

    private static HrmSalaryMonthRecord record(Long id, int year, int month, Integer checkStatus) {
        HrmSalaryMonthRecord record = new HrmSalaryMonthRecord();
        record.setSRecordId(id);
        record.setYear(year);
        record.setMonth(month);
        record.setTitle(month + "月薪资报表");
        record.setCheckStatus(checkStatus);
        record.setIsSend(0);
        return record;
    }

    private static class TestableSalaryMonthRecordService extends SalaryMonthRecordServiceNew {
        private final List<HrmSalaryMonthRecord> records = new ArrayList<>();
        private final Map<Long, Long> employeeRecordCounts = new HashMap<>();
        private final Map<Long, Long> salarySlipRecordCounts = new HashMap<>();
        private final Map<Long, Long> salarySlipCounts = new HashMap<>();
        private final List<Long> deletedRecordIds = new ArrayList<>();
        private HrmSalaryMonthRecord updatedTarget;

        @Override
        HrmSalaryMonthRecord findSalaryMonthRecordByYearAndMonth(Integer year, Integer month) {
            return records.stream()
                    .filter(record -> Objects.equals(record.getYear(), year))
                    .filter(record -> Objects.equals(record.getMonth(), month))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        List<HrmSalaryMonthRecord> findSalaryMonthRecordsAfter(Integer year, Integer month) {
            int targetIndex = year * 12 + month;
            List<HrmSalaryMonthRecord> result = new ArrayList<>();
            for (HrmSalaryMonthRecord record : records) {
                int recordIndex = record.getYear() * 12 + record.getMonth();
                if (recordIndex > targetIndex) {
                    result.add(record);
                }
            }
            return result;
        }

        @Override
        long countRecoveryEmployeeRecords(Long sRecordId) {
            return employeeRecordCounts.getOrDefault(sRecordId, 0L);
        }

        @Override
        long countRecoverySalarySlipRecords(Long sRecordId) {
            return salarySlipRecordCounts.getOrDefault(sRecordId, 0L);
        }

        @Override
        long countRecoverySalarySlips(Integer year, Integer month) {
            HrmSalaryMonthRecord record = findSalaryMonthRecordByYearAndMonth(year, month);
            return record == null ? 0L : salarySlipCounts.getOrDefault(record.getSRecordId(), 0L);
        }

        @Override
        void removeRecoverySalaryMonthRecords(List<Long> recordIds) {
            deletedRecordIds.addAll(recordIds == null ? Collections.emptyList() : recordIds);
        }

        @Override
        void updateRecoveredTargetMonthRecord(HrmSalaryMonthRecord targetRecord) {
            updatedTarget = targetRecord;
        }

        @Override
        protected <T> T runInTransaction(java.util.concurrent.Callable<T> callable) {
            try {
                return callable.call();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
