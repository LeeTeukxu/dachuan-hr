package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.enums.TaxType;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryConfig;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SalaryMonthRecordNextMonthTest {

    @Test
    public void createNextSalaryMonthRecord_shouldCreateFromSourceMonth() {
        TestableSalaryMonthRecordService service = buildService();
        service.salaryEmployeeCount = 3;
        HrmSalaryMonthRecord sourceRecord = new HrmSalaryMonthRecord();
        sourceRecord.setSRecordId(1001L);
        sourceRecord.setYear(2026);
        sourceRecord.setMonth(7);

        HrmSalaryMonthRecord nextRecord = service.createNextSalaryMonthRecord(sourceRecord);

        Assert.assertSame(service.savedRecord, nextRecord);
        Assert.assertEquals(Integer.valueOf(2026), service.savedRecord.getYear());
        Assert.assertEquals(Integer.valueOf(8), service.savedRecord.getMonth());
        Assert.assertEquals(LocalDate.of(2026, 8, 1), service.savedRecord.getStartTime());
        Assert.assertEquals(LocalDate.of(2026, 8, 31), service.savedRecord.getEndTime());
        Assert.assertEquals(Integer.valueOf(3), service.savedRecord.getNum());
        Mockito.verify(service.salaryActionRecordServiceMock, Mockito.times(1))
                .addNextMonthSalaryLog(service.savedRecord);
    }

    @Test
    public void createNextSalaryMonthRecord_shouldReuseExistingNextMonthRecord() {
        TestableSalaryMonthRecordService service = buildService();
        HrmSalaryMonthRecord existingNextRecord = new HrmSalaryMonthRecord();
        existingNextRecord.setSRecordId(2001L);
        existingNextRecord.setYear(2026);
        existingNextRecord.setMonth(8);
        service.existingNextRecord = existingNextRecord;

        HrmSalaryMonthRecord sourceRecord = new HrmSalaryMonthRecord();
        sourceRecord.setSRecordId(1001L);
        sourceRecord.setYear(2026);
        sourceRecord.setMonth(7);

        HrmSalaryMonthRecord nextRecord = service.createNextSalaryMonthRecord(sourceRecord);

        Assert.assertSame(existingNextRecord, nextRecord);
        Assert.assertNull("次月已存在时不得再次保存薪资月记录", service.savedRecord);
        Mockito.verify(service.salaryActionRecordServiceMock, Mockito.never())
                .addNextMonthSalaryLog(Mockito.any(HrmSalaryMonthRecord.class));
    }

    private TestableSalaryMonthRecordService buildService() {
        TestableSalaryMonthRecordService service = new TestableSalaryMonthRecordService();
        IHrmSalaryConfigService salaryConfigService = Mockito.mock(IHrmSalaryConfigService.class);
        HrmSalaryConfig salaryConfig = new HrmSalaryConfig();
        salaryConfig.setSalaryCycleStartDay(1);
        Mockito.when(salaryConfigService.getOne(Mockito.any())).thenReturn(salaryConfig);
        service.salaryActionRecordServiceMock = Mockito.mock(SalaryActionRecordService.class);
        ReflectionTestUtils.setField(service, "hrmSalaryConfigService", salaryConfigService);
        ReflectionTestUtils.setField(service, "salaryActionRecordService", service.salaryActionRecordServiceMock);
        return service;
    }

    private static class TestableSalaryMonthRecordService extends SalaryMonthRecordServiceNew {
        private HrmSalaryMonthRecord existingNextRecord;
        private HrmSalaryMonthRecord savedRecord;
        private SalaryActionRecordService salaryActionRecordServiceMock;
        private int salaryEmployeeCount;

        @Override
        HrmSalaryMonthRecord findSalaryMonthRecordByYearAndMonth(Integer year, Integer month) {
            if (existingNextRecord != null
                    && Objects.equals(existingNextRecord.getYear(), year)
                    && Objects.equals(existingNextRecord.getMonth(), month)) {
                return existingNextRecord;
            }
            return null;
        }

        @Override
        public boolean save(HrmSalaryMonthRecord entity) {
            this.savedRecord = entity;
            return true;
        }

        @Override
        public List<Map<String, Object>> queryPaySalaryEmployeeListByType(Integer type, TaxType taxType) {
            List<Map<String, Object>> employees = new ArrayList<>();
            for (int i = 0; i < salaryEmployeeCount; i++) {
                employees.add(new HashMap<>());
            }
            return employees;
        }
    }
}
