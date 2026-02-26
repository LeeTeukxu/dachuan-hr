package com.tianye.hrsystem.modules.salary.service;

import com.tianye.hrsystem.modules.salary.entity.SalaryBaseTotal;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.*;

public class SalaryComputeServiceNewTest {

    @Test
    public void baseComputeSalaryFromMemory_shouldPaySalary_basicCase() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, "5000", 1, 1));
        items.add(buildDto(10102, 10, "3000", 1, 1));
        items.add(buildDto(100101, 100, "500", 0, 1));
        items.add(buildDto(100102, 100, "300", 0, 1));
        items.add(buildDto(280, 280, "200", 0, 0));
        items.add(buildDto(282, 282, "100", 0, 0));

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("8000")));
        Assert.assertEquals(0, result.getProxyPaySalary().compareTo(new BigDecimal("800")));
        Assert.assertEquals(0, result.getOtherNoTaxDeductions().compareTo(new BigDecimal("200")));
        Assert.assertEquals(0, result.getTotalloanMoney().compareTo(new BigDecimal("100")));
    }

    @Test
    public void baseComputeSalaryFromMemory_withSubtractItem_shouldDeduct() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, "5000", 1, 1));
        items.add(buildDto(180101, 180, "300", 0, 1));

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("4700")));
    }

    @Test
    public void baseComputeSalaryFromMemory_emptyList_shouldReturnZeros() {
        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(
            Collections.emptyList(), null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, result.getProxyPaySalary().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void baseComputeSalaryFromMemory_nullValue_shouldTreatAsZero() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, null, 1, 1));
        items.add(buildDto(10102, 10, "3000", 1, 1));

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("3000")));
    }

    @Test
    public void baseComputeSalaryFromMemory_nonNumericValue_shouldTreatAsZero() {
        List<ComputeSalaryDto> items = new ArrayList<>();
        items.add(buildDto(10101, 10, "abc", 1, 1));
        items.add(buildDto(10102, 10, "3000", 1, 1));

        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(items, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(new BigDecimal("3000")));
    }

    @Test
    public void baseComputeSalaryFromMemory_nullList_shouldReturnZeros() {
        SalaryBaseTotal result = SalaryComputeServiceNew.baseComputeSalaryFromMemory(null, null);

        Assert.assertEquals(0, result.getShouldPaySalary().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(0, result.getProxyPaySalary().compareTo(BigDecimal.ZERO));
    }

    private ComputeSalaryDto buildDto(int code, int parentCode, String value, int isPlus, int isTax) {
        ComputeSalaryDto dto = new ComputeSalaryDto();
        dto.setCode(code);
        dto.setParentCode(parentCode);
        dto.setValue(value);
        dto.setIsPlus(isPlus);
        dto.setIsTax(isTax);
        return dto;
    }
}
