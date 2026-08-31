package com.tianye.hrsystem.modules.role.service;

import com.tianye.hrsystem.modules.role.bo.QueryRoleTypesBO;
import com.tianye.hrsystem.modules.role.entity.TbRoleTypes;
import com.tianye.hrsystem.modules.role.mapper.TbRoleTypesMapper;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbRoleTypesServiceTest {

    @InjectMocks
    private TbRoleTypesService service;

    @Mock
    private TbRoleTypesMapper mapper;

    @Test
    public void tbRoleTypesEntity_shouldMapCanUseToLegacyCamelCaseColumn() throws Exception {
        TableField tableField = TbRoleTypes.class.getDeclaredField("canUse").getAnnotation(TableField.class);

        Assert.assertNotNull("tbroletypes 表字段是 canUse，不能让 MyBatis-Plus 默认映射成 can_use", tableField);
        Assert.assertEquals("canUse", tableField.value());
    }

    @Test
    public void saveRoleType_shouldCreateEnabledRole_whenRequiredFieldsValid() {
        QueryRoleTypesBO bo = new QueryRoleTypesBO();
        bo.setName("考勤管理员");

        service.saveRoleType(bo);

        ArgumentCaptor<TbRoleTypes> captor = ArgumentCaptor.forClass(TbRoleTypes.class);
        verify(mapper).insert(captor.capture());
        Assert.assertEquals("考勤管理员", captor.getValue().getName());
        Assert.assertEquals(Integer.valueOf(0), captor.getValue().getPid());
        Assert.assertEquals(Integer.valueOf(1), captor.getValue().getCanUse());
    }

    @Test
    public void saveRoleType_shouldRejectBlankRoleName() {
        QueryRoleTypesBO bo = new QueryRoleTypesBO();
        bo.setName(" ");

        try {
            service.saveRoleType(bo);
            Assert.fail("角色名称为空应阻止保存");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("角色名称不能为空", ex.getMessage());
        }
    }
}
