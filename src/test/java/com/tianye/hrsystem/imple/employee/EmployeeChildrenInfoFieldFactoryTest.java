package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.enums.FieldEnum;
import com.tianye.hrsystem.enums.LabelGroupEnum;
import com.tianye.hrsystem.entity.po.HrmEmployeeField;
import com.tianye.hrsystem.model.HrmFieldExtend;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class EmployeeChildrenInfoFieldFactoryTest {

    @Test
    public void createFieldDefinesChildrenInfoDetailTable() {
        HrmEmployeeField field = EmployeeChildrenInfoFieldFactory.createField(99);

        Assert.assertEquals("children_info", field.getFieldName());
        Assert.assertEquals("子女信息", field.getName());
        Assert.assertEquals(FieldEnum.DETAIL_TABLE.getType(), field.getType());
        Assert.assertEquals(LabelGroupEnum.PERSONAL.getValue(), field.getLabelGroup().intValue());
        Assert.assertEquals(Integer.valueOf(0), field.getIsFixed());
        Assert.assertEquals(Integer.valueOf(0), field.getIsHidden());
        Assert.assertEquals(Integer.valueOf(1), field.getIsUpdateValue());
        Assert.assertEquals(Integer.valueOf(1), field.getIsEmployeeVisible());
        Assert.assertEquals(Integer.valueOf(1), field.getIsEmployeeUpdate());
        Assert.assertEquals(Integer.valueOf(99), field.getSorting());
    }

    @Test
    public void createExtendsDefinesNameSexAndBirthDate() {
        List<HrmFieldExtend> extendsList = EmployeeChildrenInfoFieldFactory.createFieldExtends(123L);

        Assert.assertEquals(3, extendsList.size());
        assertExtend(extendsList.get(0), 123, "childName", "姓名", FieldEnum.TEXT.getType(), 1);
        assertExtend(extendsList.get(1), 123, "childSex", "性别", FieldEnum.SELECT.getType(), 2);
        Assert.assertTrue(extendsList.get(1).getOptions().contains("男"));
        Assert.assertTrue(extendsList.get(1).getOptions().contains("女"));
        assertExtend(extendsList.get(2), 123, "childBirthDate", "出生日期", FieldEnum.DATE.getType(), 3);
    }

    private void assertExtend(HrmFieldExtend fieldExtend, Integer parentFieldId, String fieldName, String name, Integer type, Integer sorting) {
        Assert.assertEquals(parentFieldId, fieldExtend.getParentFieldId());
        Assert.assertEquals(fieldName, fieldExtend.getFieldName());
        Assert.assertEquals(name, fieldExtend.getName());
        Assert.assertEquals(type, fieldExtend.getType());
        Assert.assertEquals(sorting, fieldExtend.getSorting());
        Assert.assertEquals(Integer.valueOf(0), fieldExtend.getIsHidden());
    }
}
