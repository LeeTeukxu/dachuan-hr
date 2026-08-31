package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.entity.po.HrmEmployeeField;
import com.tianye.hrsystem.enums.FieldEnum;
import com.tianye.hrsystem.enums.LabelGroupEnum;
import com.tianye.hrsystem.model.HrmFieldExtend;

import java.util.Arrays;
import java.util.List;

final class EmployeeChildrenInfoFieldFactory {

    static final String FIELD_NAME = "children_info";
    static final String FIELD_DISPLAY_NAME = "子女信息";
    static final String CHILD_NAME_FIELD = "childName";
    static final String CHILD_SEX_FIELD = "childSex";
    static final String CHILD_BIRTH_DATE_FIELD = "childBirthDate";
    static final long DEFAULT_FIELD_ID = 900001L;
    static final int DEFAULT_SORTING = 99;

    private static final String CHILD_SEX_OPTIONS = "[{\"name\":\"男\",\"value\":1},{\"name\":\"女\",\"value\":2}]";

    private EmployeeChildrenInfoFieldFactory() {
    }

    static HrmEmployeeField createField(int sorting) {
        HrmEmployeeField field = new HrmEmployeeField();
        field.setFieldName(FIELD_NAME);
        field.setName(FIELD_DISPLAY_NAME);
        field.setType(FieldEnum.DETAIL_TABLE.getType());
        field.setComponentType(0);
        field.setLabel(1);
        field.setLabelGroup(LabelGroupEnum.PERSONAL.getValue());
        field.setRemark("添加子女信息");
        field.setInputTips("");
        field.setMaxLength(255);
        field.setDefaultValue("[]");
        field.setIsUnique(0);
        field.setIsNull(0);
        field.setSorting(sorting);
        field.setOptions("");
        field.setIsFixed(0);
        field.setOperating(0);
        field.setIsHidden(0);
        field.setIsUpdateValue(1);
        field.setIsHeadField(0);
        field.setIsImportField(0);
        field.setIsEmployeeVisible(1);
        field.setIsEmployeeUpdate(1);
        field.setStylePercent(1);
        field.setPrecisions(2);
        field.setFormPosition("");
        field.setMaxNumRestrict("");
        field.setMinNumRestrict("");
        return field;
    }

    static List<HrmFieldExtend> createFieldExtends(Long parentFieldId) {
        Integer parentId = Math.toIntExact(parentFieldId);
        return Arrays.asList(
                createExtend(parentId, CHILD_NAME_FIELD, "姓名", FieldEnum.TEXT, "", 1),
                createExtend(parentId, CHILD_SEX_FIELD, "性别", FieldEnum.SELECT, CHILD_SEX_OPTIONS, 2),
                createExtend(parentId, CHILD_BIRTH_DATE_FIELD, "出生日期", FieldEnum.DATE, "", 3)
        );
    }

    private static HrmFieldExtend createExtend(Integer parentFieldId, String fieldName, String name, FieldEnum fieldEnum, String options, Integer sorting) {
        HrmFieldExtend fieldExtend = new HrmFieldExtend();
        fieldExtend.setParentFieldId(parentFieldId);
        fieldExtend.setFieldName(fieldName);
        fieldExtend.setName(name);
        fieldExtend.setType(fieldEnum.getType());
        fieldExtend.setRemark("");
        fieldExtend.setInputTips("");
        fieldExtend.setMaxLength(255);
        fieldExtend.setDefaultValue("");
        fieldExtend.setIsUnique(0);
        fieldExtend.setIsNull(0);
        fieldExtend.setSorting(sorting);
        fieldExtend.setOptions(options);
        fieldExtend.setOperating(0);
        fieldExtend.setIsHidden(0);
        fieldExtend.setFieldType(0);
        fieldExtend.setStylePercent(1);
        fieldExtend.setPrecisions(1);
        fieldExtend.setFormPosition("");
        fieldExtend.setMaxNumRestrict("");
        fieldExtend.setMinNumRestrict("");
        return fieldExtend;
    }
}
