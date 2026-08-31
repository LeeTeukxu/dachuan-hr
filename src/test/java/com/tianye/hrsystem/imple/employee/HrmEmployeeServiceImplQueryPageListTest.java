package com.tianye.hrsystem.imple.employee;

import com.alibaba.fastjson.JSONObject;
import cn.hutool.core.date.DateUtil;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryEmployeePageListBO;
import com.tianye.hrsystem.mapper.HrmEmployeeMapper;
import com.tianye.hrsystem.service.employee.IHrmEmployeeDataService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeFieldService;
import com.tianye.hrsystem.util.EmployeeUtil;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

public class HrmEmployeeServiceImplQueryPageListTest {

    @Test
    public void queryPageList_shouldIgnoreDynamicFieldsWithoutFieldName() throws Exception {
        HrmEmployeeServiceImpl service = new HrmEmployeeServiceImpl();
        setField(service, "employeeUtil", new EmployeeUtil() {
            @Override
            public Collection<Long> queryDataAuthEmpIdByMenuId(Long menuId) {
                return Collections.singletonList(101L);
            }
        });
        setField(service, "employeeMapper", createEmployeeMapperProxy());
        setField(service, "employeeDataService", createEmployeeDataServiceProxy());
        setField(service, "employeeFieldService", createEmployeeFieldServiceProxy());

        QueryEmployeePageListBO request = new QueryEmployeePageListBO();
        request.setPage(6L);
        request.setLimit(15L);

        BasePage<Map<String, Object>> page = service.queryPageList(request);

        Map<String, Object> row = page.getList().get(0);
        Assert.assertFalse("员工列表不能写入 null key，否则第 6 页 JSON 序列化会失败", row.containsKey(null));
        Assert.assertEquals("正常自定义字段仍需保留", "正常值", row.get("customName"));
        Assert.assertEquals("员工动态字段 SQL 返回蛇形 key 时仍需保留", "蛇形值", row.get("snakeCaseField"));
        Assert.assertEquals("身份证员工列表需从证件号兜底返回出生日期", "1949-12-31", row.get("dateOfBirth"));
        Assert.assertEquals("身份证员工列表需从证件号兜底返回生日", "1231", row.get("birthday"));
        Assert.assertEquals("身份证员工列表需从证件号兜底返回年龄", DateUtil.ageOfNow("1949-12-31"), row.get("age"));
        Assert.assertEquals("历史员工未维护司龄开始日期时，列表应回退使用入职日期", "2021-09-14", row.get("companyAgeStartTime"));
        Assert.assertNotNull("历史员工未维护司龄开始日期时，列表仍应动态计算司龄", row.get("companyAge"));
    }

    private HrmEmployeeMapper createEmployeeMapperProxy() {
        return (HrmEmployeeMapper) Proxy.newProxyInstance(
                HrmEmployeeMapper.class.getClassLoader(),
                new Class<?>[]{HrmEmployeeMapper.class},
                (proxy, method, args) -> {
                    if ("queryPageList".equals(method.getName())) {
                        BasePage<Map<String, Object>> page = (BasePage<Map<String, Object>>) args[0];
                        Map<String, Object> row = new HashMap<>();
                        row.put("employeeId", 101L);
                        row.put("idType", 1);
                        row.put("idNumber", "11010519491231002X");
                        row.put("entryTime", "2021-09-14");
                        page.setRecords(Collections.singletonList(row));
                        page.setTotal(1L);
                        return page;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private IHrmEmployeeDataService createEmployeeDataServiceProxy() {
        return (IHrmEmployeeDataService) Proxy.newProxyInstance(
                IHrmEmployeeDataService.class.getClassLoader(),
                new Class<?>[]{IHrmEmployeeDataService.class},
                (proxy, method, args) -> {
                    if ("queryFiledListByEmployeeId".equals(method.getName())) {
                        JSONObject missingFieldName = new JSONObject();
                        missingFieldName.put("fieldValueDesc", "异常值");
                        missingFieldName.put("type", "1");

                        JSONObject normalField = new JSONObject();
                        normalField.put("fieldName", "custom_name");
                        normalField.put("fieldValueDesc", "正常值");
                        normalField.put("type", "1");

                        JSONObject snakeCaseField = new JSONObject();
                        snakeCaseField.put("field_name", "snake_case_field");
                        snakeCaseField.put("field_value", "蛇形值");
                        snakeCaseField.put("field_value_desc", "");
                        snakeCaseField.put("type", "1");

                        return Arrays.asList(missingFieldName, normalField, snakeCaseField);
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private IHrmEmployeeFieldService createEmployeeFieldServiceProxy() {
        return (IHrmEmployeeFieldService) Proxy.newProxyInstance(
                IHrmEmployeeFieldService.class.getClassLoader(),
                new Class<?>[]{IHrmEmployeeFieldService.class},
                (proxy, method, args) -> {
                    if ("convertValueByFormType".equals(method.getName())) {
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType.equals(Boolean.TYPE)) {
            return false;
        }
        if (returnType.equals(Integer.TYPE) || returnType.equals(Long.TYPE) || returnType.equals(Short.TYPE) || returnType.equals(Byte.TYPE)) {
            return 0;
        }
        if (returnType.equals(Double.TYPE) || returnType.equals(Float.TYPE)) {
            return 0D;
        }
        return null;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
